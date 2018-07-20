package com.emc.mongoose.item.op.token;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.OperationImpl;
import com.emc.mongoose.item.TokenItem;
import com.emc.mongoose.storage.Credential;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static java.lang.System.nanoTime;

/**
 Created by kurila on 20.10.15.
 */
public class TokenOperationImpl<I extends TokenItem>
extends OperationImpl<I>
implements TokenOperation<I> {
	
	protected transient volatile long countBytesDone;
	protected transient volatile long respDataTimeStart;
	
	public TokenOperationImpl() {
	}

	public TokenOperationImpl(
		final int originIndex, final OpType opType, final I item, final Credential credential
	) {
		super(originIndex, opType, item, null, null, credential);
	}
	
	protected TokenOperationImpl(final TokenOperationImpl<I> other) {
		super(other);
		this.countBytesDone = other.countBytesDone;
		this.respDataTimeStart = other.respDataTimeStart;
	}

	@Override @SuppressWarnings("unchecked")
	public TokenOperationImpl<I> result() {
		return new TokenOperationImpl<>(this);
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
