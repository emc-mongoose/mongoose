package com.emc.mongoose.object.load.impl.ws;
//
import com.emc.mongoose.base.load.impl.NewDataProducerBase;
import com.emc.mongoose.object.data.impl.WSObjectImpl;
//
import java.io.IOException;
/**
 Created by kurila on 15.12.14.
 */
final class NewDataProducerImpl<T extends WSObjectImpl>
extends NewDataProducerBase<T> {
	//
	protected NewDataProducerImpl(
		final long maxCount, final long minObjSize, final long maxObjSize, final float objSizeBias
	) {
		super(maxCount, minObjSize, maxObjSize, objSizeBias);
	}
	//
	@SuppressWarnings("unchecked")
	protected T produceSpecificDataItem(final long nextSize)
	throws IOException {
		return (T) new WSObjectImpl(nextSize);
	}
}
