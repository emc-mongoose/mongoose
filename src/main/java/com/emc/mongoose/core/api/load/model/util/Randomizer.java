package com.emc.mongoose.core.api.load.model.util;

import java.io.Serializable;

/**
 * Created by olga on 20.05.15.
 */
public interface Randomizer
extends Serializable {

	int nextInt(int n);
}
