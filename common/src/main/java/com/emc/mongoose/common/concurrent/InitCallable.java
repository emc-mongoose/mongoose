package com.emc.mongoose.common.concurrent;

import java.util.concurrent.Callable;

/**
 Created by kurila on 11.07.16.
 */
public interface InitCallable<V>
extends Initializable, Callable<V> {
}
