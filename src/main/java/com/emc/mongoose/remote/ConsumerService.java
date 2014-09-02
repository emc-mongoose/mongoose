package com.emc.mongoose.remote;
//
import com.emc.mongoose.Consumer;
import com.emc.mongoose.data.UniformData;
/**
 Created by kurila on 30.05.14.
 */
public interface ConsumerService<T extends UniformData>
extends Consumer<T>, Service {
}
