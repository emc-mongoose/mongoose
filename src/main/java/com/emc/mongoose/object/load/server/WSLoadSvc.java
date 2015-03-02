package com.emc.mongoose.object.load.server;
//
import com.emc.mongoose.object.load.server.ObjectLoadSvc;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.object.load.WSLoadExecutor;
//
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
