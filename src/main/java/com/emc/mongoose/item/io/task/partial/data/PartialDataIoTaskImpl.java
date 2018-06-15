package com.emc.mongoose.item.io.task.partial.data;

import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.item.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.item.io.task.data.DataIoTaskImpl;
import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.storage.Credential;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 Created by andrey on 23.11.16.
 */
public class PartialDataIoTaskImpl<I extends DataItem>
extends DataIoTaskImpl<I>
implements PartialDataIoTask<I> {

	private int partNumber;
	private CompositeDataIoTask<I> parent;

	public PartialDataIoTaskImpl() {
		super();
	}

	public PartialDataIoTaskImpl(
		final int originIndex, final IoType ioType, final I part, final String srcPath,
		final String dstPath, final Credential credential, final int partNumber,
		final CompositeDataIoTask<I> parent
	) {
		super(originIndex, ioType, part, srcPath, dstPath, credential, null, 0);
		this.partNumber = partNumber;
		this.parent = parent;
	}

	protected PartialDataIoTaskImpl(final PartialDataIoTaskImpl<I> other) {
		super(other);
		this.partNumber = other.partNumber;
		this.parent = other.parent;
	}

	@Override
	public PartialDataIoTaskImpl<I> result() {
		buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return new PartialDataIoTaskImpl<>(this);
	}

	@Override
	public final int partNumber() {
		return partNumber;
	}

	@Override
	public final CompositeDataIoTask<I> parent() {
		return parent;
	}

	@Override
	public final void finishResponse() {
		super.finishResponse();
		parent.markSubTaskCompleted();
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
