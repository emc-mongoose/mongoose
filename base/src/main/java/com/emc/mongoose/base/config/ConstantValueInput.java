package com.emc.mongoose.base.config;

import com.github.akurilov.commons.io.Input;

/**
* A marker interface notifying that the given input retuns the same value always.
*
* @param <T>
*/
public interface ConstantValueInput<T> extends Input<T> {}
