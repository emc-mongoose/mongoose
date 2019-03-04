package com.emc.mongoose.base.item.op.path;

import static java.lang.System.nanoTime;

import com.emc.mongoose.base.item.PathItem;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.OperationImpl;
import com.emc.mongoose.base.storage.Credential;

/** Created by kurila on 30.01.17. */
public class PathOperationImpl<I extends PathItem> extends OperationImpl<I>
				implements PathOperation<I> {

	protected volatile long countBytesDone;
	protected volatile long respDataTimeStart;

	public PathOperationImpl() {
		super();
	}

	public PathOperationImpl(
					final int originIndex, final OpType opType, final I item, final Credential credential) {
		super(originIndex, opType, item, null, null, credential);
		item.reset();
	}

	protected PathOperationImpl(final PathOperationImpl<I> other) {
		super(other);
		this.countBytesDone = other.countBytesDone;
		this.respDataTimeStart = other.respDataTimeStart;
	}

	@Override
	public PathOperationImpl<I> result() {
		buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return new PathOperationImpl<>(this);
	}

	@Override
	public long countBytesDone() {
		return countBytesDone;
	}

	@Override
	public void countBytesDone(final long n) {
		this.countBytesDone = n;
	}

	@Override
	public long respDataTimeStart() {
		return respDataTimeStart;
	}

	@Override
	public void startDataResponse() {
		respDataTimeStart = START_OFFSET_MICROS + nanoTime() / 1000;
	}
}
