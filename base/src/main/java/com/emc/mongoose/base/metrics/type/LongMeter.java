package com.emc.mongoose.base.metrics.type;

import java.io.Serializable;

/** @author veronika K. on 11.10.18 */
public interface LongMeter<S extends Serializable> {

	void update(final long v);

	S snapshot();
}
