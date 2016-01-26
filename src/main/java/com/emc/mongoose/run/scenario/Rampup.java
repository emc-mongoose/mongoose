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
	private final AppConfig appConfig;
	private final long timeOut, unChangedCountLimit;
	private final TimeUnit timeUnit;
	private final String loadTypeSeq[], sizeSeq[], connCountSeq[];
	//
	public Rampup(final AppConfig appConfig) {
		this.appConfig = appConfig;
		this.timeOut = appConfig.getLoadLimitTimeValue();
		this.unChangedCountLimit = appConfig.getLoadLimitCount();
		this.timeUnit = appConfig.getLoadLimitTimeUnit();
		this.loadTypeSeq = appConfig.getScenarioChainLoad();
		this.sizeSeq = appConfig.getScenarioRampupSizes();
		this.connCountSeq = appConfig.getScenarioRampupConnCounts();
		// adjust some defaults if necessary
		LOG.debug(Markers.MSG, "Setting the metric update period to zero for chain scenario");
		appConfig.set(RunTimeConfig.KEY_LOAD_METRICS_PERIOD_SEC, 0);
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
				// each finished load job affects global count limitation, should be reset
				appConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, unChangedCountLimit);
				appConfig.set(RunTimeConfig.KEY_DATA_SIZE, nextSizeStr);
				appConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, nextSizeStr);
				appConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, nextSizeStr);
				appConfig.set(RunTimeConfig.KEY_LOAD_CONNS, nextConnCountStr);
				appConfig.set(RunTimeConfig.KEY_APPEND_CONNS, nextConnCountStr);
				appConfig.set(RunTimeConfig.KEY_CREATE_CONNS, nextConnCountStr);
				appConfig.set(RunTimeConfig.KEY_DELETE_CONNS, nextConnCountStr);
				appConfig.set(RunTimeConfig.KEY_READ_CONNS, nextConnCountStr);
				appConfig.set(RunTimeConfig.KEY_UPDATE_CONNS, nextConnCountStr);
				nextLoadSeq = new Chain(appConfig, timeOut, timeUnit, loadTypeSeq, false);
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
		final AppConfig appConfig = BasicConfig.CONTEXT_CONFIG.get();
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
