package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.concurrent.Sequencer;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
//
import org.apache.commons.collections4.map.LRUMap;
//
import org.apache.http.concurrent.BasicFuture;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
/**
 Created by kurila on 31.07.15.
 */
public final class BasicObjectContainerMock<T extends DataObjectMock>
extends LRUMap<String, T>
implements ObjectContainerMock<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final String name;
	private final Sequencer seqWorker;
	//
	public BasicObjectContainerMock(final String name, final int capacity) {
		super(capacity);
		this.name = name;
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		seqWorker = new Sequencer(
			"containerSequencer<" + name + ">", true, rtConfig.getBatchSize(),
			rtConfig.getBatchSize()
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
	protected final void moveToMRU(final LinkEntry<String, T> entry) {
		// disable entry moving to MRU in case of access
		// it's required to make list method (right below) working (keeping the linked list order)
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
			if(nextEntry == null || nextEntry.getKey() == null) {
				break;
			}
			buffDst.add(nextEntry.getValue());
		}
		LOG.debug(
			Markers.MSG, "Container \"{}\": listed {} objects beginning after oid \"{}\"",
			name, buffDst.size(), afterOid
		);
		return (nextEntry == null || nextEntry.getKey() == null) ? null : nextEntry.getValue();
	}
	//
	@Override
	public final void close() {
		seqWorker.interrupt();
		LOG.debug(Markers.MSG, "{}: interrupted", seqWorker.getName());
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
	public final Future<T> submitPut(final T obj)
	throws InterruptedException {
		return seqWorker.submit(new PutObjectTask<>(this, obj));
	}
	//
	private final static class PutObjectTask<T extends DataObjectMock>
	extends BasicFuture<T>
	implements RunnableFuture<T> {
		//
		private final ObjectContainerMock<T> index;
		private final T obj;
		//
		private PutObjectTask(final ObjectContainerMock<T> index, final T obj) {
			super(null);
			this.index = index;
			this.obj = obj;
		}
		//
		@Override
		public final void run() {
			completed(index.put(obj.getId(), obj));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> submitGet(final String oid)
	throws InterruptedException {
		return seqWorker.submit(new GetObjectTask<>(this, oid));
	}
	//
	private final static class GetObjectTask<T extends DataObjectMock>
	extends BasicFuture<T>
	implements RunnableFuture<T> {
		//
		private final ObjectContainerMock<T> index;
		private final String oid;
		//
		private GetObjectTask(final ObjectContainerMock<T> index, final String oid) {
			super(null);
			this.index = index;
			this.oid = oid;
		}
		//
		@Override
		public final void run() {
			completed(index.get(oid));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> submitRemove(final String oid)
	throws InterruptedException {
		return seqWorker.submit(new RemoveObjectTask<>(this, oid));
	}
	//
	private final static class RemoveObjectTask<T extends DataObjectMock>
	extends BasicFuture<T>
	implements RunnableFuture<T> {
		//
		private final ObjectContainerMock<T> index;
		private final String oid;
		//
		private RemoveObjectTask(final ObjectContainerMock<T> index, final String oid) {
			super(null);
			this.index = index;
			this.oid = oid;
		}
		//
		@Override
		public final void run() {
			completed(index.remove(oid));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final Future<T> submitList(final String oid, final Collection<T> buff, final int limit)
	throws InterruptedException {
		return seqWorker.submit(new ListObjectTask<>(this, oid, buff, limit));
	}
	//
	public final static class ListObjectTask<T extends DataObjectMock>
	extends BasicFuture<T>
	implements RunnableFuture<T> {
		//
		private final ObjectContainerMock<T> index;
		private final String oid;
		private final Collection<T> buff;
		private final int limit;
		//
		private ListObjectTask(
			final ObjectContainerMock<T> index, final String oid, final Collection<T> buff,
			final int limit
		) {
			super(null);
			this.index = index;
			this.oid = oid;
			this.buff = buff;
			this.limit = limit;
		}
		//
		@Override
		public final void run() {
			completed(index.list(oid, buff, limit));
		}
	}
}
