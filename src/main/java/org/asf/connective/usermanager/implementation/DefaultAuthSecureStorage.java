package org.asf.connective.usermanager.implementation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.asf.connective.usermanager.api.AuthSecureStorage;
import org.asf.connective.usermanager.security.DecryptingInputStream;
import org.asf.connective.usermanager.security.EncryptingOutputStream;
import org.asf.cyan.api.packet.PacketBuilder;
import org.asf.cyan.api.packet.PacketEntry;
import org.asf.cyan.api.packet.PacketParser;
import org.asf.cyan.api.packet.entries.IntEntry;

public class DefaultAuthSecureStorage extends AuthSecureStorage {

	private static final int containerVersion = 2;

	private class StorageEntry {
		public String name;
		public Object value;
		public StorageEntry next = null;
	}

	private class StorageIter implements Iterator<UserEntry> {
		public StorageEntry next = null;

		@Override
		public boolean hasNext() {
			while (next != null && next.value == null)
				next = next.next;
			return next != null;
		}

		@Override
		public UserEntry next() {
			while (next.value == null)
				next = next.next;
			StorageEntry ent = next;
			next = next.next;
			return new UserEntry(ent.name, ent.value.getClass(), ent.value);
		}

	}

	@Override
	public Iterable<UserEntry> getAllEntries() {
		return new Iterable<UserEntry>() {

			@Override
			public Iterator<UserEntry> iterator() {
				StorageIter iter = new StorageIter();
				iter.next = first;
				return iter;
			}

		};
	}

	private File containerFile;
	private StorageEntry first = null;

	private byte[] securityKey = new byte[0];

	public static void assign() {
		implementation = new DefaultAuthSecureStorage();
	}

	@Override
	protected AuthSecureStorage newInstance() {
		return new DefaultAuthSecureStorage();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String name, Class<T> type) {
		StorageEntry ent = getInternal(name, type);
		if (ent == null)
			return null;
		else
			return (T) ent.value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> type) {
		StorageEntry ent = first;
		while (ent != null) {
			if (type.isAssignableFrom(ent.value.getClass())) {
				return (T) ent.value;
			}
			ent = ent.next;
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] getAll(Class<T> type) {
		ArrayList<T> values = new ArrayList<T>();
		StorageEntry ent = first;
		while (ent != null) {
			if (type.isAssignableFrom(ent.value.getClass())) {
				values.add((T) ent.value);
			}
			ent = ent.next;
		}
		return values.toArray(t -> (T[]) Array.newInstance(type, t));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] getAll(String name, Class<T> type) {
		ArrayList<T> values = new ArrayList<T>();
		StorageEntry ent = first;
		while (ent != null) {
			if (ent.name.equals(name) && type.isAssignableFrom(ent.value.getClass())) {
				values.add((T) ent.value);
			}
			ent = ent.next;
		}
		return values.toArray(t -> (T[]) Array.newInstance(type, t));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String name) {
		return (T) get(name, Object.class);
	}

	@Override
	public Object[] getAll(String name) {
		return getAll(name, Object.class);
	}

	private <T> StorageEntry getInternal(String name, Class<T> type) {
		StorageEntry ent = first;
		while (ent != null) {
			if (ent.name.equals(name) && ent.value != null && type.isAssignableFrom(ent.value.getClass())) {
				return ent;
			}
			ent = ent.next;
		}
		return null;
	}

	@Override
	public <T> void set(String name, T value) {
		StorageEntry old = getInternal(name, value.getClass());
		if (old != null) {
			old.value = value;
		} else {
			StorageEntry ent = first;
			while (ent != null) {
				if (ent.value == null) {
					ent.name = name;
					ent.value = value;
					return;
				}
				ent = ent.next;
			}

			ent = new StorageEntry();
			ent.name = name;
			ent.value = value;

			if (first == null) {
				first = ent;
				return;
			}

			StorageEntry owner = first;
			while (true) {
				if (owner.next == null)
					break;
				owner = owner.next;
			}

			owner.next = ent;
		}
	}

	@Override
	public <T> boolean has(String name, Class<T> type) {
		return get(name, type) != null;
	}

	@Override
	protected void importContainer(byte[] securityKey) throws IOException {
		this.securityKey = securityKey;
		if (!containerFile.getParentFile().exists())
			containerFile.getParentFile().mkdirs();

		if (containerFile.exists()) {
			PacketParser parser = new PacketParser();
			File backup = new File(containerFile.getAbsolutePath() + ".bak");
			File backuplock = new File(containerFile.getAbsolutePath() + ".bak.lck");
			if (backup.exists() && !backuplock.exists()) {
				info("Found backup container file, meaning the container has most likely been corrupted.\nRestoring container backup...");
				byte[] data = Files.readAllBytes(backup.toPath());
				Files.write(containerFile.toPath(), data);
				info("Importing backup instead of the container...");
			}
			if (backuplock.exists())
				backuplock.delete();
			if (backup.exists())
				backup.delete();

			DecryptingInputStream strm = new DecryptingInputStream(securityKey, new FileInputStream(containerFile));
			parser.importStream(strm);
			strm.close();

			boolean newFormat = false;
			PacketEntry<?> entry = parser.nextEntry();
			if (entry instanceof IntEntry)
				newFormat = true;

			first = null;

			if (!newFormat) {
				while (entry != null) {
					String nm = entry.get().toString();
					try {
						set(nm, parser.nextEntry().get());
					} catch (Exception e) {
						error("Failed to load entry " + nm + " of user container " + containerFile.getName()
								+ ", this is a legacy container, and will be converted, but the failed entry is skipped.");
					}
					entry = parser.nextEntry();
				}
				write();
			} else {
				int version = (int) entry.get();
				entry = parser.nextEntry();
				switch (version) {
				case 2:

					while (entry != null) {
						if (entry instanceof IntEntry) {
							int count = (int) entry.get();
							String type = parser.nextEntry().get().toString();
							String name = parser.nextEntry().get().toString();

							Object arr;
							try {
								arr = Array.newInstance(Class.forName(type), count);
							} catch (NegativeArraySizeException | ClassNotFoundException e) {
								try {
									arr = Array.newInstance(Class.forName(type, false, getClass().getClassLoader()),
											count);
								} catch (NegativeArraySizeException | ClassNotFoundException e2) {
									throw new IOException("Failed to instantiate array for type " + type, e2);
								}
							}

							for (int i = 0; i < count; i++) {
								Array.set(arr, i, parser.nextEntry().get());
							}
							set(name, arr);
						} else {
							set(entry.get().toString(), parser.nextEntry().get());
						}
						entry = parser.nextEntry();

					}

					break;
				default:
					throw new IOException("Unsupported RSDC version: " + version);
				}
			}
		} else {
			write();
		}
	}

	@Override
	protected void assignContainer(File containerFile) {
		this.containerFile = containerFile;
	}

	@Override
	public void write() throws IOException {
		if (!containerFile.getParentFile().exists())
			containerFile.getParentFile().mkdirs();

		File backup = new File(containerFile.getAbsolutePath() + ".bak");
		if (containerFile.exists()) {
			File backuplock = new File(containerFile.getAbsolutePath() + ".bak.lck");

			if (backup.exists())
				backup.delete();
			if (!backuplock.exists())
				backuplock.createNewFile();

			Files.copy(containerFile.toPath(), backup.toPath());
			backuplock.delete();
		} else {
			File backuplock = new File(containerFile.getAbsolutePath() + ".bak.lck");

			if (backup.exists())
				backup.delete();
			if (!backuplock.exists())
				backuplock.createNewFile();

			FileOutputStream fOut = new FileOutputStream(containerFile);
			EncryptingOutputStream dest = new EncryptingOutputStream(securityKey, fOut);
			PacketBuilder builder = new PacketBuilder();

			builder.add(containerVersion);

			builder.build(dest);
			dest.close();
			fOut.close();

			Files.copy(containerFile.toPath(), backup.toPath());
			backuplock.delete();
		}

		PacketBuilder builder = new PacketBuilder();
		builder.add(containerVersion);

		StorageEntry ent = first;
		while (ent != null) {
			if (ent.value == null) {
				ent = ent.next;
				continue;
			}

			if (!ent.value.getClass().isArray())
				builder.add(ent.name);

			if (ent.value instanceof String) {
				builder.add((String) ent.value);
			} else if (ent.value instanceof Integer) {
				builder.add((int) ent.value);
			} else if (ent.value instanceof Float) {
				builder.add((float) ent.value);
			} else if (ent.value instanceof byte[]) {
				builder.add((byte[]) ent.value);
			} else if (ent.value instanceof Character) {
				builder.add((char) ent.value);
			} else if (ent.value instanceof Byte) {
				builder.add((byte) ent.value);
			} else if (ent.value instanceof Double) {
				builder.add((double) ent.value);
			} else if (ent.value instanceof Long) {
				builder.add((long) ent.value);
			} else if (ent.value instanceof PacketEntry) {
				builder.add((PacketEntry<?>) ent.value);
			} else if (ent.value.getClass().isArray()) {
				int l = Array.getLength(ent.value);
				builder.add(l);
				builder.add(ent.value.getClass().componentType().getTypeName());
				builder.add(ent.name);

				for (int i = 0; i < l; i++)
					builder.add(Array.get(ent.value, i));
			} else {
				builder.add(ent.value);
			}

			ent = ent.next;
		}

		FileOutputStream fOut = new FileOutputStream(containerFile);
		EncryptingOutputStream dest = new EncryptingOutputStream(securityKey, fOut);
		builder.build(dest);
		dest.close();
		fOut.close();

		backup.delete();
	}

	@Override
	public void changeKey(byte[] oldKey, byte[] newKey) throws IOException {
		if (!Arrays.equals(oldKey, securityKey))
			throw new IOException("Current and old key don't match.");

		securityKey = newKey;
		write();
	}

	@Override
	public void changeFileLocation(File newContainerFile) throws IOException {
		containerFile.delete();
		containerFile = newContainerFile;
		write();
	}

	@Override
	public <T> void set(T value) {
		set("no-name", value);
	}

	@Override
	public boolean checkSecurity(byte[] key) {
		return Arrays.equals(key, securityKey);
	}

	@Override
	public <T> void reassign(String name, Class<?> oldValueType, T newValue) {
		StorageEntry old = getInternal(name, oldValueType);
		if (old != null) {
			old.value = newValue;
		} else {
			StorageEntry ent = first;
			while (ent != null) {
				if (ent.value == null) {
					ent.name = name;
					ent.value = newValue;
					return;
				}
				ent = ent.next;
			}

			ent = new StorageEntry();
			ent.name = name;
			ent.value = newValue;

			if (first == null) {
				first = ent;
				return;
			}

			StorageEntry owner = first;
			while (true) {
				if (owner.next == null)
					break;
				owner = owner.next;
			}

			owner.next = ent;
		}
	}

	@Override
	public <T> void remove(String name, Class<T> type) {
		StorageEntry itm = getInternal(name, type);
		if (itm != null) {
			itm.value = null;
		}
	}

}
