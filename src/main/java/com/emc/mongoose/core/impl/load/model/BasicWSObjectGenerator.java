package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.core.impl.data.BasicWSObject;
//
import java.io.IOException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 15.12.14.
 */
public final class BasicWSObjectGenerator<T extends BasicWSObject>
extends DataItemGeneratorBase<T> {
	//
	public BasicWSObjectGenerator(
		final long maxCount, final long minObjSize, final long maxObjSize, final float objSizeBias
	) {
		super(maxCount, minObjSize, maxObjSize, objSizeBias);
	}
	//
	@SuppressWarnings("unchecked")
	protected T produceSpecificDataItem(final long nextSize)
	throws IOException {
		return (T) new BasicWSObject(nextSize);
	}
	//
	@Override
	public final void await()
		throws InterruptedException {
		join();
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
		throws InterruptedException {
		timeUnit.timedJoin(this, timeOut);
	}
}
