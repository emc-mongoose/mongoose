package com.emc.mongoose.model.impl.data;

import com.emc.mongoose.common.exception.IoFireball;

/**
 Created by andrey on 26.06.15.
 */
public abstract class DataVerificationException
extends IoFireball {
	public long offset;
}
