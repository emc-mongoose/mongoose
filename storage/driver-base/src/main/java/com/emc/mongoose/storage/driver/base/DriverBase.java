package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.concurrent.DaemonBase;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.api.load.Monitor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 Created by kurila on 11.07.16.
 This mock just passes the submitted tasks to the load monitor em
 */
public abstract class DriverBase<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements Driver<I, O> {

	private final static Logger LOG = LogManager.getLogger();

	protected final AtomicReference<Monitor<I, O>> monitorRef = new AtomicReference<>(null);
	protected final int concurrencyLevel;

	protected DriverBase(final Config.LoadConfig loadConfig) {
		concurrencyLevel = loadConfig.getConcurrency();
	}

	@Override
	public final void registerMonitor(final Monitor<I, O> monitor)
	throws IllegalStateException {
		if(monitorRef.compareAndSet(null, monitor)) {
			monitor.registerDriver(this);
		} else {
			throw new IllegalStateException("Driver is already used by another monitor");
		}
	}

	@Override
	public void close()
	throws IOException {
		if(!isInterrupted()) {
			try {
				interrupt();
			} catch(final UserShootHisFootException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to interrupt");
			}
		}
	}
}
