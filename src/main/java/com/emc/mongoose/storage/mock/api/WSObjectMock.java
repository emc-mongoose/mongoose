package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import java.util.List;
/**
 Created by kurila on 03.07.15.
 */
public interface WSObjectMock
extends WSObject {
	void updateRanges(final List<Long> ranges);
}
