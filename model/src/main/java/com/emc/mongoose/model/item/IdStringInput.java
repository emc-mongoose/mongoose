package com.emc.mongoose.model.item;

import com.emc.mongoose.common.io.Input;

/**
 Created by andrey on 01.12.16.
 */
public interface IdStringInput
extends Input<String> {
	long getLastValue();
}
