package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.util.DataItemOutput;
import com.emc.mongoose.core.api.load.model.Consumer;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 19.06.15.
 */
public class DataItemOutputConsumer<T extends DataItem>
implements Consumer<T> {
	//
	protected final DataItemOutput<T> itemOut;
	//
	public DataItemOutputConsumer(final DataItemOutput<T> itemOut) {
		this.itemOut = itemOut;
	}
	//
	@Override
	public void submit(final T data)
	throws RemoteException, InterruptedException, RejectedExecutionException {
		try {
			itemOut.write(data);
		} catch(final IOException e) {
			throw new RejectedExecutionException(e);
		}
	}
	//
	@Override
	public void shutdown()
	throws RemoteException {
	}
	//
	@Override
	public long getMaxCount()
	throws RemoteException {
		return Long.MAX_VALUE;
	}
	//
	@Override
	public void close()
	throws IOException {
		itemOut.close();
	}
}
