package com.emc.mongoose;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.load.step.LoadStepManagerService;
import com.emc.mongoose.load.step.service.LoadStepManagerServiceImpl;
import com.emc.mongoose.load.step.service.file.FileManagerServiceImpl;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.metrics.MetricsManager;
import com.emc.mongoose.svc.Service;
import com.github.akurilov.confuse.Config;
import org.apache.logging.log4j.Level;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static com.github.akurilov.commons.concurrent.AsyncRunnable.State;

/**
 @author veronika K. on 08.11.18 */
public class NodeImpl
	implements Node {

	private final LocalDateTime startTime;
	private Supplier<State> stateSupplier = () -> State.STOPPED;
	private Config config;
	private List<Extension> extensions;
	private MetricsManager metricsManager;

	public NodeImpl(final Config config, final List<Extension> extensions, final MetricsManager metricsMgr) {
		this.config = config;
		this.extensions = extensions;
		this.metricsManager = metricsMgr;
		this.startTime = LocalDateTime.now();

	}

	@Override
	public void run()
	throws InterruptRunException, InterruptedException {
		final int listenPort = config.intVal("load-step-node-port");
		try(
			final Service fileMgrSvc = new FileManagerServiceImpl(listenPort);
			final LoadStepManagerService scenarioStepSvc = new LoadStepManagerServiceImpl(listenPort, extensions,
				metricsManager
			)
		) {
			stateSupplier = () -> {
				try {
					return scenarioStepSvc.state();
				} catch(RemoteException e) {
					e.printStackTrace();
				}
				return stateSupplier.get();
			};
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
	public State state() {
		return stateSupplier.get();
	}
}
