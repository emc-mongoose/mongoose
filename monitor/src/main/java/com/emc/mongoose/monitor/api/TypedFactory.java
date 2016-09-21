package com.emc.mongoose.monitor.api;

/**
 * Created by on 9/21/2016.
 */
public interface TypedFactory<T, E extends Enum<E>> {

    T create(final E type);

}
