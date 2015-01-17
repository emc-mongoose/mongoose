package com.emc.mongoose.web.load.server;
//
import com.emc.mongoose.object.load.server.ObjectLoadSvc;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.load.WSLoadExecutor;
//
import org.apache.http.HttpHost;
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
	HttpResponse execute(final HttpHost tgtHost, final HttpRequest request)
	throws RemoteException;
}
