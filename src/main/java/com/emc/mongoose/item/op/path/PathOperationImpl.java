package com.emc.mongoose.item.op.path;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.OperationImpl;
import com.emc.mongoose.item.PathItem;
import com.emc.mongoose.storage.Credential;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static java.lang.System.nanoTime;

/**
 Created by kurila on 30.01.17.
 */
public class PathOperationImpl<I extends PathItem>
extends OperationImpl<I>
implements PathOperation<I> {
	
	protected volatile long countBytesDone;
	protected volatile long respDataTimeStart;
	
	public PathOperationImpl() {
		super();
	}
	
	public PathOperationImpl(
		final int originIndex, final OpType opType, final I item, final Credential credential
	) {
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
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeLong(countBytesDone);
		out.writeLong(respDataTimeStart);
	}

	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		countBytesDone = in.readLong();
		respDataTimeStart = in.readLong();
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
