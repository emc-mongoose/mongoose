package com.emc.mongoose.model.io.task.partial.data;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.model.io.task.data.BasicDataIoTask;
import com.emc.mongoose.model.item.DataItem;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 Created by andrey on 23.11.16.
 */
public class BasicPartialDataIoTask<I extends DataItem>
extends BasicDataIoTask<I>
implements PartialDataIoTask<I> {

	private int partNumber;
	private CompositeDataIoTask<I> parent;

	public BasicPartialDataIoTask() {
		super();
	}

	public BasicPartialDataIoTask(
		final int originCode, final IoType ioType, final I part, final String srcPath,
		final String dstPath, final String uid, final String secret, final int partNumber,
		final CompositeDataIoTask<I> parent
	) {
		super(originCode, ioType, part, srcPath, dstPath, uid, secret, null, 0);
		this.partNumber = partNumber;
		this.parent = parent;
	}

	protected BasicPartialDataIoTask(final BasicPartialDataIoTask<I> other) {
		super(other);
		this.partNumber = other.partNumber;
		this.parent = other.parent;
	}

	@Override
	public BasicPartialDataIoTask<I> getResult() {
		buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return new BasicPartialDataIoTask<>(this);
	}

	@Override
	public final int getPartNumber() {
		return partNumber;
	}

	@Override
	public final CompositeDataIoTask<I> getParent() {
		return parent;
	}

	@Override
	public final void finishResponse() {
		super.finishResponse();
		parent.subTaskCompleted();
	}

	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeInt(partNumber);
		out.writeObject(parent);
	}

	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.partNumber = in.readInt();
		this.parent = (CompositeDataIoTask<I>) in.readObject();
	}
}
