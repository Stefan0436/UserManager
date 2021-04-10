package org.asf.connective.usermanager.security;

import java.io.IOException;
import java.io.InputStream;

public class DecryptingInputStream extends InputStream {

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
