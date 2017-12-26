package com.emc.mongoose.scenario.step.local;

import java.util.List;

public interface Sliceable<T> {

	List<T> slice();
}
