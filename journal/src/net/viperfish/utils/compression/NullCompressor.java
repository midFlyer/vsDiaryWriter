package net.viperfish.utils.compression;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class NullCompressor extends Compressor {

	public NullCompressor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected OutputStream createOutputStream(ByteArrayOutputStream out) {
		return new BufferedOutputStream(out);
	}

	@Override
	protected InputStream createInputStream(ByteArrayInputStream in) {
		return new BufferedInputStream(in);
	}

}