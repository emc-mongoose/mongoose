package com.emc.mongoose.integTestTools;

import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Created by olga on 02.07.15.
 */
public final class MD5SumJava {

	public static String getMD5Checksum(final String dataID)
	throws Exception {
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		byte[] buffer = new byte[1024];
		int numRead;

		try (InputStream inputStream = WgetJava.getStream(dataID)) {
			do {
				numRead = inputStream.read(buffer);
				if (numRead > 0) {
					messageDigest.update(buffer, 0, numRead);
				}
			} while (numRead != -1);
		}

		byte[] checksum = messageDigest.digest();
		String result = "";

		//a faster way to convert a byte array to a HEX string
		for (byte aChecksum : checksum) {
			result += Integer.toString((aChecksum & 0xff) + 0x100, 16).substring(1);
		}

		return result;
	}
}
