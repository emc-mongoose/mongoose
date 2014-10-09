package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.data.DataItem;
/**
 Created by kurila on 09.10.14.
 */
public final class StorageNodeSubmitTask<T extends DataItem>
implements Runnable {
	//
	private final StorageNodeExecutor<T> node;
	private final T dataItem;
	//
	public StorageNodeSubmitTask(final StorageNodeExecutor<T> node, final T dataItem) {
		this.node = node;
		this.dataItem = dataItem;
	}
	//
	@Override
	public final void run() {
		node.submit(dataItem);
	}
}
