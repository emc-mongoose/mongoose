package com.emc.mongoose.common.pattern;

/**
 * Created by on 9/21/2016.
 */
public interface EnumFactory<T, E extends Enum<E>> {

    T create(final E type);

}
