package com.emc.mongoose.storage.mock.api;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

/**
 Created on 30.08.16.
 */
public interface NodeAggregator<T extends MutableDataItemMock> {

	Collection<StorageMock<T>> getNodes() throws RemoteException;

}
