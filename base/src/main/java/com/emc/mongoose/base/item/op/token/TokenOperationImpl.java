package com.emc.mongoose.base.item.op.token;

import static java.lang.System.nanoTime;

import com.emc.mongoose.base.item.TokenItem;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.OperationImpl;
import com.emc.mongoose.base.storage.Credential;

/** Created by kurila on 20.10.15. */
public class TokenOperationImpl<I extends TokenItem> extends OperationImpl<I>
				implements TokenOperation<I> {

	protected volatile long countBytesDone;
	protected volatile long respDataTimeStart;

	public TokenOperationImpl() {}

	public TokenOperationImpl(
					final int originIndex, final OpType opType, final I item, final Credential credential) {
		super(originIndex, opType, item, null, null, credential);
	}

	protected TokenOperationImpl(final TokenOperationImpl<I> other) {
		super(other);
		this.countBytesDone = other.countBytesDone;
		this.respDataTimeStart = other.respDataTimeStart;
	}

	@Override
	@SuppressWarnings("unchecked")
	public TokenOperationImpl<I> result() {
		return new TokenOperationImpl<>(this);
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
