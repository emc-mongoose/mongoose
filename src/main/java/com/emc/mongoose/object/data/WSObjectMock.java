package com.emc.mongoose.object.data;

import com.emc.mongoose.object.data.WSObject;

import java.util.List;

/**
 * Created by olga on 23.01.15.
 */
public interface WSObjectMock
extends WSObject{

	public void updateRanges(final List<Long> ranges);
}
