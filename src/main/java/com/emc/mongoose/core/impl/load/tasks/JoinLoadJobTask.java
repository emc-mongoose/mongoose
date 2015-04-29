package com.emc.mongoose.core.impl.load.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
/**
 Created by kurila on 23.12.14.
 */
public final class JoinLoadJobTask
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadExecutor loadJob;
	private final long timeOutMilliSec;
	//
	public JoinLoadJobTask(final LoadExecutor loadJob, final long timeOutMilliSec) {
		this.loadJob = loadJob;
		this.timeOutMilliSec = timeOutMilliSec;
	}
	//
	@Override
	public final void run() {
		try {
			LOG.debug(
				LogUtil.MSG, "Wait for the remote load service \"{}\" to complete at {}[ms]",
				loadJob.getName(), timeOutMilliSec
			);
			loadJob.join(timeOutMilliSec);
		} catch(final NoSuchObjectException e) {
			LogUtil.failure(LOG, Level.DEBUG, e, "Remote join failed, no such service");
		} catch(final RemoteException e) {
			LogUtil.failure(LOG, Level.WARN, e, "Remote join task failure");
		} catch (final InterruptedException e) {
			LOG.debug(LogUtil.MSG, "Remote join task for \"{}\" was interrupted", loadJob);
		} finally {
			LOG.debug(LogUtil.MSG, "Remote join task for \"{}\" was completed", loadJob);
		}
	}
}
