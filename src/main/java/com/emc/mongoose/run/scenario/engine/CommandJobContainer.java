package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ThreadFactory;

import static com.emc.mongoose.common.log.LogUtil.BLUE;
import static com.emc.mongoose.common.log.LogUtil.CYAN;
import static com.emc.mongoose.common.log.LogUtil.GREEN;
import static com.emc.mongoose.common.log.LogUtil.RED;

/**
 Created by andrey on 07.04.16.
 */
public final class CommandJobContainer
implements JobContainer {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static ThreadFactory TF_STD_IN = new NamingThreadFactory("stdInReader", true);
	private final static ThreadFactory TF_STD_ERR = new NamingThreadFactory("stdErrReader", true);
	//
	private final String cmdLine;
	private final boolean blockingFlag;
	private final boolean consoleColorFlag;
	//
	public CommandJobContainer(final String cmdLine, final boolean blockingFlag)
	throws IllegalArgumentException {
		this.cmdLine = cmdLine;
		this.blockingFlag = blockingFlag;
		consoleColorFlag = LogUtil.isConsoleColoringEnabled();
	}
	//
	@Override
	public final AppConfig getConfig() {
		return null;
	}
	//
	@Override
	public final boolean append(final JobContainer subJob) {
		throw new IllegalStateException("Appending sub jobs to a sleep step is not allowed");
	}
	//
	@Override
	public final void run() {
		try {
			LOG.info(
				Markers.MSG, "Invoking the shell command:\n{}{}{}",
				consoleColorFlag ? CYAN : "", cmdLine, consoleColorFlag ? GREEN : ""
			);
			final Process process = new ProcessBuilder("bash", "-c", cmdLine).start();
			final Thread processStdInReader = TF_STD_IN.newThread(
				new Runnable() {
					@Override
					public final void run() {
						try(
							final BufferedReader bufferedReader = new BufferedReader(
								new InputStreamReader(process.getInputStream())
							)
						) {
							String nextLine;
							while(null != (nextLine = bufferedReader.readLine())) {
								LOG.info(
									Markers.MSG, "{}{}{}", consoleColorFlag ? BLUE : "", nextLine,
									consoleColorFlag ? GREEN : ""
								);
							}
						} catch(final IOException e) {
							LogUtil.exception(
								LOG, Level.WARN, e, "Failed to read the process stdin"
							);
						}
					}
				}
			);
			processStdInReader.start();
			final Thread processStdErrReader = TF_STD_ERR.newThread(
				new Runnable() {
					@Override
					public final void run() {
						try(
							final BufferedReader bufferedReader = new BufferedReader(
								new InputStreamReader(process.getErrorStream())
							)
						) {
							String nextLine;
							while(null != (nextLine = bufferedReader.readLine())) {
								LOG.info(
									Markers.MSG, "{}{}{}", consoleColorFlag ? RED : "", nextLine,
									consoleColorFlag ? GREEN : ""
								);
							}
						} catch(final IOException e) {
							LogUtil.exception(
								LOG, Level.WARN, e, "Failed to read the process stdin"
							);
						}
					}
				}
			);
			processStdErrReader.start();
			if(blockingFlag) {
				try {
					final int exitCode = process.waitFor();
					if(exitCode == 0) {
						LOG.info(Markers.MSG, "Shell command \"{}\" finished", cmdLine);
					} else {
						LOG.warn(
							Markers.ERR, "Shell command \"{}\" finished with exit code {}", cmdLine,
							exitCode
						);
					}
				} catch(final InterruptedException e) {
					LOG.info(Markers.MSG, "Shell command \"{}\" interrupted", cmdLine);
				} finally {
					processStdInReader.interrupt();
					processStdErrReader.interrupt();
					process.destroy();
				}
			}
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Shell command \"{}\" failed", cmdLine);
		}
	}
}
