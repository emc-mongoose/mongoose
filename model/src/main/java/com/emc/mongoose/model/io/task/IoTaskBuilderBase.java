package com.emc.mongoose.model.io.task;

import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

/**
 Created by kurila on 14.07.16.
 */
public abstract class IoTaskBuilderBase<I extends Item, O extends IoTask<I, R>, R extends IoResult>
implements IoTaskBuilder<I, O, R> {
	
	protected volatile IoType ioType = IoType.CREATE; // by default
	protected volatile String srcPath = null;
	protected final int originCode = hashCode();

	@Override
	public final int getOriginCode() {
		return originCode;
	}
	
	@Override
	public final IoType getIoType() {
		return ioType;
	}

	@Override
	public final IoTaskBuilderBase<I, O, R> setIoType(final IoType ioType) {
		this.ioType = ioType;
		return this;
	}

	@Override
	public final String getSrcPath() {
		return srcPath;
	}

	@Override
	public final IoTaskBuilderBase<I, O, R> setSrcPath(final String srcPath) {
		this.srcPath = srcPath;
		return this;
	}

	/*@Override @SuppressWarnings("unchecked")
	public O getInstance(final I item, final String dstPath)
	throws IOException {
		return (O) new IoTaskBase<>(originCode, ioType, item, srcPath, dstPath);
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		for(final I item : items) {
			tasks.add((O) new IoTaskBase<>(originCode, ioType, item, srcPath, null));
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final String dstPath)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		for(final I item : items) {
			tasks.add((O) new IoTaskBase<>(originCode, ioType, item, srcPath, dstPath));
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final List<String> dstPaths)
	throws IOException {
		final int n = items.size();
		if(dstPaths.size() != n) {
			throw new IllegalArgumentException("Items count and paths count should be equal");
		}
		final List<O> tasks = new ArrayList<>(n);
		for(int i = 0; i < n; i ++) {
			tasks.add(
				(O) new IoTaskBase<>(originCode, ioType, items.get(i), srcPath, dstPaths.get(i))
			);
		}
		return tasks;
	}*/
}
