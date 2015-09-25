package com.emc.mongoose.run.scenario;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
//
import com.emc.mongoose.util.shared.WSLoadBuilderFactory;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import java.rmi.RemoteException;
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
	private final String loadTypeSeq[], sizeSeq[], connCountSeq[];
	//
	public Rampup(final RunTimeConfig rtConfig) {
		this.loadBuilder = WSLoadBuilderFactory.getInstance(rtConfig);
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
				final long nextSize = SizeUtil.toSize(nextSizeStr);
				final String nextStepName = nextConnCountStr + "x" + nextSizeStr;
				LOG.debug(Markers.MSG, "Build the next step load chain: \"{}\"", nextStepName);
				try {
					loadBuilder
						.setMinObjSize(nextSize)
						.setMaxObjSize(nextSize)
						.setConnPerNodeDefault(Integer.parseInt(nextConnCountStr));
					nextLoadSeq = new Chain(
						loadBuilder, timeOut, timeUnit, loadTypeSeq, false, true
					);
					LOG.info(Markers.PERF_SUM, "---- Step {} start ----", nextStepName);
					nextLoadSeq.run();
					if (nextLoadSeq.isInterrupted()) {
						return;
					}
				} catch(final RemoteException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to apply rampup params remotely");
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
