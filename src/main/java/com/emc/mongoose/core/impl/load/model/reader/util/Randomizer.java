package com.emc.mongoose.core.impl.load.model.reader.util;

import java.io.Serializable;

/**
 * Created by olga on 20.05.15.
 */
public interface Randomizer
extends Serializable {

	int nextInt(int n);
}
