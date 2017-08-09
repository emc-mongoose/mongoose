package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.api.model.concurrent.NamingThreadFactory;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.ui.log.LogUtil.BLUE;
import static com.emc.mongoose.ui.log.LogUtil.CYAN;
import static com.emc.mongoose.ui.log.LogUtil.RED;
import static com.emc.mongoose.ui.log.LogUtil.RESET;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadFactory;

/**
 Created by andrey on 07.04.16.
 */
public final class CommandStep
extends StepBase {
	//
	private static final ThreadFactory TF_STD_IN = new NamingThreadFactory("stdInReader", true);
	private static final ThreadFactory TF_STD_ERR = new NamingThreadFactory("stdErrReader", true);
	private static final String KEY_NODE_BLOCKING = "blocking";
	//
	private final String cmdLine;
	private final boolean blockingFlag;
	private final boolean stdOutColorFlag;
	//
	public CommandStep(final Config appConfig, final Map<String, Object> subTree)
	throws IllegalArgumentException {
		super(appConfig);
		cmdLine = (String) subTree.get(KEY_NODE_VALUE);
		if(subTree.containsKey(KEY_NODE_BLOCKING)) {
			blockingFlag = (boolean) subTree.get(KEY_NODE_BLOCKING);
		} else {
			blockingFlag = true;
		}
		stdOutColorFlag = appConfig.getOutputConfig().getColor();
	}
	//
	@Override
	protected final void invoke()
	throws CancellationException {
		try {
			Loggers.MSG.info(
				"Invoking the shell command:\n{}{}{}",
				stdOutColorFlag ? CYAN : "", cmdLine, stdOutColorFlag ? RESET : ""
			);
			final Process process = new ProcessBuilder("bash", "-c", cmdLine).start();
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
			if(blockingFlag) {
				try {
					final int exitCode = process.waitFor();
					if(exitCode == 0) {
						Loggers.MSG.info("Shell command \"{}\" finished", cmdLine);
					} else {
						Loggers.ERR.warn(
							"Shell command \"{}\" finished with exit code {}", cmdLine, exitCode
						);
					}
				} finally {
					processStdInReader.interrupt();
					processStdErrReader.interrupt();
					process.destroy();
				}
			}
		} catch(final InterruptedException e) {
			throw new CancellationException();
		} catch(final Exception e) {
			LogUtil.exception(Level.WARN, e, "Shell command \"{}\" failed", cmdLine);
		}
	}

	@Override
	public final void close() {
	}
}
