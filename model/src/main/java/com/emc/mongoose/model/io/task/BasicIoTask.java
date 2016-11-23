package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.io.task.result.BasicIoResult;
import com.emc.mongoose.model.io.task.result.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 Created by kurila on 20.10.15.
 */
public class BasicIoTask<I extends Item, R extends IoResult>
implements IoTask<I, R> {
	
	protected IoType ioType;
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
	
	public BasicIoTask(
		final IoType ioType, final I item, final String srcPath, final String dstPath
	) {
		this.ioType = ioType;
		this.item = item;
		if(srcPath == null) {
			final String itemName = item.getName();
			final int lastSlashIndex = itemName.lastIndexOf(SLASH);
			if(lastSlashIndex > 0 && lastSlashIndex < itemName.length()) {
				this.srcPath = itemName.substring(0, lastSlashIndex);
				item.setName(itemName.substring(lastSlashIndex + 1));
			} else {
				this.srcPath = null;
			}
		} else {
			this.srcPath = srcPath;
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
	public final IoType getIoType() {
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
	public final String getSrcPath() {
		return srcPath;
	}
	
	@Override
	public final void setSrcPath(final String srcPath) {
		this.srcPath = srcPath;
	}
	
	@Override
	public final String getDstPath() {
		return dstPath;
	}
	
	@Override
	public final void setDstPath(final String dstPath) {
		this.dstPath = dstPath;
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
	public final long getReqTimeStart() {
		return reqTimeStart;
	}

	@Override
	public final long getReqTimeDone() {
		return reqTimeDone;
	}

	@Override
	public final long getRespTimeStart() {
		return respTimeStart;
	}

	@Override
	public final long getRespTimeDone() {
		return respTimeDone;
	}

	protected static String getItemPath(
		final String itemName, final String srcPath, final String dstPath
	) {
		if(dstPath == null) {
			if(srcPath != null) {
				if(srcPath.endsWith("/")) {
					return srcPath + itemName;
				} else {
					return srcPath + "/" + itemName;
				}
			}
		} else {
			if(dstPath.endsWith("/")) {
				return dstPath + itemName;
			} else {
				return dstPath + "/" + itemName;
			}
		}
		return "/" + itemName;
	}

	@Override @SuppressWarnings("unchecked")
	public R getResult(
		final String hostAddr,
		final boolean useStorageDriverResult,
		final boolean useStorageNodeResult,
		final boolean useItemPathResult,
		final boolean useIoTypeCodeResult,
		final boolean useStatusCodeResult,
		final boolean useReqTimeStartResult,
		final boolean useDurationResult,
		final boolean useRespLatencyResult,
		final boolean useDataLatencyResult,
		final boolean useTransferSizeResult
	) {
		return (R) new BasicIoResult(
			useStorageDriverResult ? hostAddr : null,
			useStorageNodeResult ? nodeAddr : null,
			useItemPathResult ? getItemPath(item.getName(), srcPath, dstPath) : null,
			useIoTypeCodeResult ? ioType.ordinal() : - 1,
			useStatusCodeResult ? status.ordinal() : - 1,
			useReqTimeStartResult ? reqTimeStart : - 1,
			useDurationResult ? respTimeDone - reqTimeStart : - 1,
			useRespLatencyResult ? respTimeStart - reqTimeDone : - 1
		);
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
			.append(dstPath).append(',')
			.toString();
	}
	
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeObject(ioType);
		out.writeObject(item);
		out.writeObject(dstPath);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		ioType = (IoType) in.readObject();
		item = (I) in.readObject();
		dstPath = (String) in.readObject();
	}

	@Override
	public final int hashCode() {
		return item.hashCode();
	}
}
