package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.core.impl.data.BasicWSObject;
//
import java.io.IOException;
/**
 Created by kurila on 15.12.14.
 */
public final class BasicWSDataGenerator<T extends BasicWSObject>
extends DataGeneratorBase<T> {
	//
	public BasicWSDataGenerator(
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
