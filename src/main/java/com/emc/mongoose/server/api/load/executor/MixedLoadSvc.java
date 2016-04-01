package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.core.api.item.base.Item;
//
import java.rmi.RemoteException;
import java.util.Map;
/**
 Created by kurila on 01.04.16.
 */
public interface MixedLoadSvc<T extends Item>
extends LoadSvc<T> {
	//
	Map<LoadType, String> getWrappedLoadSvcNames()
	throws RemoteException;
}
