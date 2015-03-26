package com.emc.mongoose.server.impl.persist;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.impl.persist.TmpFileItemBuffer;
//
import com.emc.mongoose.server.api.persist.DataItemBufferSvc;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 11.03.15.
 */
public final class TmpFileItemBufferSvc<T extends DataItem>
extends TmpFileItemBuffer<T>
implements DataItemBufferSvc<T> {
	//
	public TmpFileItemBufferSvc(final long maxCount, final int threadCount) {
		super(maxCount, threadCount);
	}
	//
	@Override
	public final String getName()
	throws RemoteException {
		return toString();
	}
}
