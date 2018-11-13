package com.emc.mongoose;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.load.step.service.LoadStepManagerServiceImpl;
import com.emc.mongoose.load.step.service.file.FileManagerServiceImpl;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.metrics.MetricsManager;
import com.emc.mongoose.svc.Service;
import com.github.akurilov.confuse.Config;
import org.apache.logging.log4j.Level;

import java.time.LocalDateTime;
import java.util.List;

/**
 @author veronika K. on 08.11.18 */
public class NodeImpl
	implements Node {

	private final LocalDateTime startTime;
	private final String status = "RUN";

	public NodeImpl() {
		this.startTime = LocalDateTime.now();
	}

	@Override
	public void run(final Config config, final List<Extension> extensions, final MetricsManager metricsMgr)
	throws InterruptRunException, InterruptedException {
		final int listenPort = config.intVal("load-step-node-port");
		//TODO: put data to metaData map
		try(
			final Service fileMgrSvc = new FileManagerServiceImpl(listenPort);
			final Service scenarioStepSvc = new LoadStepManagerServiceImpl(listenPort, extensions, metricsMgr)
		) {
			fileMgrSvc.start();
			scenarioStepSvc.start();
			scenarioStepSvc.await();
		} catch(final InterruptedException | InterruptRunException e) {
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
	public String status() {
		return status;
	}
}
