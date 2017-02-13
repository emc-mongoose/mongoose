package com.emc.mongoose.model.io.task;

import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import static java.lang.System.nanoTime;

/**
 Created by kurila on 20.10.15.
 */
public class BasicIoTask<I extends Item, R extends IoResult<I>>
implements IoTask<I, R> {
	
	protected int originCode;
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
		final int originCode, final IoType ioType, final I item, final String srcPath,
		final String dstPath
	) {
		this.originCode = originCode;
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
	}
	
	public BasicIoTask(final R ioResult) {
		this(-1, IoType.values()[ioResult.getIoTypeCode()], ioResult.getItem(), null, null);
	}

	@Override
	public void reset() {
		item.reset();
		nodeAddr = null;
		status = Status.PENDING;
		reqTimeStart = reqTimeDone = respTimeStart = respTimeDone = 0;
	}
	
	@Override
	public final int getOriginCode() {
		return originCode;
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
		reqTimeStart = START_OFFSET_MICROS + nanoTime() / 1000;
		status = Status.ACTIVE;
	}

	@Override
	public final void finishRequest() {
		reqTimeDone = START_OFFSET_MICROS + nanoTime() / 1000;
	}

	@Override
	public final void startResponse() {
		respTimeStart = START_OFFSET_MICROS + nanoTime() / 1000;
	}

	@Override
	public void finishResponse() {
		respTimeDone = START_OFFSET_MICROS + nanoTime() / 1000;
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
	
	public static class BasicIoResult<I extends Item>
	implements IoResult<I> {
		
		private String storageDriverAddr;
		private String storageNodeAddr;
		private I item;
		private int ioTypeCode;
		private int statusCode;
		private long reqTimeStart;
		private long duration;
		private long latency;

		public BasicIoResult() {
		}
		
		public BasicIoResult(
			final String storageDriverAddr, final String storageNodeAddr, final I item,
			final int ioTypeCode, final int statusCode, final long reqTimeStart,
			final long duration, final long latency
		) {
			this.storageDriverAddr = storageDriverAddr;
			this.storageNodeAddr = storageNodeAddr;
			this.item = item;
			this.ioTypeCode = ioTypeCode;
			this.statusCode = statusCode;
			this.reqTimeStart = reqTimeStart;
			this.duration = duration;
			this.latency = duration > latency ? latency : -1;
		}
		
		@Override
		public final String getStorageDriverAddr() {
			return storageDriverAddr;
		}
		
		@Override
		public final String getStorageNodeAddr() {
			return storageNodeAddr;
		}

		@Override
		public final I getItem() {
			return item;
		}
		
		@Override
		public final int getIoTypeCode() {
			return ioTypeCode;
		}
		
		@Override
		public final int getStatusCode() {
			return statusCode;
		}
		
		@Override
		public final long getTimeStart() {
			return reqTimeStart;
		}
		
		@Override
		public final long getDuration() {
			return duration;
		}
		
		@Override
		public final long getLatency() {
			return latency;
		}
		
		@Override
		public void writeExternal(final ObjectOutput out)
		throws IOException {
			out.writeUTF(storageDriverAddr == null ? "" : storageDriverAddr);
			out.writeUTF(storageNodeAddr == null ? "" : storageNodeAddr);
			out.writeObject(item);
			out.writeInt(ioTypeCode);
			out.writeInt(statusCode);
			out.writeLong(reqTimeStart);
			out.writeLong(duration);
			out.writeLong(latency);
		}
		
		@Override @SuppressWarnings("unchecked")
		public void readExternal(final ObjectInput in)
		throws IOException, ClassNotFoundException {
			storageDriverAddr = in.readUTF();
			storageNodeAddr = in.readUTF();
			item = (I) in.readObject();
			ioTypeCode = in.readInt();
			statusCode = in.readInt();
			reqTimeStart = in.readLong();
			duration = in.readLong();
			latency = in.readLong();
		}
		
		protected static final ThreadLocal<StringBuilder> STRB = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		};
		
		@Override
		public final String toString() {
			final StringBuilder strb = STRB.get();
			strb.setLength(0);
			if(storageNodeAddr != null && !storageNodeAddr.isEmpty()) {
				strb.append("endpoint: ").append(storageNodeAddr).append(", ");
			}
			if(storageDriverAddr != null && !storageDriverAddr.isEmpty()) {
				strb.append("client: ").append(storageDriverAddr).append(", ");
			}
			if(item != null) {
				strb.append("item: ").append(item.getName()).append(", ");
			}
			if(ioTypeCode > -1) {
				strb.append("operation: ").append(IoType.values()[ioTypeCode]).append(", ");
			}
			if(statusCode > -1) {
				strb.append("status: ").append(Status.values()[statusCode]);
			}
			return strb.toString();
		}
	}
	
	
	@Override @SuppressWarnings("unchecked")
	public R getResult(
		final String hostAddr,
		final boolean useStorageDriverResult,
		final boolean useStorageNodeResult,
		final boolean useItemInfoResult,
		final boolean useIoTypeCodeResult,
		final boolean useStatusCodeResult,
		final boolean useReqTimeStartResult,
		final boolean useDurationResult,
		final boolean useRespLatencyResult,
		final boolean useDataLatencyResult,
		final boolean useTransferSizeResult
	) {
		buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return (R) new BasicIoResult(
			useStorageDriverResult ? hostAddr : null,
			useStorageNodeResult ? nodeAddr : null,
			useItemInfoResult ? item : null,
			useIoTypeCodeResult ? ioType.ordinal() : -1,
			useStatusCodeResult ? status.ordinal() : -1,
			useReqTimeStartResult ? reqTimeStart : -1,
			useDurationResult ? respTimeDone - reqTimeStart : -1,
			useRespLatencyResult ? respTimeStart - reqTimeDone : -1
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
			.append(dstPath == null ? "" : dstPath).append(',')
			.toString();
	}
	
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeInt(originCode);
		out.writeInt(ioType.ordinal());
		out.writeObject(item);
		out.writeUTF(srcPath == null ? "" : srcPath);
		out.writeUTF(dstPath == null ? "" : dstPath);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		originCode = in.readInt();
		ioType = IoType.values()[in.readInt()];
		item = (I) in.readObject();
		srcPath = in.readUTF();
		dstPath = in.readUTF();
	}

	@Override
	public final int hashCode() {
		return item.hashCode();
	}
}
