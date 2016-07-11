package com.emc.mongoose.config;

/**
 Created on 11.07.16.
 */
public interface Decoder {

	void init();

	void destroy();

	interface Text<T> extends Decoder {
		/**
		 * Decode the given String into an object of type T.
		 *
		 * @param s string to be decoded.
		 * @return the decoded message as an object of type T
		 */
		T decode(String s) throws DecodeException;

		/**
		 * Answer whether the given String can be decoded into an object of type T.
		 *
		 * @param s the string being tested for decodability.
		 * @return whether this decoder can decoded the supplied string.
		 */
		boolean willDecode(String s);
	}
	
}
