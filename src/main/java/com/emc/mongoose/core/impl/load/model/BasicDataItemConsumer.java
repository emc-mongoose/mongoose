package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemDst;
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.load.model.DataItemConsumer;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 25.09.15.
 */
public class BasicDataItemConsumer<T extends DataItem>
implements DataItemConsumer<T> {
	//
	protected final DataItemDst<T> itemDst;
	//
	public
	BasicDataItemConsumer(final DataItemDst<T> itemDst) {
		this.itemDst = itemDst;
	}
	//
	@Override
	public void put(final T dataItem)
	throws IOException, InterruptedException, RejectedExecutionException {
		itemDst.put(dataItem);
	}
	//
	@Override
	public int put(final List<T> buffer, final int from, final int to)
	throws IOException, InterruptedException, RejectedExecutionException {
		return itemDst.put(buffer, from, to);
	}
	//
	@Override
	public int put(final List<T> buffer)
	throws IOException, InterruptedException, RejectedExecutionException {
		return itemDst.put(buffer);
	}
	//
	@Override
	public DataItemSrc<T> getDataItemSrc()
	throws IOException {
		return itemDst.getDataItemSrc();
	}
	//
	@Override
	public String getName()
	throws RemoteException {
		return "syncConsumer<" + itemDst + ">";
	}
	//
	@Override
	public void start()
	throws RemoteException, IllegalThreadStateException {
	}
	//
	@Override
	public void shutdown()
	throws RemoteException, IllegalStateException {
	}
	//
	@Override
	public void await()
	throws RemoteException, InterruptedException {
	}
	//
	@Override
	public void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException {
	}
	//
	@Override
	public void interrupt()
	throws RemoteException {
	}
	//
	@Override
	public void close()
	throws IOException {
		itemDst.close();
	}
}
