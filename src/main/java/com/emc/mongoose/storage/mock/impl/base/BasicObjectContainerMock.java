package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
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
	private final static class LRUIndex<String, T extends DataObjectMock>
	extends LRUMap<String, T> {
		//
		public LRUIndex(final int capacity) {
			super(capacity);
		}
		//
		public final T list(
			final String afterOid, final Collection<T> buffDst, final int limit
		) {
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
	private final LRUIndex<String, T> index;
	private final BlockingQueue<FutureTask<T>> taskQueue = new ArrayBlockingQueue<>(0x1000);
	//
	public BasicObjectContainerMock(final String name, final int capacity) {
		super(name);
		setDaemon(true);
		index = new LRUIndex<>(capacity);
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
		final FutureTask<T> putTask = PutTask.getInstance(index, oid, obj);
		taskQueue.put(putTask);
		return putTask;
	}
	//
	public final static class PutTask<T extends DataObjectMock>
	extends FutureTask<T>
	implements Reusable<PutTask<T>> {
		//
		private final static class PutCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			public Map<String, T> index = null;
			public String oid = null;
			public T obj = null;
			//
			@Override
			public final T call()
			throws Exception {
				return index.put(oid, obj);
			}
		}
		//
		private final PutCall<T> putCall;
		//
		private PutTask(final PutCall<T> putCall) {
			super(putCall);
			this.putCall = putCall;
		}
		//
		public PutTask() {
			this(new PutCall<T>());
		}
		//
		@Override
		public final PutTask<T> reuse(final Object... args)
		throws IllegalArgumentException, IllegalStateException {
			if(args != null) {
				if(args.length > 0) {
					putCall.index = (Map<String, T>) args[0];
				}
				if(args.length > 1) {
					putCall.oid = (String) args[1];
				}
				if(args.length > 2) {
					putCall.obj = (T) args[2];
				}
			}
			return this;
		}
		//
		private final static InstancePool<PutTask<DataObjectMock>>
			POOL = new InstancePool(PutTask.class);
		//
		@Override
		public final void release() {
			POOL.release((PutTask<DataObjectMock>) this);
		}
		//
		public static <T extends DataObjectMock> PutTask<T> getInstance(
			final Map<String, T> index, final String oid, final T obj
		) {
			return (PutTask<T>) POOL.take(index, oid, obj);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> get(final String oid)
	throws InterruptedException {
		final FutureTask<T> getTask = GetTask.getInstance(index, oid);
		taskQueue.put(getTask);
		return getTask;
	}
	//
	public final static class GetTask<T extends DataObjectMock>
	extends FutureTask<T>
	implements Reusable<GetTask<T>> {
		//
		private final static class GetCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			public Map<String, T> index = null;
			public String oid = null;
			//
			@Override
			public final T call()
			throws Exception {
				return index.get(oid);
			}
		}
		//
		private final GetCall<T> getCall;
		//
		private GetTask(final GetCall<T> getCall) {
			super(getCall);
			this.getCall = getCall;
		}
		//
		public GetTask() {
			this(new GetCall<T>());
		}
		//
		@Override
		public final GetTask<T> reuse(final Object... args)
		throws IllegalArgumentException, IllegalStateException {
			if(args != null) {
				if(args.length > 0) {
					getCall.index = (Map<String, T>) args[0];
				}
				if(args.length > 1) {
					getCall.oid = (String) args[1];
				}
			}
			return this;
		}
		//
		private final static InstancePool<GetTask<DataObjectMock>>
			POOL = new InstancePool(GetTask.class);
		//
		@Override
		public final void release() {
			POOL.release((GetTask<DataObjectMock>) this);
		}
		//
		public static <T extends DataObjectMock> GetTask<T> getInstance(
			final Map<String, T> index, final String oid
		) {
			return (GetTask<T>) POOL.take(index, oid);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> remove(final String oid)
	throws InterruptedException {
		final FutureTask<T> removeTask = RemoveTask.getInstance(index, oid);
		taskQueue.put(removeTask);
		return removeTask;
	}
	//
	public final static class RemoveTask<T extends DataObjectMock>
	extends FutureTask<T>
	implements Reusable<RemoveTask<T>> {
		//
		private final static class RemoveCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			public Map<String, T> index = null;
			public String oid = null;
			//
			@Override
			public final T call()
			throws Exception {
				return index.remove(oid);
			}
		}
		//
		private final RemoveCall<T> removeCall;
		//
		private RemoveTask(final RemoveCall<T> removeCall) {
			super(removeCall);
			this.removeCall = removeCall;
		}
		//
		public RemoveTask() {
			this(new RemoveCall<T>());
		}
		//
		@Override
		public final RemoveTask<T> reuse(final Object... args)
		throws IllegalArgumentException, IllegalStateException {
			if(args != null) {
				if(args.length > 0) {
					removeCall.index = (Map<String, T>) args[0];
				}
				if(args.length > 1) {
					removeCall.oid = (String) args[1];
				}
			}
			return this;
		}
		//
		private final static InstancePool<RemoveTask<DataObjectMock>>
			POOL = new InstancePool(RemoveTask.class);
		//
		@Override
		public final void release() {
			POOL.release((RemoveTask<DataObjectMock>) this);
		}
		//
		public static <T extends DataObjectMock> RemoveTask<T> getInstance(
			final Map<String, T> index, final String oid
		) {
			return (RemoveTask<T>) POOL.take(index, oid);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> list(final String oid, final Collection<T> buff, final int limit)
	throws InterruptedException {
		final FutureTask<T> listTask = ListTask.getInstance(index, oid, buff, limit);
		taskQueue.put(listTask);
		return listTask;
	}
	//
	public final static class ListTask<T extends DataObjectMock>
	extends FutureTask<T>
	implements Reusable<ListTask<T>> {
		//
		final static class ListCall<T extends DataObjectMock>
			implements Callable<T> {
			//
			public LRUIndex<String, T> index = null;
			public String oid = null;
			public Collection<T> buff = null;
			public int limit = 0;
			//
			@Override
			public final T call()
			throws Exception {
				return index.list(oid, buff, limit);
			}
		}
		//
		private final ListCall<T> listCall;
		//
		private ListTask(final ListCall<T> listCall) {
			super(listCall);
			this.listCall = listCall;
		}
		//
		public ListTask() {
			this(new ListCall<T>());
		}
		//
		@Override
		public final ListTask<T> reuse(final Object... args)
		throws IllegalArgumentException, IllegalStateException {
			if(args != null) {
				if(args.length > 0) {
					listCall.index = (LRUIndex<String, T>) args[0];
				}
				if(args.length > 1) {
					listCall.oid = (String) args[1];
				}
				if(args.length > 2) {
					listCall.buff = (Collection<T>) args[2];
				}
				if(args.length > 3) {
					listCall.limit = (int) args[3];
				}
			}
			return this;
		}
		//
		private final static InstancePool<ListTask<DataObjectMock>>
			POOL = new InstancePool(ListTask.class);
		//
		@Override
		public final void release() {
			POOL.release((ListTask<DataObjectMock>) this);
		}
		//
		public static <T extends DataObjectMock> ListTask<T> getInstance(
			final Map<String, T> index, final String oid, final Collection<T> buff, final int limit
		) {
			return (ListTask<T>) POOL.take(index, oid, buff, limit);
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
