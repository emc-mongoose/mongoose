package com.emc.mongoose.storage.mock.impl.base;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.emc.mongoose.model.api.metrics.IoStats;
import com.emc.mongoose.model.impl.metrics.CustomMeter;
import com.emc.mongoose.model.impl.metrics.ResumableUserTimeClock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageIoStats;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class BasicStorageIoStats
extends Thread
implements StorageIoStats {

	private final static Logger LOG = LogManager.getLogger();

	private final Counter countFailWrite, countFailRead, countFailDelete, countContainers;
	private final CustomMeter tpWrite, tpRead, tpDelete, bwWrite, bwRead;
	private final long updatePeriodMilliSec;
	private final StorageMock storage;

	public BasicStorageIoStats(final StorageMock storage, final int metricsPeriodSec) {
		super(BasicStorageIoStats.class.getSimpleName());
		setDaemon(true);
		this.updatePeriodMilliSec = metricsPeriodSec;
		this.storage = storage;
		final Clock clock = new ResumableUserTimeClock();
		countFailWrite = new Counter();
		countFailRead = new Counter();
		countFailDelete = new Counter();
		countContainers = new Counter();
		tpWrite = new CustomMeter(clock, metricsPeriodSec);
		tpRead = new CustomMeter(clock, metricsPeriodSec);
		tpDelete = new CustomMeter(clock, metricsPeriodSec);
		bwWrite = new CustomMeter(clock, metricsPeriodSec);
		bwRead = new CustomMeter(clock, metricsPeriodSec);
	}

	private final static String
		MSG_FMT_METRICS = "Capacity used: %d (%.1f%%), containers count: %d\n" +
		"\tOperation |Count       |Failed      |TP[op/s]avg |TP[op/s]last|BW[MB/s]avg |BW[MB/s]last\n" +
		"\t----------|------------|------------|------------|------------|------------|------------\n" +
		"\tWrite     |%12d|%12d|%12.3f|%12.3f|%12.3f|%12.3f\n" +
		"\tRead      |%12d|%12d|%12.3f|%12.3f|%12.3f|%12.3f\n" +
		"\tDelete    |%12d|%12d|%12.3f|%12.3f|            |";

	@Override
	public synchronized void start() {
		LOG.debug(Markers.MSG, "Start");
		super.start();
	}

	@Override
	public void run() {
		LOG.debug(Markers.MSG, "Running");
		try {
			while(updatePeriodMilliSec > 0 && !isInterrupted()) {
				LOG.info(Markers.MSG, toString());
				Thread.sleep(updatePeriodMilliSec);
				close();
			}
		} catch(final InterruptedException ignored) {
			LOG.debug(Markers.MSG, "Interrupted");
		} catch(final IOException e) {
			LOG.debug(Markers.MSG, "Failed to close");
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failure");
		}
	}

	@Override
	public void markWrite(final boolean success, final long size) {
		if(success) {
			tpWrite.mark();
			bwWrite.mark(size);
		} else {
			countFailWrite.inc();
		}
	}

	@Override
	public void markRead(final boolean success, final long size) {
		if(success) {
			tpRead.mark();
			bwRead.mark(size);
		} else {
			countFailRead.inc();
		}
	}

	@Override
	public void markDelete(final boolean success) {
		if(success) {
			tpDelete.mark();
		} else {
			countFailDelete.inc();
		}
	}

	@Override
	public void containerCreate() {
		countContainers.inc();
	}

	@Override
	public void containerDelete() {
		countContainers.dec();
	}

	@Override
	public double getWriteRate() {
		return tpWrite.getLastRate();
	}

	@Override
	public double getWriteRateBytes() {
		return bwWrite.getLastRate();
	}

	@Override
	public double getReadRate() {
		return tpRead.getLastRate();
	}

	@Override
	public double getReadRateBytes() {
		return bwRead.getLastRate();
	}

	@Override
	public double getDeleteRate() {
		return tpDelete.getLastRate();
	}

	@Override
	public void close()
	throws IOException {
		if(!isInterrupted()) {
			interrupt();
		}
	}

	@Override
	public final String toString() {
		final long countTotal = storage.getSize();
		return String.format(
			LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
			//
			countTotal, 100.0 * countTotal / storage.getCapacity(), countContainers.getCount(),
			//
			tpWrite.getCount(), countFailWrite.getCount(),
			tpWrite.getMeanRate(), tpWrite.getLastRate(),
			bwWrite.getMeanRate() / IoStats.MIB, bwWrite.getLastRate() / IoStats.MIB,
			//
			tpRead.getCount(), countFailRead.getCount(),
			tpRead.getMeanRate(), tpRead.getLastRate(),
			bwRead.getMeanRate() / IoStats.MIB, bwRead.getLastRate() / IoStats.MIB,
			//
			tpDelete.getCount(), countFailDelete.getCount(),
			tpDelete.getMeanRate(), tpDelete.getLastRate()
		);
	}
}
