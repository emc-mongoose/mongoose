package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
//
import org.apache.commons.collections4.map.LRUMap;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
/**
 Created by kurila on 31.07.15.
 */
public final class BasicObjectContainerMock<T extends DataObjectMock>
extends Thread
implements ObjectContainerMock<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static class ObjectMockIndex<T extends DataObjectMock>
	extends LRUMap<String, T> {
		//
		public ObjectMockIndex(final int capacity) {
			super(capacity);
		}
		//
		public final T list(final String afterOid, final Collection<T> buffDst, final int limit) {
			if(isEmpty()) {
				return null;
			}
			LinkEntry<String, T> nextEntry = getEntry(afterOid);
			for(int i = 0; i < limit; i++) {
				nextEntry = nextEntry == null ? getEntry(firstKey()) : entryAfter(nextEntry);
				if(nextEntry == null) {
					break;
				}
				buffDst.add(nextEntry.getValue());
			}
			return nextEntry == null ? null : nextEntry.getValue();
		}
	}
	//
	private final ObjectMockIndex<T> index;
	private final BlockingQueue<FutureTask<T>> taskQueue = new ArrayBlockingQueue<>(0x1000);
	//
	public BasicObjectContainerMock(final String name, final int capacity) {
		super(name);
		setDaemon(true);
		index = new ObjectMockIndex<>(capacity);
		start();
	}
	//
	public final void run() {
		final int batchSize = RunTimeConfig.getContext().getBatchSize();
		final List<FutureTask<T>> taskBuff = new ArrayList<>(batchSize);
		int n;
		try {
			while(true) {
				n = taskQueue.drainTo(taskBuff);
				if(n > 0) {
					for(final FutureTask<T> nextTask : taskBuff) {
						try {
							nextTask.run();
						} catch(final Exception e) {
							LogUtil.exception(LOG, Level.WARN, e, "Task \"{}\" failed", nextTask);
						}
					}
					taskBuff.clear();
				} else {
					Thread.sleep(10);
				}
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted");
		} finally {
			close();
		}
	}
	//
	@Override
	public final int size() {
		return index.size();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> put(final String oid, final T obj)
	throws InterruptedException {
		final FutureTask<T> putTask = new PutTask<>(index, oid, obj);
		taskQueue.put(putTask);
		return putTask;
	}
	//
	private final static class PutTask<T extends DataObjectMock>
	extends FutureTask<T> {
		//
		private final static class PutCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			private final Map<String, T> index;
			private final String oid;
			private final T obj;
			//
			private PutCall(final Map<String, T> index, final String oid, final T obj) {
				this.index = index;
				this.oid = oid;
				this.obj = obj;
			}
			//
			@Override
			public final T call()
			throws Exception {
				return index.put(oid, obj);
			}
		}
		//
		public PutTask(final Map<String, T> index, final String oid, final T obj) {
			super(new PutCall<>(index, oid, obj));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> get(final String oid)
	throws InterruptedException {
		final FutureTask<T> getTask = new GetTask<>(index, oid);
		taskQueue.put(getTask);
		return getTask;
	}
	//
	private final static class GetTask<T extends DataObjectMock>
	extends FutureTask<T> {
		//
		private final static class GetCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			private final Map<String, T> index;
			private final String oid;
			//
			private GetCall(final Map<String, T> index, final String oid) {
				this.index = index;
				this.oid = oid;
			}
			//
			@Override
			public final T call()
			throws Exception {
				return index.get(oid);
			}
		}
		//
		public GetTask(final Map<String, T> index, final String oid) {
			super(new GetCall<T>(index, oid));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> remove(final String oid)
	throws InterruptedException {
		final FutureTask<T> removeTask = new RemoveTask<>(index, oid);
		taskQueue.put(removeTask);
		return removeTask;
	}
	//
	private final static class RemoveTask<T extends DataObjectMock>
	extends FutureTask<T> {
		//
		private final static class RemoveCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			private final Map<String, T> index;
			private final String oid;
			//
			private RemoveCall(final Map<String, T> index, final String oid) {
				this.index = index;
				this.oid = oid;
			}
			//
			@Override
			public final T call()
			throws Exception {
				return index.remove(oid);
			}
		}
		//
		public RemoveTask(final Map<String, T> index, final String oid) {
			super(new RemoveCall<T>(index, oid));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> list(final String oid, final Collection<T> buff, final int limit)
	throws InterruptedException {
		final FutureTask<T> listTask = new ListTask<>(index, oid, buff, limit);
		taskQueue.put(listTask);
		return listTask;
	}
	//
	public final static class ListTask<T extends DataObjectMock>
	extends FutureTask<T> {
		//
		private final static class ListCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			private final ObjectMockIndex<T> index;
			private final String oid;
			private final Collection<T> buff;
			private final int limit;
			//
			private ListCall(
				final ObjectMockIndex<T> index, final String oid, final Collection<T> buff,
				final int limit
			) {
				this.index = index;
				this.oid = oid;
				this.buff = buff;
				this.limit = limit;
			}
			//
			@Override
			public final T call()
			throws Exception {
				return index.list(oid, buff, limit);
			}
		}
		//
		public ListTask(
			final ObjectMockIndex<T> index, final String oid, final Collection<T> buff,
			final int limit
		) {
			super(new ListCall<T>(index, oid, buff, limit));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void close() {
		if(isAlive()) {
			interrupt();
		}
		taskQueue.clear();
		index.clear();
	}
	//
	@Override
	protected void finalize()
	throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}
}
