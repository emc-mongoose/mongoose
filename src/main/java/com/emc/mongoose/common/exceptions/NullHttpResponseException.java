package com.emc.mongoose.common.exceptions;

import java.io.IOException;

/**
 * Created by gusakk on 03.09.15.
 */
public class NullHttpResponseException extends IOException {
	//
	public NullHttpResponseException(String message, Throwable cause) {
		super(message, cause);
	}
	//
	public NullHttpResponseException(Throwable cause) {
		super(cause);
	}
	//
}
