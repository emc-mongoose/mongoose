package com.emc.mongoose.object.load.driver;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.driver.LoadBuilderService;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.load.ObjectLoadBuilder;
/**
 Created by kurila on 29.09.14.
 */
public interface ObjectLoadBuilderService<T extends DataObject, U extends LoadExecutor<T>>
extends LoadBuilderService<T, U>, ObjectLoadBuilder<T, U> {
}
