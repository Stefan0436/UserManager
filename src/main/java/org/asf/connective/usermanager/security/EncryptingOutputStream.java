package org.asf.connective.usermanager.security;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 * Rain encrypting output stream - encrypts data before sending it to the
 * destination stream.<br/>
 * <b>WARNING:</b> only supports byte-sized writing, do not use integers!
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EncryptingOutputStream extends OutputStream {

	/**
	 * Instantiates the encrypting output stream
	 * 
	 * @param key         Key to use for encryption, can be anything, the larger and
	 *                    more random, the more secure.
	 * @param destination Destination stream.
	 */
	public EncryptingOutputStream(byte[] key, OutputStream destination) {
		this.key = key;
		this.destination = destination;
	}

	private byte[] key;
	private int loc = 0;

	private OutputStream destination;

	@Override
	public void write(int dat) throws IOException {
		if (key.length == 0) {
			destination.write(dat);
			return;
		}
		dat += key[loc++];
		if (loc == key.length)
			loc = 0;

		dat += (128 * 2);
		destination.write(dat);
	}

	@Override
	public void close() throws IOException {
		destination.close();
	}

}
