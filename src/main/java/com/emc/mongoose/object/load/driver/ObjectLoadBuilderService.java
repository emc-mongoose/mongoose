package com.emc.mongoose.object.load.driver;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.load.driver.LoadBuilderService;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.load.ObjectLoadBuilder;
import com.emc.mongoose.util.conf.RunTimeConfig;

import java.rmi.RemoteException;
/**
 Created by kurila on 29.09.14.
 */
public interface ObjectLoadBuilderService<T extends DataObject, U extends ObjectLoadService<T>>
extends LoadBuilderService<T, U>, ObjectLoadBuilder<T, U> {
	//
	@Override
	ObjectLoadBuilderService<T, U> setProperties(final RunTimeConfig props)
	throws RemoteException;
	//
	@Override
	ObjectLoadBuilderService<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws RemoteException;
	//
	@Override
	ObjectLoadBuilderService<T, U> setLoadType(final Request.Type loadType)
	throws IllegalStateException, RemoteException;
	//
	@Override
	ObjectLoadBuilderService<T, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderService<T, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderService<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderService<T, U> setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderService<T, U> setThreadsPerNodeFor(
		final short threadCount, final Request.Type loadType
	) throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderService<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderService<T, U> setInputFile(final String listFile)
	throws RemoteException;
	//
	@Override
	ObjectLoadBuilderService<T, U> setUpdatesPerItem(final int count)
	throws RemoteException;
	//
}
