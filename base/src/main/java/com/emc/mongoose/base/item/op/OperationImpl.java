package com.emc.mongoose.base.item.op;

import static java.lang.System.nanoTime;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.storage.Credential;

/** Created by kurila on 20.10.15. */
public class OperationImpl<I extends Item> implements Operation<I> {

	protected int originIndex;
	protected OpType opType;
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

	public OperationImpl() {}

	public OperationImpl(
					final int originIndex,
					final OpType opType,
					final I item,
					final String srcPath,
					final String dstPath,
					final Credential credential) {
		this.originIndex = originIndex;
		this.opType = opType;
		this.item = item;

		final String itemName = item.name();
		final int lastSlashIndex = itemName.lastIndexOf(SLASH);
		if (lastSlashIndex > 0 && lastSlashIndex < itemName.length()) {
			this.srcPath = itemName.substring(0, lastSlashIndex);
			item.name(itemName.substring(lastSlashIndex + 1));
		} else {
			this.srcPath = srcPath;
		}

		if (dstPath == null) {
			if (OpType.READ.equals(opType)
							|| OpType.UPDATE.equals(opType)
							|| OpType.DELETE.equals(opType)) {
				this.dstPath = this.srcPath;
			}
		} else {
			this.dstPath = dstPath;
		}

		this.credential = credential;
	}

	protected OperationImpl(final OperationImpl<I> other) {
		this.originIndex = other.originIndex;
		this.opType = other.opType;
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
	public OperationImpl<I> result() {
		buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return new OperationImpl<>(this);
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
	public final OpType type() {
		return opType;
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
		if (respTimeStart > 0) {
			throw new IllegalStateException(
							"Request is finished ("
											+ reqTimeDone
											+ ") after the response is started ("
											+ respTimeStart
											+ ")");
		}
	}

	@Override
	public final void startResponse() {
		respTimeStart = START_OFFSET_MICROS + nanoTime() / 1000;
		if (reqTimeDone > respTimeStart) {
			throw new IllegalStateException(
							"Response is started ("
											+ respTimeStart
											+ ") before the request is finished ("
											+ reqTimeDone
											+ ")");
		}
	}

	@Override
	public void finishResponse() {
		respTimeDone = START_OFFSET_MICROS + nanoTime() / 1000;
		if (respTimeStart == 0) {
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

	protected static final ThreadLocal<StringBuilder> STRB = ThreadLocal.withInitial(StringBuilder::new);

	@Override
	public String toString() {
		final StringBuilder strb = STRB.get();
		strb.setLength(0);
		return strb.append(opType.name())
						.append(',')
						.append(item.toString())
						.append(',')
						.append(dstPath == null ? "" : dstPath)
						.append(',')
						.toString();
	}

	@Override
	public final int hashCode() {
		return originIndex ^ opType.ordinal() ^ item.hashCode();
	}
}
