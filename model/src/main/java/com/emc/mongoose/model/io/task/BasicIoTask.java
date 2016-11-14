package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadType;

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
	
	public BasicIoTask(
		final LoadType ioType, final I item, final String srcPath, final String dstPath
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

	public static class BasicIoResult
	implements IoResult {

		private final LoadType loadType;
		private final Status status;
		private final String storageDriverAddr;
		private final String storageNodeAddr;
		private final String itemInfo;
		private final long reqTimeStart;
		private final long duration;
		private final long latency;

		public BasicIoResult(
			final LoadType loadType, final Status status, final String storageDriverAddr,
			final String storageNodeAddr, final String itemInfo,
			final long reqTimeStart, final long duration, final long latency
		) {
			this.loadType = loadType;
			this.status = status;
			this.storageDriverAddr = storageDriverAddr;
			this.storageNodeAddr = storageNodeAddr;
			this.itemInfo = itemInfo;
			this.reqTimeStart = reqTimeStart;
			this.duration = duration;
			this.latency = latency;
		}

		@Override
		public LoadType getLoadType() {
			return loadType;
		}

		@Override
		public Status getStatus() {
			return status;
		}

		@Override
		public String getStorageDriverAddr() {
			return storageDriverAddr;
		}

		@Override
		public String getStorageNodeAddr() {
			return storageNodeAddr;
		}

		@Override
		public String getItemInfo() {
			return itemInfo;
		}

		@Override
		public long getTimeStart() {
			return reqTimeStart;
		}

		@Override
		public long getDuration() {
			return duration;
		}

		@Override
		public long getLatency() {
			return latency;
		}
	}

	protected final String getItemPath() {
		if(dstPath == null) {
			if(srcPath != null) {
				if(srcPath.endsWith(SLASH)) {
					return srcPath + item.getName();
				} else {
					return srcPath + SLASH + item.getName();
				}
			}
		} else {
			if(dstPath.endsWith(SLASH)) {
				return dstPath + item.getName();
			} else {
				return dstPath + SLASH + item.getName();
			}
		}
		return SLASH + item.getName();
	}

	@Override
	public BasicIoResult getIoResult() {
		return new BasicIoResult(
			ioType, status, STORAGE_DRIVER_ADDR, nodeAddr, item.toString(getItemPath()),
			reqTimeStart, respTimeDone - reqTimeStart, respTimeStart - reqTimeDone
		);
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
		ioType = (LoadType) in.readObject();
		item = (I) in.readObject();
		dstPath = (String) in.readObject();
	}

	@Override
	public final int hashCode() {
		return item.hashCode();
	}
}
