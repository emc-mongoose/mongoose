package com.emc.mongoose.common.config.reader.jsr353.decoder;

/**
 Created on 11.07.16.
 */
public class DecodeException extends Exception {

	private final String encodedString;

	public DecodeException(String encodedString, String message, Throwable cause) {
		super(message, cause);
		this.encodedString = encodedString;
	}

	/**
	 * Constructs a DecodedException with the given encoded string that cannot
	 * be decoded, and reason why. The encoded string may represent the whole message,
	 * or the part of the message most relevant to the decoding error, depending
	 * whether the application is using one
	 * of the streaming methods or not.
	 *
	 * @param encodedString the string representing the (part of) the message that
	 * could not be decoded.
	 * @param message       the reason for the failure.
	 */
	public DecodeException(String encodedString, String message) {
		super(message);
		this.encodedString = encodedString;
	}

	/**
	 * Return the encoded string that is either the whole message, or the partial
	 * message that could not be decoded, or {@code null} if
	 * this exception arose from a failure to decode a binary message..
	 *
	 * @return the text not decoded or {@code null} for binary message failures.
	 */
	public String getText() {
		return this.encodedString;
	}
}
