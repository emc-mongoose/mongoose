package com.emc.mongoose.threading;
//
import com.emc.mongoose.logging.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
/**
 Created by kurila on 14.08.14.
 */
public class RejectedTaskHandler
implements RejectedExecutionHandler {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String ERR_MSG_PATTERN = "The task \"{}\" rejected {} times";
	public final static int WAIT_QUANT_MILLISECS = 100;
	//
	@SuppressWarnings("unchecked")
	@Override
	public final void rejectedExecution(final Runnable task, final ThreadPoolExecutor executor) {
		//
		if(LOG.isTraceEnabled()) {
			LOG.trace(Markers.MSG, "Handle rejected task \"{}\"", task);
		}
		boolean passed = false;
		int rejectCount = 1;
		//
		while(!passed && rejectCount<100) { // to avoid stack overflow
			//
			if(rejectCount==4 && LOG.isTraceEnabled()) {
				LOG.trace(Markers.ERR, ERR_MSG_PATTERN, task, rejectCount);
			} else if(rejectCount==16 && LOG.isDebugEnabled()) {
				LOG.debug(Markers.ERR, ERR_MSG_PATTERN, task, rejectCount);
			} else if(rejectCount==64) {
				LOG.info(Markers.ERR, ERR_MSG_PATTERN, task, rejectCount);
			} else if(rejectCount==256) {
				LOG.warn(Markers.ERR, ERR_MSG_PATTERN, task, rejectCount);
				break;
			}
			//
			try {
				Thread.sleep(rejectCount*WAIT_QUANT_MILLISECS);
				executor.submit(task);
				passed = true;
			} catch(final InterruptedException e) {
				break;
			} catch(final RejectedExecutionException e) {
				rejectCount++;
			}
		}
		//
		if(passed && LOG.isTraceEnabled()) {
			LOG.trace(
				Markers.MSG, "Task \"{}\" was submitted after {} rejections",
				task, rejectCount
			);
		} else if(!passed) {
			LOG.debug(
				Markers.ERR, "Task \"{}\" was dropped after {} rejections",
				task, rejectCount
			);
		}
	}
	//
}
