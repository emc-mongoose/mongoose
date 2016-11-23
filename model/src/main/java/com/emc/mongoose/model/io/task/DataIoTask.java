package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.io.task.result.DataIoResult;
import com.emc.mongoose.model.item.DataItem;

import java.util.List;

/**
 Created by kurila on 11.07.16.
 */
public interface DataIoTask<D extends DataItem, R extends DataIoResult>
extends IoTask<D, R> {

	@Override D getItem();

	boolean isMultiPart();

	List<DataItem> getParts();

	long getCountBytesDone();

	void setCountBytesDone(long n);

	long getRespDataTimeStart();

	void startDataResponse();
}
