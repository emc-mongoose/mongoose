package com.emc.mongoose;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.load.step.LoadStepManagerService;
import com.emc.mongoose.load.step.service.LoadStepManagerServiceImpl;
import com.emc.mongoose.load.step.service.file.FileManagerServiceImpl;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.metrics.MetricsManager;
import com.emc.mongoose.svc.Service;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.confuse.Config;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 @author veronika K. on 08.11.18 */
public class NodeImpl
	extends AsyncRunnableBase
	implements Node {

	private final LocalDateTime startTime;
	private final MetricsManager metricsManager;
	private final LoadStepManagerService stepSvc;
	private final Service fileMgrSvc;
	private final int listenPort;

	public NodeImpl(final Config config, final List<Extension> extensions, final MetricsManager metricsMgr) {
		this.metricsManager = metricsMgr;
		this.startTime = LocalDateTime.now();
		this.listenPort = config.intVal("load-step-node-port");
		this.stepSvc = new LoadStepManagerServiceImpl(listenPort, extensions, metricsManager);
		this.fileMgrSvc = new FileManagerServiceImpl(listenPort);
	}

	@Override
	public void run()
	throws InterruptedException {
		super.start(); //??????
		try {
			fileMgrSvc.start();
			stepSvc.start();
			stepSvc.await();
		} catch(final InterruptedException e) {
			throw e;
		} catch(final Throwable cause) {
			LogUtil.trace(Loggers.ERR, Level.FATAL, cause, "Run node failure");
		}
	}

	@Override
	public LocalDateTime startTime() {
		return startTime;
	}

	@Override
	protected void doClose()
	throws IOException {
		fileMgrSvc.close();
		stepSvc.close();
	}
}
