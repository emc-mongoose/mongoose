package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.ui.log.LogUtil.BLUE;
import static com.emc.mongoose.ui.log.LogUtil.CYAN;
import static com.emc.mongoose.ui.log.LogUtil.RED;
import static com.emc.mongoose.ui.log.LogUtil.RESET;

import org.apache.logging.log4j.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class CommandStep
extends StepBase {

	public static final String TYPE = "Command";
	private static final ThreadFactory THREAD_FACTORY = new LogContextThreadFactory(
		"processMonitor", true
	);

	private String cmd = null;
	private volatile Process process = null;
	private Thread processMonitor = null;

	public CommandStep(final Config baseConfig) {
		this(baseConfig, null, null);
	}

	public CommandStep(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs,
		final Map<String, String> env
	) {
		super(baseConfig, stepConfigs, env);
	}

	private CommandStep(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs,
		final Map<String, String> env, final String cmd
	) {
		super(baseConfig, stepConfigs, env);
		this.cmd = cmd;
	}

	@Override
	protected final void doStartLocal(final Config actualConfig) {

		final boolean stdOutColorFlag = actualConfig.getOutputConfig().getColor();
		Loggers.MSG.info(
			"Invoking the shell command:\n{}{}{}",
			stdOutColorFlag ? CYAN : "", cmd, stdOutColorFlag ? RESET : ""
		);

		try {
			process = new ProcessBuilder("sh", "-c", cmd).start();
		} catch(final IOException e) {
			return;
		}

		processMonitor = THREAD_FACTORY.newThread(
			() -> {

				final BufferedReader stdOut = new BufferedReader(
					new InputStreamReader(process.getInputStream())
				);
				final BufferedReader stdErr = new BufferedReader(
					new InputStreamReader(process.getErrorStream())
				);

				try {

					String nextLine;
					int exitCode = -1;

					do {

						if(process.waitFor(1, TimeUnit.MILLISECONDS)) {
							exitCode = process.exitValue();
							Loggers.MSG.log(
								exitCode == 0 ? Level.DEBUG : Level.WARN,
								"Command \"{}\" exited with code {}", cmd, exitCode
							);
						}

						while(null != (nextLine = stdOut.readLine())) {
							Loggers.MSG.info(
								"{}{}{}", stdOutColorFlag ? BLUE : "", nextLine,
								stdOutColorFlag ? RESET : ""
							);
						}

						while(null != (nextLine = stdErr.readLine())) {
							Loggers.MSG.info(
								"{}{}{}", stdOutColorFlag ? RED : "", nextLine,
								stdOutColorFlag ? RESET : ""
							);
						}

					} while(exitCode == -1);

				} catch(final IOException e) {
					LogUtil.exception(
						Level.WARN, e, "Failed to read from the process stdout either stdin"
					);
				} catch(final InterruptedException e) {
					throw new CancellationException();
				} finally {
					try {
						stdOut.close();
						stdErr.close();
					} catch(final IOException ignored) {
					}
					// change the step state
					doFinish();
				}
			}
		);
		processMonitor.start();
	}

	@Override
	protected final void doStop() {
		super.doStop();
		if(!distributedFlag) {
			processMonitor.interrupt();
			process.destroyForcibly();
		}
	}

	@Override
	protected final void doClose() {
		super.doClose();
		if(!distributedFlag) {
			processMonitor = null;
			process = null;
		}
	}

	@Override
	protected final String getTypeName() {
		return TYPE;
	}

	@Override
	protected final CommandStep copyInstance(final Object config) {
		if(!(config instanceof String)) {
			throw new IllegalArgumentException(
				getTypeName() + " step type accepts only string parameter for the config method"
			);
		}
		return new CommandStep(baseConfig, stepConfigs, env, (String) config);
	}
}
