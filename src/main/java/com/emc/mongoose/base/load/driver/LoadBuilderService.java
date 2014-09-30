package com.emc.mongoose.base.load.driver;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadBuilder;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.remote.Service;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 */
public interface LoadBuilderService<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilder<T, U>, Service {
	//
	String buildRemotely()
	throws RemoteException;
	//
}
