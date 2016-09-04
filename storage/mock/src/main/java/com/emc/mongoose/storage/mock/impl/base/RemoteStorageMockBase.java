package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.RemoteStorageMock;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 Created on 31.08.16.
 */
public abstract class RemoteStorageMockBase<T extends MutableDataItemMock>
extends UnicastRemoteObject
implements RemoteStorageMock<T> {

	protected RemoteStorageMockBase() throws RemoteException {}
}
