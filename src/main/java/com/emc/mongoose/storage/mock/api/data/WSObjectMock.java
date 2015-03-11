package com.emc.mongoose.storage.mock.api.data;

import com.emc.mongoose.core.api.data.WSObject;

import java.util.List;

/**
 * Created by olga on 23.01.15.
 */
public interface WSObjectMock
extends WSObject {

	public void updateRanges(final List<Long> ranges);
}
