package com.emc.mongoose.model.io.task.token;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.BasicIoTask;
import com.emc.mongoose.model.item.TokenItem;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import static java.lang.System.nanoTime;

/**
 Created by kurila on 20.10.15.
 */
public class BasicTokenIoTask<I extends TokenItem>
extends BasicIoTask<I>
implements TokenIoTask<I> {
	
	protected transient volatile long countBytesDone;
	protected transient volatile long respDataTimeStart;
	
	public BasicTokenIoTask() {
	}

	public BasicTokenIoTask(
		final int originCode, final IoType ioType, final I item, final String uid,
		final String secret
	) {
		super(originCode, ioType, item, null, null, uid, secret);
	}
	
	protected BasicTokenIoTask(final BasicTokenIoTask<I> other) {
		super(other);
		this.countBytesDone = other.countBytesDone;
		this.respDataTimeStart = other.respDataTimeStart;
	}

	@Override @SuppressWarnings("unchecked")
	public BasicTokenIoTask<I> getResult() {
		return new BasicTokenIoTask<>(this);
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
