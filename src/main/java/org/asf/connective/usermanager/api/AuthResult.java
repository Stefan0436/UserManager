package org.asf.connective.usermanager.api;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Random;

import org.asf.connective.usermanager.UserManagerModule;

/**
 * 
 * Authentication result, contains username, group and a secure storage object
 * for an authenticated user.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class AuthResult {
	protected boolean status;

	protected String group = null;
	protected String username = null;

	protected AuthSecureStorage secureStorage = null;
	private byte[] oldKey = null;

	/**
	 * Instanciates a authentication result with failed status
	 */
	public AuthResult() {
		this.status = false;
	}

	/**
	 * Intanciates a successful aithentication result and opens the secure storage
	 * 
	 * @param group    User group
	 * @param username Username
	 * @param password User password for unlocking the secure storage
	 * @throws IOException If opening the secure storage fails
	 */
	public AuthResult(String group, String username, char[] password) throws IOException {
		this.username = username;
		this.group = group;
		this.status = true;

		oldKey = rainkey(password, false).getBytes();
	}

	/**
	 * Loads the secure storage data if needed
	 * 
	 * @throws IOException If loading fails
	 */
	public void openSecureStorage() throws IOException {
		if (secureStorage == null)
			secureStorage = UserManagerModule.getSecureStore(group, username, oldKey);
	}

	private String rainkey(char[] password, boolean update) throws IOException {
		long dateOfUpdate = 0;
		long nano = 0;

		File userDataFile = new File(UserManagerModule.getActivatedUsersDir(), group + "." + username + ".lck");
		if (!userDataFile.exists() || update) {
			if (update)
				userDataFile.delete();

			ZonedDateTime tm = ZonedDateTime.ofInstant(new Date().toInstant(), ZoneId.of("UTC"));
			dateOfUpdate = tm.toEpochSecond();
			nano = tm.getNano();

			userDataFile.getParentFile().mkdirs();
			Files.writeString(userDataFile.toPath(), dateOfUpdate + "/" + nano);
		} else {
			String data = Files.readAllLines(userDataFile.toPath()).get(0);
			dateOfUpdate = Long.valueOf(data.substring(0, data.lastIndexOf("/")));
			nano = Long.valueOf(data.substring(data.lastIndexOf("/") + 1));
		}

		BigInteger size = BigInteger.valueOf(dateOfUpdate).multiply(BigInteger.valueOf(nano));
		long totalLength = password.length * (dateOfUpdate / (nano / 4));
		while (totalLength > Integer.MAX_VALUE) {
			totalLength = totalLength / 10;
		}
		byte[] containerPassSeed = new byte[(int) totalLength];
		byte[] sizeArray = size.toByteArray();

		int ind = 0;
		int i2 = 0;
		int i3 = 0;
		int segmentSize = (int) (totalLength / password.length);

		for (int i = 0; i < totalLength; i++) {
			if (ind == segmentSize) {
				containerPassSeed[i] = ByteBuffer.allocate(2).putChar(password[i2++]).array()[0];
				if (i2 == password.length)
					i2 = 0;

				ind = 0;
			} else {
				containerPassSeed[i] = sizeArray[i3++];
				if (i3 == sizeArray.length)
					i3 = 0;
			}
			ind++;
		}

		String seedChars = new String(Base64.getEncoder().encode(containerPassSeed));
		long seed = 1;
		long seedMin = 0;
		for (char s : seedChars.toCharArray()) {
			try {
				seed = Long.valueOf(seed + "" + (int) s);
			} catch (NumberFormatException e) {
				if (seedMin + seed < 0) {
					break;
				}
				seedMin += seed;
				seed = 1;
			}
		}

		Random rnd1 = new Random(seedMin);
		Random rnd2 = new Random(seed);

		String passkey = "";
		for (int i = 0; i < totalLength; i += 2) {
			int num = rnd1.nextInt('9');
			while (num <= '0')
				num = rnd1.nextInt('9');
			passkey += (char) num;

			num = rnd2.nextInt('Z');
			while (num < 'A')
				num = rnd2.nextInt('Z');
			passkey += (char) num;
		}
		return passkey;
	}

	/**
	 * Retrieves the authentication status
	 * 
	 * @return True if authenticated successfully, false otherwise
	 */
	public boolean success() {
		return status;
	}

	/**
	 * Retrieves the user group name
	 * 
	 * @return User group name
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Retrieves the username
	 * 
	 * @return Username used to authenticate
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Retrieves the secure storage instance of the user
	 * 
	 * @return User secure storage
	 */
	public AuthSecureStorage getUserStorage() {
		try {
			openSecureStorage();
		} catch (IOException e) {
			throw new RuntimeException("Could not load the secure storage container!");
		}
		return secureStorage;
	}

	/**
	 * Changes the user password (for the user storage container only)
	 * 
	 * @param password New password
	 * @throws IOException If updating fails
	 */
	public void setNewPassword(char[] password) throws IOException {
		try {
			openSecureStorage();
		} catch (IOException e) {
			throw new RuntimeException("Could not load the secure storage container!");
		}
		String newKey = rainkey(password, true);
		secureStorage.changeKey(oldKey, newKey.getBytes());
		oldKey = newKey.getBytes();
	}

	/**
	 * Changes the username for this result
	 * 
	 * @param newUserName New username
	 * @throws IOException
	 */
	public void setNewUsername(String newUserName) throws IOException {
		try {
			openSecureStorage();
		} catch (IOException e) {
			throw new RuntimeException("Could not load the secure storage container!");
		}
		File userDataFile = new File(UserManagerModule.getActivatedUsersDir(), group + "." + username + ".lck");
		File newUserDataFile = new File(UserManagerModule.getActivatedUsersDir(), group + "." + newUserName + ".lck");

		if (newUserDataFile.exists())
			throw new IOException("Username already taken.");
		if (userDataFile.exists())
			Files.move(userDataFile.toPath(), newUserDataFile.toPath());

		UserManagerModule.updateUserName(group, username, newUserName);
		secureStorage.changeFileLocation(UserManagerModule.getStoreFile(group, newUserName));
		username = newUserName;
	}

	/**
	 * Deletes user storage and lock files
	 */
	public void deleteUser() {
		File userDataFile = new File(UserManagerModule.getActivatedUsersDir(), group + "." + username + ".lck");
		if (userDataFile.exists())
			userDataFile.delete();

		if (UserManagerModule.getStoreFile(group, username).exists())
			UserManagerModule.getStoreFile(group, username).delete();

		secureStorage = null;
	}
}
