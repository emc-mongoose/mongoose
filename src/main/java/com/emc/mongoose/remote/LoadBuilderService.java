package com.emc.mongoose.remote;
//
import com.emc.mongoose.LoadExecutor;
import com.emc.mongoose.LoadBuilder;
import com.emc.mongoose.data.UniformData;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 */
public interface LoadBuilderService<T extends LoadExecutor<? extends UniformData>>
extends LoadBuilder<T>, Service {
	//
	String buildRemotely()
	throws RemoteException;
	//
}
