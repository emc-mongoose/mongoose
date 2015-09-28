package com.emc.mongoose.core.impl.io.task;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.ObjectRequestConfig;
import com.emc.mongoose.core.api.io.task.DataObjectIOTask;
import com.emc.mongoose.core.api.data.DataObject;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 23.12.14.
 */
public class BasicObjectIOTask<T extends DataObject>
extends BasicIOTask<T>
implements DataObjectIOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicObjectIOTask(
		final T dataObject, final String nodeAddr, final ObjectRequestConfig<T> reqConf
	) {
		super(dataObject, nodeAddr, reqConf);
	}
}
