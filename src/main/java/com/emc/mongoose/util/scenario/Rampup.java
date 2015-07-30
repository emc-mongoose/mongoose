package com.emc.mongoose.util.scenario;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
//
import com.emc.mongoose.run.cli.HumanFriendly;
import com.emc.mongoose.util.scenario.shared.WSLoadBuilderFactory;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import java.rmi.RemoteException;
import java.util.Map;
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
	private final LoadBuilder loadBuilder;
	private final long timeOut;
	private final TimeUnit timeUnit;
	private final String loadTypeSeq[], sizeSeq[], threadCountSeq[];
	//
	public Rampup(
		final LoadBuilder loadBuilder, final long timeOut, final TimeUnit timeUnit,
		final String loadTypeSeq[], final String sizeSeq[], final String threadCountSeq[]
	) {
		this.loadBuilder = loadBuilder;
		this.timeOut = timeOut;
		this.timeUnit = timeUnit;
		this.loadTypeSeq = loadTypeSeq;
		this.sizeSeq = sizeSeq;
		this.threadCountSeq = threadCountSeq;
	}
	//
	@Override
	public final void run() {
		Chain nextLoadSeq;
		for(int i = 0; i < sizeSeq.length; i++) {
			final String nextSizeStr = sizeSeq[i];
			for(final String nextThreadCountStr : threadCountSeq) {
				ThreadContext.put("currentSize", nextSizeStr + "-" + i);
				ThreadContext.put("currentThreadCount", nextThreadCountStr);
				final long nextSize = SizeUtil.toSize(nextSizeStr);
				final String nextStepName = nextThreadCountStr + "x" + nextSizeStr;
				LOG.debug(Markers.MSG, "Build the next step load chain: \"{}\"", nextStepName);
				try {
					loadBuilder
						.setMinObjSize(nextSize)
						.setMaxObjSize(nextSize)
						.setThreadsPerNodeDefault(Integer.parseInt(nextThreadCountStr));
					nextLoadSeq = new Chain(loadBuilder, timeOut, timeUnit, loadTypeSeq, false);
					LOG.info(Markers.PERF_SUM, "---- Step {} start ----", nextStepName);
					nextLoadSeq.run();
				} catch(final RemoteException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to apply rampup params remotely");
				}
			}
		}
	}
	//
	public static void main(final String... args) {
		try {
			//
			RunTimeConfig.initContext();
			final RunTimeConfig runTimeConfig = RunTimeConfig.getContext();
			// load the config from CLI arguments
			final Map<String, String> properties = HumanFriendly.parseCli(args);
			if(!properties.isEmpty()) {
				LOG.debug(Markers.MSG, "Overriding properties {}", properties);
				RunTimeConfig.getContext().overrideSystemProperties(properties);
			}
			//
			LOG.info(Markers.MSG, RunTimeConfig.getContext().toString());
			//
			final LoadBuilder loadBuilder = WSLoadBuilderFactory.getInstance(runTimeConfig);
			final long timeOut = runTimeConfig.getLoadLimitTimeValue();
			final TimeUnit timeUnit = runTimeConfig.getLoadLimitTimeUnit();
			// adjust some defaults if necessary
			LOG.debug(Markers.MSG, "Setting the metric update period to zero for chain scenario");
			runTimeConfig.set(RunTimeConfig.KEY_LOAD_METRICS_PERIOD_SEC, 0);
			//
			final String[] loadTypeSeq = runTimeConfig.getScenarioChainLoad();
			final String[] sizeSeq = runTimeConfig.getScenarioRampupSizes();
			final String[] threadCountSeq = runTimeConfig.getScenarioRampupThreadCounts();
			final Rampup rampupScenario = new Rampup(
				loadBuilder, timeOut, timeUnit, loadTypeSeq, sizeSeq, threadCountSeq
			);
			//
			rampupScenario.run();
		} catch(final Exception e) {
			e.printStackTrace(System.err);
			LogUtil.exception(LOG, Level.ERROR, e, "Scenario failed");
		}
	}
}
