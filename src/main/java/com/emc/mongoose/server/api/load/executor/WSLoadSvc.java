package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadSvc<T extends WSObject>
extends WSLoadExecutor<T>, ObjectLoadSvc<T> {
	@Override
	HttpResponse execute(final HttpRequest request)
	throws RemoteException;
}
