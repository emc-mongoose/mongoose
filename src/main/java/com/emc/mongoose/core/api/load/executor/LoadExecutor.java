package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.common.concurrent.LifeCycle;
//
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.load.generator.LoadState;
import com.emc.mongoose.core.api.load.generator.LoadGenerator;
import com.emc.mongoose.core.api.load.metrics.IoStats;
//
import org.apache.logging.log4j.Marker;
//
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 28.04.14.
 A mechanism of data items load execution.
 May be a consumer and producer both also.
 Supports method "join" for waiting the load execution to be done.
 */
public interface LoadExecutor<T extends Item> {
	//
	int
		DEFAULT_INTERNAL_BATCH_SIZE = 0x80,
		DEFAULT_RESULTS_QUEUE_SIZE = 0x10000;
	//
	int submit(final List<T> items, final LoadType loadType)
	throws RemoteException;
}
