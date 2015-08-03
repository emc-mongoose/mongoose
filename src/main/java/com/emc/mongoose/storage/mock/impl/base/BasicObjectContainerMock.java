package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
//
import org.apache.commons.collections4.map.LRUMap;
//
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
/**
 Created by kurila on 31.07.15.
 */
public final class BasicObjectContainerMock<T extends DataObjectMock>
implements ObjectContainerMock<T> {
	//
	private final String name;
	private final Thread taskWorker = new Thread() {
		@Override
		public final void run() {
			taskQueue.drainTo();
		}
	};
	private final BlockingQueue<Callable> taskQueue;
	private final LRUMap<String, T> index;
	//
	public BasicObjectContainerMock(final String name, final int capacity) {
		taskWorker = new Thread();
		taskWorker.setDaemon(true);
		taskWorker.setName("containerWorker<" + name + ">");
		index = new LRUMap<>(capacity);
		this.name = name;
		taskQueue = new ArrayBlockingQueue<>(0x10000);
		taskWorker.start();
	}
	//
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
	public final Future<String> list(
		final String marker, final Collection<T> buffDst, final int maxCount
	) {
		return worker.submit(
			new Callable<String>() {
				@Override
				public final String call()
				throws Exception {
					LinkEntry < String, T > nextEntry = index.getEntry(marker);
					for(int i = 0; i < maxCount; i ++) {
						nextEntry = nextEntry == null ? getEntry(firstKey()) : entryAfter(nextEntry);
						if(nextEntry == null) {
							break;
						}
						buffDst.add(nextEntry.getValue());
					}
					return nextEntry == null ? null : nextEntry.getKey();
				}
			}

	}
	//
	@Override
	public final Future<T> put(final String oid, final T obj) {
		return worker.submit(
			new Callable<T>() {
				@Override
				public final T call() {
					return put(oid, obj);
				}
			}
		);
	}
	//
	@Override
	public final Future<T> remove(final String oid) {
		return worker.submit(
			new Callable<T>() {
				@Override
				public final T call() {
					return remove(oid);
				}
			}
		);
	}
	//
	@Override
	protected void finalize()
	throws Throwable {
		index.clear();
		super.finalize();
	}
}
