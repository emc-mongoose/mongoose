package com.emc.mongoose.storage.mock.api.exception;
/**
 Created by kurila on 31.07.15.
 */
public class ContainerMockException
extends Exception {
	//
	public ContainerMockException() {
		super();
	}
	//
	public ContainerMockException(final String name) {
		super(name);
	}
	//
	public ContainerMockException(final Throwable t) {
		super(t);
	}
}
