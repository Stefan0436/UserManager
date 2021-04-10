package org.asf.connective.usermanager.api;

import java.io.File;
import java.io.IOException;

/**
 * 
 * User secure storage - encrypted user storage, implementation-based.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class AuthSecureStorage {
	protected AuthSecureStorage() {
	}

	/**
	 * Main implementation
	 */
	protected static AuthSecureStorage implementation = null;

	/**
	 * Opens the secure storage file
	 * 
	 * @param containerFile Secure user container storage file
	 * @param securityKey   Security key
	 * @return New AuthSecureStorage instance with the given file as container.
	 * @throws IOException If loading fails
	 */
	public static AuthSecureStorage open(File containerFile, byte[] securityKey) throws IOException {
		AuthSecureStorage storage = implementation.newInstance();
		storage.assignContainer(containerFile);
		storage.importContainer(securityKey);
		return storage;
	}

	/**
	 * Retrieves the first value with the given name
	 * 
	 * @param <T>  Value type (unchecked)
	 * @param name Key name
	 * @return First value of the given key or null.
	 */
	public abstract <T> T get(String name);

	/**
	 * Retrieves the first value with the given class
	 * 
	 * @param <T>  Value type
	 * @param type Value class
	 * @return First value of the given class or null.
	 */
	public abstract <T> T get(Class<T> type);

	/**
	 * Retrieves the first value with the given class and name
	 * 
	 * @param <T>  Value type
	 * @param name Key name
	 * @param type Value class
	 * @return First value of the given class and name
	 */
	public abstract <T> T get(String name, Class<T> type);

	/**
	 * Sets the no-name key to the given value (class-based, can have more values)
	 * 
	 * @param <T>   Value type
	 * @param value Value to set/add
	 */
	public abstract <T> void set(T value);

	/**
	 * Sets the given key to the given value
	 * 
	 * @param <T>   Value type
	 * @param name  Key name
	 * @param value Value to set/add
	 */
	public abstract <T> void set(String name, T value);

	/**
	 * Checks if a given key and type are present in the user storage
	 * 
	 * @param <T>  Value type
	 * @param name Key name
	 * @param type Value class
	 * @return True if present, false otherwise
	 */
	public abstract <T> boolean has(String name, Class<T> type);

	/**
	 * Imports a container, file provider by assignContainer, this is to decrypt the
	 * file
	 * 
	 * @param securityKey File securtiy key
	 * @throws IOException If decrypting fails
	 */
	protected abstract void importContainer(byte[] securityKey) throws IOException;

	/**
	 * Assigns the container file
	 * 
	 * @param containerFile Container file
	 */
	protected abstract void assignContainer(File containerFile);

	/**
	 * Saves changes made to the container
	 * 
	 * @throws IOException If saving fails
	 */
	public abstract void write() throws IOException;

	/**
	 * Moves the container file (like when the username changes)
	 * 
	 * @param newContainerFile New location
	 * @throws IOException If saving fails
	 */
	public abstract void changeFileLocation(File newContainerFile) throws IOException;

	/**
	 * Changes the key used to encrypt the storage
	 * 
	 * @param oldKey Current key (check/decrypt)
	 * @param newKey New key
	 * @throws IOException If saving fails
	 */
	public abstract void changeKey(byte[] oldKey, byte[] newKey) throws IOException;

	/**
	 * Instanciates a new container instance (Main implementation)
	 * 
	 * @return New container instance
	 */
	protected abstract AuthSecureStorage newInstance();

	/**
	 * Retrieves all values of a given name
	 * 
	 * @param name Key name
	 * @return Array of values
	 */
	public abstract Object[] getAll(String name);

	/**
	 * Retrieves all values of a given name and class
	 * 
	 * @param <T>  Value type
	 * @param name Key name
	 * @param type Value class
	 * @return Array of values matching the given name and class
	 */
	public abstract <T> T[] getAll(String name, Class<T> type);

	/**
	 * Retrieves all values of a given class
	 * 
	 * @param <T>  Value type
	 * @param type Value class
	 * @return Array of values matching the given class
	 */
	public abstract <T> T[] getAll(Class<T> type);
}
