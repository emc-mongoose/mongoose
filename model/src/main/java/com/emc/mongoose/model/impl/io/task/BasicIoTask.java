package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.LoadType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
/**
 Created by kurila on 20.10.15.
 */
public class BasicIoTask<I extends Item>
implements IoTask<I> {
	
	protected LoadType ioType;
	protected I item;
	protected String srcPath;
	protected String dstPath;
	
	protected volatile String nodeAddr;
	protected volatile Status status;
	protected volatile long reqTimeStart;
	protected volatile long reqTimeDone;
	protected volatile long respTimeStart;
	protected volatile long respTimeDone;
	
	public BasicIoTask() {
	}
	
	public BasicIoTask(final LoadType ioType, final I item, final String dstPath) {
		this.ioType = ioType;
		this.item = item;
		final String itemName = item.getName();
		final int lastSlashIndex = itemName.lastIndexOf(SLASH);
		if(lastSlashIndex > 0 && lastSlashIndex < itemName.length()) {
			srcPath = itemName.substring(0, lastSlashIndex);
			item.setName(itemName.substring(lastSlashIndex + 1));
		} else {
			srcPath = null;
		}
		this.dstPath = dstPath;
		reset();
	}
	
	@Override
	public void reset() {
		item.reset();
		nodeAddr = null;
		status = Status.PENDING;
		reqTimeStart = reqTimeDone = respTimeStart = reqTimeDone = 0;
	}
	
	@Override
	public final I getItem() {
		return item;
	}
	
	@Override
	public final LoadType getLoadType() {
		return ioType;
	}
	
	@Override
	public final String getNodeAddr() {
		return nodeAddr;
	}
	
	@Override
	public final void setNodeAddr(final String nodeAddr) {
		this.nodeAddr = nodeAddr;
	}
	
	@Override
	public final Status getStatus() {
		return status;
	}
	
	@Override
	public final void setStatus(final Status status) {
		this.status = status;
	}
	
	@Override
	public final long getReqTimeStart() {
		return reqTimeStart;
	}
	
	@Override
	public final void startRequest() {
		reqTimeStart = System.nanoTime() / 1000;
		status = Status.ACTIVE;
	}

	@Override
	public final void finishRequest() {
		reqTimeDone = System.nanoTime() / 1000;
	}

	@Override
	public final void startResponse() {
		respTimeStart = System.nanoTime() / 1000;
	}

	@Override
	public final void finishResponse() {
		respTimeDone = System.nanoTime() / 1000;
	}
	
	@Override
	public final int getDuration() {
		return (int) (respTimeDone - reqTimeStart);
	}
	
	@Override
	public final int getLatency() {
		return (int) (respTimeStart - reqTimeDone);
	}

	@Override
	public final String getSrcPath() {
		return srcPath;
	}

	@Override
	public final String getDstPath() {
		return dstPath;
	}
	
	protected final static ThreadLocal<StringBuilder> STRB = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	
	@Override
	public String toString() {
		final StringBuilder strb = STRB.get();
		strb.setLength(0);
		final long respLatency = getLatency();
		final long reqDuration = getDuration();
		return strb
			.append(ioType.ordinal()).append(',')
			.append(
				dstPath == null ?
					item.getName() :
					dstPath.endsWith(SLASH) ?
						dstPath + item.getName() :
						dstPath + SLASH + item.getName()
			)
			.append(',')
			.append(status.code).append(',')
			.append(reqTimeStart).append(',')
			.append(respLatency > 0 ? respLatency : 0).append(',')
			.append(reqDuration)
			.toString();
	}
	
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeObject(ioType);
		out.writeObject(item);
		out.writeObject(dstPath);
		out.writeObject(nodeAddr);
		out.writeObject(status);
		out.writeLong(reqTimeStart);
		out.writeLong(reqTimeDone);
		out.writeLong(respTimeStart);
		out.writeLong(respTimeDone);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		ioType = (LoadType) in.readObject();
		item = (I) in.readObject();
		dstPath = (String) in.readObject();
		nodeAddr = (String) in.readObject();
		status = (Status) in.readObject();
		reqTimeStart = in.readLong();
		reqTimeDone = in.readLong();
		respTimeStart = in.readLong();
		respTimeDone = in.readLong();
	}

	@Override
	public final int hashCode() {
		return item.hashCode();
	}
}
