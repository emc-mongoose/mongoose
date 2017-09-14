package com.emc.mongoose.run.scenario.jsr223;

import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.ui.log.LogUtil.BLUE;
import static com.emc.mongoose.ui.log.LogUtil.CYAN;
import static com.emc.mongoose.ui.log.LogUtil.RED;
import static com.emc.mongoose.ui.log.LogUtil.RESET;

import org.apache.logging.log4j.Level;

import javax.script.Bindings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadFactory;

/**
 The specific scenario step executing a shell command.
 */
public final class CommandStep
extends StepBase
implements ValueStep {

	private static final ThreadFactory TF_STD_IN = new LogContextThreadFactory("stdInReader", true);
	private static final ThreadFactory TF_STD_ERR = new LogContextThreadFactory("stdErrReader", true);

	private final String cmd;

	public CommandStep(final Config config) {
		super(config);
		this.cmd = null;
	}

	private CommandStep(final Config config, final String cmd) {
		super(config);
		this.cmd = cmd;
	}

	@Override
	public final CommandStep config(final Bindings stepConfig) {
		final Config configCopy = new Config(config);
		configCopy.apply(stepConfig, "command-" + LogUtil.getDateTimeStamp() + "-" + hashCode());
		return new CommandStep(configCopy, cmd);
	}

	@Override
	public final CommandStep value(final String value) {
		return new CommandStep(new Config(config), value);
	}

	@Override
	public final void run() {
		final boolean stdOutColorFlag = config.getOutputConfig().getColor();
		try {
			Loggers.MSG.info(
				"Invoking the shell command:\n{}{}{}",
				stdOutColorFlag ? CYAN : "", cmd, stdOutColorFlag ? RESET : ""
			);
			final Process process = new ProcessBuilder("bash", "-c", cmd).start();
			final Thread processStdInReader = TF_STD_IN.newThread(
				() -> {
					try(
						final BufferedReader bufferedReader = new BufferedReader(
							new InputStreamReader(process.getInputStream())
						)
					) {
						String nextLine;
						while(null != (nextLine = bufferedReader.readLine())) {
							Loggers.MSG.info(
								"{}{}{}", stdOutColorFlag ? BLUE : "", nextLine,
								stdOutColorFlag ? RESET : ""
							);
						}
					} catch(final IOException e) {
						LogUtil.exception(
							Level.DEBUG, e, "Failed to read the process stdin"
						);
					}
				}
			);
			final Thread processStdErrReader = TF_STD_ERR.newThread(
				() -> {
					try(
						final BufferedReader bufferedReader = new BufferedReader(
							new InputStreamReader(process.getErrorStream())
						)
					) {
						String nextLine;
						while(null != (nextLine = bufferedReader.readLine())) {
							Loggers.MSG.info(
								"{}{}{}", stdOutColorFlag ? RED : "", nextLine,
								stdOutColorFlag ? RESET : ""
							);
						}
					} catch(final IOException e) {
						LogUtil.exception(
							Level.DEBUG, e, "Failed to read the process error input"
						);
					}
				}
			);
			processStdInReader.start();
			processStdErrReader.start();
			try {
				final int exitCode = process.waitFor();
				if(exitCode == 0) {
					Loggers.MSG.info("Shell command \"{}\" finished", cmd);
				} else {
					Loggers.ERR.warn(
						"Shell command \"{}\" finished with exit code {}", cmd, exitCode
					);
				}
			} finally {
				processStdInReader.interrupt();
				processStdErrReader.interrupt();
				process.destroy();
			}
		} catch(final InterruptedException e) {
			throw new CancellationException();
		} catch(final Exception e) {
			LogUtil.exception(Level.WARN, e, "Shell command \"{}\" failed", cmd);
		}
	}
}
