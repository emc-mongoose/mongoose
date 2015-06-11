package com.emc.mongoose.run.examples;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
//
import com.emc.mongoose.run.cli.HumanFriendly;
import com.emc.mongoose.run.examples.shared.LoadBuilderFactory;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 11.06.15.
 */
public class Rampup
implements Runnable {
	//
	static {
		try {
			LogUtil.init();
			RunTimeConfig.initContext();
		} catch(final Exception e) {
			e.printStackTrace(System.err);
		}
	}
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Map<String, Chain> loadStepsMap = new HashMap();
	//
	public Rampup(
		final LoadBuilder loadBuilder, final long timeOut, final TimeUnit timeUnit,
		final String loadTypeSeq[], final String sizeSeq[], final String threadCountSeq[]
	) {
		String nextSizeStr, nextStepName;
		long nextSize;
		for(int i = 0; i < sizeSeq.length; i++) {
			nextSizeStr = sizeSeq[i];
			for(final String nextThreadCountStr : threadCountSeq) {
				ThreadContext.put("currentSize", nextSizeStr + "-" + i);
				ThreadContext.put("currentThreadCount", nextThreadCountStr);
				nextSize = SizeUtil.toSize(nextSizeStr);
				nextStepName = nextThreadCountStr + "x" + nextSizeStr;
				LOG.debug(Markers.MSG, "Build the next step load chain: \"{}\"", nextStepName);
				try {
					loadBuilder
						.setMinObjSize(nextSize)
						.setMaxObjSize(nextSize)
						.setThreadsPerNodeDefault(Short.parseShort(nextThreadCountStr));
					loadStepsMap.put(
						nextStepName, new Chain(loadBuilder, timeOut, timeUnit, loadTypeSeq, false)
					);
				} catch(final RemoteException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to apply rampup params remotely");
				}
			}
		}
	}
	//
	@Override
	public final void run() {
		for(final String nextStepName : loadStepsMap.keySet()) {
			LOG.info(Markers.PERF_SUM, "---- Step {} start ----", nextStepName);
			loadStepsMap.get(nextStepName).run();
		}
	}
	//
	public static void main(final String... args) {
		try {
			//
			final RunTimeConfig runTimeConfig = RunTimeConfig.getContext();
			runTimeConfig.loadPropsFromJsonCfgFile(
				Paths.get(RunTimeConfig.DIR_ROOT, Constants.DIR_CONF)
					.resolve(RunTimeConfig.FNAME_CONF)
			);
			runTimeConfig.loadSysProps();
			// load the config from CLI arguments
			final Map<String, String> properties = HumanFriendly.parseCli(args);
			if(!properties.isEmpty()) {
				LOG.debug(Markers.MSG, "Overriding properties {}", properties);
				RunTimeConfig.getContext().overrideSystemProperties(properties);
			}
			//
			LOG.info(Markers.MSG, RunTimeConfig.getContext().toString());
			//
			final LoadBuilder loadBuilder = LoadBuilderFactory.getInstance();
			final long timeOut = runTimeConfig.getLoadLimitTimeValue();
			final TimeUnit timeUnit = runTimeConfig.getLoadLimitTimeUnit();
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
