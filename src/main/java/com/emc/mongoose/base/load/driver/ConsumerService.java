package com.emc.mongoose.base.load.driver;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.util.remote.Service;
/**
 Created by kurila on 30.05.14.
 */
public interface ConsumerService<T extends DataItem>
extends Consumer<T>, Service {
}
