package com.emc.mongoose.item.io.task;

import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.storage.Credential;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static java.lang.System.nanoTime;

/**
 Created by kurila on 20.10.15.
 */
public class BasicIoTask<I extends Item>
implements IoTask<I> {
	
	protected int originIndex;
	protected IoType ioType;
	protected I item;
	protected String srcPath;
	protected String dstPath;
	protected Credential credential;
	
	protected volatile String nodeAddr;
	protected volatile Status status;
	protected volatile long reqTimeStart;
	protected volatile long reqTimeDone;
	protected volatile long respTimeStart;
	protected volatile long respTimeDone;
	
	public BasicIoTask() {
	}
	
	public BasicIoTask(
		final int originIndex, final IoType ioType, final I item, final String srcPath,
		final String dstPath, final Credential credential
	) {
		this.originIndex = originIndex;
		this.ioType = ioType;
		this.item = item;

		final String itemName = item.getName();
		final int lastSlashIndex = itemName.lastIndexOf(SLASH);
		if(lastSlashIndex > 0 && lastSlashIndex < itemName.length()) {
			this.srcPath = itemName.substring(0, lastSlashIndex);
			item.setName(itemName.substring(lastSlashIndex + 1));
		} else {
			this.srcPath = srcPath;
		}

		if(dstPath == null) {
			if(
				IoType.READ.equals(ioType) || IoType.UPDATE.equals(ioType) ||
				IoType.DELETE.equals(ioType)
			) {
				this.dstPath = this.srcPath;
			}
		} else {
			this.dstPath = dstPath;
		}
		
		this.credential = credential;
	}

	protected BasicIoTask(final BasicIoTask<I> other) {
		this.originIndex = other.originIndex;
		this.ioType = other.ioType;
		this.item = other.item;
		this.srcPath = other.srcPath;
		this.dstPath = other.dstPath;
		this.credential = other.credential;
		this.nodeAddr = other.nodeAddr;
		this.status = other.status;
		this.reqTimeStart = other.reqTimeStart;
		this.reqTimeDone = other.reqTimeDone;
		this.respTimeStart = other.respTimeStart;
		this.respTimeDone = other.respTimeDone;
	}

	@Override
	public BasicIoTask<I> result() {
		buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return new BasicIoTask<>(this);
	}

	@Override
	public void reset() {
		item.reset();
		nodeAddr = null;
		status = Status.PENDING;
		reqTimeStart = reqTimeDone = respTimeStart = respTimeDone = 0;
	}
	
	@Override
	public final int originIndex() {
		return originIndex;
	}
	
	@Override
	public final I item() {
		return item;
	}
	
	@Override
	public final IoType ioType() {
		return ioType;
	}

	@Override
	public final String nodeAddr() {
		return nodeAddr;
	}
	
	@Override
	public final void nodeAddr(final String nodeAddr) {
		this.nodeAddr = nodeAddr;
	}
	
	@Override
	public final Status status() {
		return status;
	}
	
	@Override
	public final void status(final Status status) {
		this.status = status;
	}
	
	@Override
	public final String srcPath() {
		return srcPath;
	}
	
	@Override
	public final void srcPath(final String srcPath) {
		this.srcPath = srcPath;
	}
	
	@Override
	public final String dstPath() {
		return dstPath;
	}
	
	@Override
	public final void dstPath(final String dstPath) {
		this.dstPath = dstPath;
	}
	
	@Override
	public final Credential credential() {
		return credential;
	}
	
	@Override
	public final void credential(final Credential credential) {
		this.credential = credential;
	}

	@Override
	public final void startRequest() {
		reqTimeStart = START_OFFSET_MICROS + nanoTime() / 1000;
		status = Status.ACTIVE;
	}

	@Override
	public final void finishRequest() {
		reqTimeDone = START_OFFSET_MICROS + nanoTime() / 1000;
		if(respTimeStart > 0) {
			throw new IllegalStateException(
				"Request is finished (" + reqTimeDone + ") after the response is started (" +
				respTimeStart + ")"
			);
		}
	}

	@Override
	public final void startResponse() {
		respTimeStart = START_OFFSET_MICROS + nanoTime() / 1000;
		if(reqTimeDone > respTimeStart) {
			throw new IllegalStateException(
				"Response is started (" + respTimeStart + ") before the request is finished (" +
				reqTimeDone + ")"
			);
		}
	}

	@Override
	public void finishResponse() {
		respTimeDone = START_OFFSET_MICROS + nanoTime() / 1000;
		if(respTimeStart == 0) {
			throw new IllegalStateException("Response is finished while not started");
		}
	}

	@Override
	public final long reqTimeStart() {
		return reqTimeStart;
	}

	@Override
	public final long reqTimeDone() {
		return reqTimeDone;
	}

	@Override
	public final long respTimeStart() {
		return respTimeStart;
	}

	@Override
	public final long respTimeDone() {
		return respTimeDone;
	}

	@Override
	public final long duration() {
		return respTimeDone - reqTimeStart;
	}

	@Override
	public final long latency() {
		return respTimeStart - reqTimeDone;
	}

	protected static final ThreadLocal<StringBuilder> STRB = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	
	@Override
	public String toString() {
		final StringBuilder strb = STRB.get();
		strb.setLength(0);
		return strb
			.append(ioType.name()).append(',')
			.append(item.toString()).append(',')
			.append(dstPath == null ? "" : dstPath).append(',')
			.toString();
	}
	
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeInt(originIndex);
		out.writeInt(ioType.ordinal());
		out.writeObject(item);
		out.writeUTF(srcPath == null ? "" : srcPath);
		out.writeUTF(dstPath == null ? "" : dstPath);
		out.writeObject(credential);
		out.writeUTF(nodeAddr == null ? "" : nodeAddr);
		out.writeInt(status == null ? Status.PENDING.ordinal() : status.ordinal());
		out.writeLong(reqTimeStart);
		out.writeLong(reqTimeDone);
		out.writeLong(respTimeStart);
		out.writeLong(respTimeDone);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		originIndex = in.readInt();
		ioType = IoType.values()[in.readInt()];
		item = (I) in.readObject();
		srcPath = in.readUTF();
		dstPath = in.readUTF();
		credential = (Credential) in.readObject();
		nodeAddr = in.readUTF();
		status = Status.values()[in.readInt()];
		reqTimeStart = in.readLong();
		reqTimeDone = in.readLong();
		respTimeStart = in.readLong();
		respTimeDone = in.readLong();
	}

	@Override
	public final int hashCode() {
		return originIndex ^ ioType.ordinal() ^ item.hashCode();
	}
}
