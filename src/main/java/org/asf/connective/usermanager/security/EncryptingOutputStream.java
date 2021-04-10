package org.asf.connective.usermanager.security;

import java.io.IOException;
import java.io.OutputStream;

public class EncryptingOutputStream extends OutputStream {

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
