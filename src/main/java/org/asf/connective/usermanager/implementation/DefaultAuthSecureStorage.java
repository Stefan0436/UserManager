package org.asf.connective.usermanager.implementation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import org.asf.connective.usermanager.api.AuthSecureStorage;
import org.asf.connective.usermanager.security.DecryptingInputStream;
import org.asf.connective.usermanager.security.EncryptingOutputStream;
import org.asf.cyan.api.packet.PacketBuilder;
import org.asf.cyan.api.packet.PacketEntry;
import org.asf.cyan.api.packet.PacketParser;

public class DefaultAuthSecureStorage extends AuthSecureStorage {

	private class StorageEntry {
		public String name;
		public Object value;
		public StorageEntry next = null;
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
			if (ent.name.equals(name) && type.isAssignableFrom(ent.value.getClass())) {
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
			StorageEntry ent = new StorageEntry();
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
			DecryptingInputStream strm = new DecryptingInputStream(securityKey, new FileInputStream(containerFile));
			parser.importStream(strm);
			strm.close();

			PacketEntry<?> entry = parser.nextEntry();

			first = null;
			while (entry != null) {
				set(entry.get().toString(), parser.nextEntry().get());
				entry = parser.nextEntry();
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

		PacketBuilder builder = new PacketBuilder();
		StorageEntry ent = first;
		while (ent != null) {
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
			} else {
				builder.add(ent.value);
			}

			ent = ent.next;
		}
		EncryptingOutputStream dest = new EncryptingOutputStream(securityKey, new FileOutputStream(containerFile));
		builder.build(dest);
		dest.close();
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

}
