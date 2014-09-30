package com.emc.mongoose.object.load.driver;
//
import com.emc.mongoose.base.load.driver.LoadService;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.load.ObjectLoadExecutor;
/**
 Created by kurila on 29.09.14.
 */
public interface ObjectLoadService<T extends DataObject>
extends ObjectLoadExecutor<T>, LoadService<T> {
}
