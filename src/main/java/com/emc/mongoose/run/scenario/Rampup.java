package com.emc.mongoose.run.scenario;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 11.06.15.
 */
public class Rampup
implements Runnable {
	//
	private final static Logger LOG;
	static {
		LogUtil.init();
		LOG = LogManager.getLogger();
	}
	//
	private final RunTimeConfig rtConfig;
	private final long timeOut;
	private final TimeUnit timeUnit;
	private final String loadTypeSeq[], sizeSeq[], connCountSeq[];
	//
	public Rampup(final RunTimeConfig rtConfig) {
		this.rtConfig = rtConfig;
		this.timeOut = rtConfig.getLoadLimitTimeValue();
		this.timeUnit = rtConfig.getLoadLimitTimeUnit();
		this.loadTypeSeq = rtConfig.getScenarioChainLoad();
		this.sizeSeq = rtConfig.getScenarioRampupSizes();
		this.connCountSeq = rtConfig.getScenarioRampupConnCounts();
		// adjust some defaults if necessary
		LOG.debug(Markers.MSG, "Setting the metric update period to zero for chain scenario");
		rtConfig.set(RunTimeConfig.KEY_LOAD_METRICS_PERIOD_SEC, 0);
	}
	//
	@Override
	public final void run() {
		Chain nextLoadSeq;
		for(int i = 0; i < sizeSeq.length; i++) {
			final String nextSizeStr = sizeSeq[i];
			for(final String nextConnCountStr : connCountSeq) {
				ThreadContext.put("currentSize", nextSizeStr + "-" + i);
				ThreadContext.put("currentConnCount", nextConnCountStr);
				final String nextStepName = nextConnCountStr + "x" + nextSizeStr;
				LOG.debug(Markers.MSG, "Build the next step load chain: \"{}\"", nextStepName);
				rtConfig.set(RunTimeConfig.KEY_DATA_SIZE, nextSizeStr);
				rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, nextSizeStr);
				rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, nextSizeStr);
				rtConfig.set(RunTimeConfig.KEY_LOAD_CONNS, nextConnCountStr);
				rtConfig.set(RunTimeConfig.KEY_APPEND_CONNS, nextConnCountStr);
				rtConfig.set(RunTimeConfig.KEY_CREATE_CONNS, nextConnCountStr);
				rtConfig.set(RunTimeConfig.KEY_DELETE_CONNS, nextConnCountStr);
				rtConfig.set(RunTimeConfig.KEY_READ_CONNS, nextConnCountStr);
				rtConfig.set(RunTimeConfig.KEY_UPDATE_CONNS, nextConnCountStr);
				nextLoadSeq = new Chain(rtConfig, timeOut, timeUnit, loadTypeSeq, false);
				LOG.info(Markers.PERF_SUM, "---- Step {} start ----", nextStepName);
				nextLoadSeq.run();
				if(nextLoadSeq.isInterrupted()) {
					return;
				}
			}
		}
	}
	//
	public static void main(final String... args) {
		RunTimeConfig.initContext();
		final RunTimeConfig runTimeConfig = RunTimeConfig.getContext();
		//
		LOG.info(Markers.MSG, runTimeConfig);
		//
		try {
			final Rampup rampupScenario = new Rampup(runTimeConfig);
			rampupScenario.run();
			LOG.info(Markers.MSG, "Scenario end");
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			LogUtil.exception(LOG, Level.ERROR, e, "Scenario failed");
		}
	}
}
