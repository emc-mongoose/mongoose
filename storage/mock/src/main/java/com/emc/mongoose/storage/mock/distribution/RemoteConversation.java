package com.emc.mongoose.storage.mock.distribution;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 Created on 26.08.16.
 */
public class RemoteConversation
extends UnicastRemoteObject
implements Conversation, Serializable {

	public RemoteConversation()
	throws RemoteException {
	}

	public String getGreeting()
	throws RemoteException {
		return "Hello";
	}
}
