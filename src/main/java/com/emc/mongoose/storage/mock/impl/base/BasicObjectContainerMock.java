package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.concurrent.Sequencer;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
//
import org.apache.commons.collections4.map.LRUMap;
//
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
/**
 Created by kurila on 31.07.15.
 */
public final class BasicObjectContainerMock<T extends DataObjectMock>
extends LRUMap<String, T>
implements ObjectContainerMock<T> {
	//
	private final String name;
	private final Sequencer seqWorker;
	//
	public BasicObjectContainerMock(final String name, final int capacity) {
		super(capacity);
		this.name = name;
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		seqWorker = new Sequencer(
			"sequencer<" + name + ">", true, rtConfig.getTasksMaxQueueSize(),
			rtConfig.getBatchSize(), 10
		);
		seqWorker.start();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
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
	//
	@Override
	public final void close() {
		if(seqWorker.isAlive()) {
			seqWorker.interrupt();
		}
		clear();
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> submitPut(final String oid, final T obj)
		throws InterruptedException {
		final FutureTask<T> putTask = new PutObjectTask<>(this, oid, obj);
		seqWorker.submit(putTask);
		return putTask;
	}
	//
	private final static class PutObjectTask<T extends DataObjectMock>
	extends FutureTask<T> {
		//
		private final static class PutObjectCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			private final ObjectContainerMock<T> index;
			private final String oid;
			private final T obj;
			//
			private PutObjectCall(final ObjectContainerMock<T> index, final String oid, final T obj) {
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
		public PutObjectTask(final ObjectContainerMock<T> index, final String oid, final T obj) {
			super(new PutObjectCall<>(index, oid, obj));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> submitGet(final String oid)
	throws InterruptedException {
		final FutureTask<T> getTask = new GetObjectTask<>(this, oid);
		seqWorker.submit(getTask);
		return getTask;
	}
	//
	private final static class GetObjectTask<T extends DataObjectMock>
	extends FutureTask<T> {
		//
		private final static class GetObjectCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			private final ObjectContainerMock<T> index;
			private final String oid;
			//
			private GetObjectCall(final ObjectContainerMock<T> index, final String oid) {
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
		public GetObjectTask(final ObjectContainerMock<T> index, final String oid) {
			super(new GetObjectCall<>(index, oid));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> submitRemove(final String oid)
	throws InterruptedException {
		final FutureTask<T> removeTask = new RemoveObjectTask<>(this, oid);
		seqWorker.submit(removeTask);
		return removeTask;
	}
	//
	private final static class RemoveObjectTask<T extends DataObjectMock>
	extends FutureTask<T> {
		//
		private final static class RemoveObjectCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			private final ObjectContainerMock<T> index;
			private final String oid;
			//
			private RemoveObjectCall(final ObjectContainerMock<T> index, final String oid) {
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
		public RemoveObjectTask(final ObjectContainerMock<T> index, final String oid) {
			super(new RemoveObjectCall<>(index, oid));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> submitList(final String oid, final Collection<T> buff, final int limit)
	throws InterruptedException {
		final FutureTask<T> listTask = new ListObjectTask<>(this, oid, buff, limit);
		seqWorker.submit(listTask);
		return listTask;
	}
	//
	public final static class ListObjectTask<T extends DataObjectMock>
	extends FutureTask<T> {
		//
		private final static class ListObjectCall<T extends DataObjectMock>
		implements Callable<T> {
			//
			private final ObjectContainerMock<T> index;
			private final String oid;
			private final Collection<T> buff;
			private final int limit;
			//
			private ListObjectCall(
				final ObjectContainerMock<T> index, final String oid, final Collection<T> buff,
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
		public ListObjectTask(
			final ObjectContainerMock<T> index, final String oid, final Collection<T> buff,
			final int limit
		) {
			super(new ListObjectCall<>(index, oid, buff, limit));
		}
	}
}
