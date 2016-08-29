package com.emc.mongoose.storage.mock.distribution;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 Created on 26.08.16.
 */
public interface Conversation
extends Remote {

	public static String SERVICE_NAME = "conversation";

	String GREETING = "greeting";

	String getGreeting()
	throws RemoteException;

}
