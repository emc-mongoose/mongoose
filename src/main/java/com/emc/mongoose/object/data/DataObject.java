package com.emc.mongoose.object.data;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.AppendableDataItem;
import com.emc.mongoose.base.data.UpdatableDataItem;
//
import java.io.IOException;
/**
 Created by kurila on 29.09.14.
 */
public interface DataObject
extends DataItem, AppendableDataItem, UpdatableDataItem {
	//
	long getId();
	//
	void setId(final long id)
	throws IOException;
	//
}
