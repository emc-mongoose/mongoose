package com.emc.mongoose.web.load.impl;
//
import com.emc.mongoose.base.load.impl.NewDataProducerBase;
import com.emc.mongoose.web.data.impl.BasicWSObject;
//
import java.io.IOException;
/**
 Created by kurila on 15.12.14.
 */
final class BasicNewDataProducer<T extends BasicWSObject>
extends NewDataProducerBase<T> {
	//
	protected BasicNewDataProducer(
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
}
