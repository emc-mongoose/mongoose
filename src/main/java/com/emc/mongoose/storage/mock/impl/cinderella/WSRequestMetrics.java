package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.management.MBeanServer;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 13.05.15.
 */
public final class WSRequestMetrics
extends Thread
implements Closeable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static String METRIC_COUNT = "count", ALL_METHODS = "all";
	//
	private final MetricRegistry metricRegistry = new MetricRegistry();
	private final MBeanServer mBeanServer = ServiceUtils.getMBeanServer(
		RunTimeConfig.getContext().getRemotePortExport()
	);
	private final JmxReporter metricsReporter = JmxReporter.forRegistry(metricRegistry)
		.convertDurationsTo(TimeUnit.SECONDS)
		.convertRatesTo(TimeUnit.SECONDS)
		.registerWith(mBeanServer)
		.build();
	private final Counter
		countSuccCreate = metricRegistry.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_SUCC
			)
		),
		countSuccRead = metricRegistry.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_SUCC
			)
		),
		countSuccDelete = metricRegistry.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.DELETE), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_SUCC
			)
		),
		countFailCreate = metricRegistry.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_FAIL
			)
		),
		countFailRead = metricRegistry.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_FAIL
			)
		);
	private final Meter
		bwAll = metricRegistry.meter(
			MetricRegistry.name(Cinderella.class, ALL_METHODS, LoadExecutor.METRIC_NAME_BW)
		),
		bwCreate = metricRegistry.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), LoadExecutor.METRIC_NAME_BW
			)
		),
		bwRead = metricRegistry.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), LoadExecutor.METRIC_NAME_BW
			)
		),
		tpAll = metricRegistry.meter(
			MetricRegistry.name(
				Cinderella.class, ALL_METHODS, LoadExecutor.METRIC_NAME_TP
			)
		),
		tpCreate = metricRegistry.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), LoadExecutor.METRIC_NAME_TP
			)
		),
		tpRead = metricRegistry.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), LoadExecutor.METRIC_NAME_TP
			)
		);
	//
	private final long updateMilliPeriod;
	//
	public WSRequestMetrics(final RunTimeConfig runTimeConfig) {
		super(WSRequestMetrics.class.getSimpleName());
		setDaemon(true);
		updateMilliPeriod = TimeUnit.SECONDS.toMillis(runTimeConfig.getLoadMetricsPeriodSec());
		metricsReporter.start();
		start();
	}
	//
	private final static String
		MSG_FMT_METRICS = "count(succ=(%d/%d/%d); fail=(%d/%d)); " +
		"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)";
	//
	private void printMetrics() {
		LOG.info(
			LogUtil.PERF_AVG,
			String.format(
				LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
				//
				countSuccCreate.getCount(), countSuccRead.getCount(), countSuccDelete.getCount(),
				countFailCreate.getCount(), countFailRead.getCount(),
				//
				tpAll.getMeanRate(),
				tpAll.getOneMinuteRate(),
				tpAll.getFiveMinuteRate(),
				tpAll.getFifteenMinuteRate(),
				//
				bwAll.getMeanRate() / LoadExecutor.MIB,
				bwAll.getOneMinuteRate() / LoadExecutor.MIB,
				bwAll.getFiveMinuteRate() / LoadExecutor.MIB,
				bwAll.getFifteenMinuteRate() / LoadExecutor.MIB
			)
		);
	}
	//
	public final void run() {
		try {
			while(updateMilliPeriod > 0) {
				printMetrics();
				Thread.sleep(updateMilliPeriod);
			}
		} catch(final InterruptedException ignored) {
		} finally {
			close();
		}
	}
	//
	public final void close() {
		if(isAlive()) {
			interrupt();
		}
		metricsReporter.close();
	}
	//
	public final double getMeanRate() {
		return tpAll.getMeanRate();
	}
	//
	public final void markCreate(final long size) {
		if(size < 0) {
			countFailCreate.inc();
		} else {
			countSuccCreate.inc();
			tpCreate.mark();
			bwCreate.mark(size);
			tpAll.mark();
			bwAll.mark(size);
		}
	}
	//
	public final void markRead(final long size) {
		if(size < 0) {
			countFailRead.inc();
		} else {
			countSuccRead.inc();
			tpRead.mark();
			bwRead.mark(size);
			tpAll.mark();
			bwAll.mark(size);
		}
	}
	//
	public final void markDelete() {
		countSuccDelete.inc();
		tpAll.mark();
	}
}
