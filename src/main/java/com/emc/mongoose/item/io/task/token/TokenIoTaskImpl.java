package com.emc.mongoose.item.io.task.token;

import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.item.io.task.IoTaskImpl;
import com.emc.mongoose.item.TokenItem;
import com.emc.mongoose.storage.Credential;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static java.lang.System.nanoTime;

/**
 Created by kurila on 20.10.15.
 */
public class TokenIoTaskImpl<I extends TokenItem>
extends IoTaskImpl<I>
implements TokenIoTask<I> {
	
	protected transient volatile long countBytesDone;
	protected transient volatile long respDataTimeStart;
	
	public TokenIoTaskImpl() {
	}

	public TokenIoTaskImpl(
		final int originIndex, final IoType ioType, final I item, final Credential credential
	) {
		super(originIndex, ioType, item, null, null, credential);
	}
	
	protected TokenIoTaskImpl(final TokenIoTaskImpl<I> other) {
		super(other);
		this.countBytesDone = other.countBytesDone;
		this.respDataTimeStart = other.respDataTimeStart;
	}

	@Override @SuppressWarnings("unchecked")
	public TokenIoTaskImpl<I> result() {
		return new TokenIoTaskImpl<>(this);
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
	public long getCountBytesDone() {
		return countBytesDone;
	}
	
	@Override
	public void setCountBytesDone(final long n) {
		this.countBytesDone = n;
	}
	
	@Override
	public long getRespDataTimeStart() {
		return respDataTimeStart;
	}
	
	@Override
	public void startDataResponse() {
		respDataTimeStart = START_OFFSET_MICROS + nanoTime() / 1000;
	}
	
	
}
