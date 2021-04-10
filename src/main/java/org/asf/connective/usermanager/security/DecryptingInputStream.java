package org.asf.connective.usermanager.security;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * Rain decrypting input stream - decrypts RAIN-encrypted data.<br/>
 * <b>WARNING:</b> only supports byte-sized reading, do not use integers!
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class DecryptingInputStream extends InputStream {

	/**
	 * Instanciates the decrypting input stream.
	 * 
	 * @param key    Key used for encryption.
	 * @param source Source stream.
	 */
	public DecryptingInputStream(byte[] key, InputStream source) {
		this.key = key;
		this.source = source;
	}

	private byte[] key;
	private int loc = 0;

	private InputStream source;

	@Override
	public int read() throws IOException {
		if (key.length == 0) {
			return source.read();
		}
		int dat = source.read();
		if (dat == -1)
			return -1;

		dat -= key[loc++];
		if (loc == key.length)
			loc = 0;

		return dat;
	}

	@Override
	public void close() throws IOException {
		source.close();
	}

}
