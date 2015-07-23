package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
//
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.integ.tools.ProcessManager;
import com.emc.mongoose.integ.tools.TestConstants;
//
import com.emc.mongoose.run.scenario.ScriptRunner;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by olga on 23.07.15.
 */
public class InfiniteWriteTest {
	//
	private static BufferingOutputStream savedOutputStream;
	//
	private static String createRunId = TestConstants.LOAD_CREATE;
	private static final long EXPECTED_RUN_TIME = 180000;
	private static long ACTUAL_RUN_TIME;
	private static Thread writeScenarioMongoose;

	@BeforeClass
	public static void before()
		throws Exception {
		// Set new saved console output stream
		savedOutputStream = new BufferingOutputStream(System.out);
		System.setOut(new PrintStream(savedOutputStream));

		//Create run ID
		createRunId += "Infinite:" + ":" + TestConstants.FMT_DT.format(
			Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime()
		);

		System.setProperty(RunTimeConfig.KEY_RUN_ID, createRunId);
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(TestConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, TestConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(TestConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);

		LogUtil.init();
		final Logger rootLogger = org.apache.logging.log4j.LogManager.getRootLogger();

		//Reload default properties
		final RunTimeConfig runTimeConfig = new RunTimeConfig();
		runTimeConfig.loadProperties();
		RunTimeConfig.setContext(runTimeConfig);

		//run mongoose default scenario in standalone mode
		writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				//Create thread for call SIGINT
				final Thread processSIGINT = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							System.out.println("start");
							Thread.sleep(EXPECTED_RUN_TIME);
							System.out.println("kill");
							ProcessManager.callSIGINT(writeScenarioMongoose.getId());
						} catch (final InterruptedException e) {
							//do nothing
						} catch (IOException e) {
							LogUtil.exception(rootLogger, Level.ERROR, e,
								"Can't call SIGINT ");
						}

					}
				}, "processSIGINT");
				processSIGINT.setDaemon(true);
				processSIGINT.start();
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, createRunId);
				// For correct work of verification option
				UniformDataSource.DEFAULT = new UniformDataSource();
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "writeScenarioMongoose");



		ACTUAL_RUN_TIME = System.nanoTime();
		writeScenarioMongoose.start();
		writeScenarioMongoose.join();
		writeScenarioMongoose.interrupt();
		ACTUAL_RUN_TIME = System.nanoTime() - ACTUAL_RUN_TIME;
		// Wait logger's output from console
		Thread.sleep(3000);
		savedOutputStream.close();
	}

	@Test
	public void shouldBlaBla()
	throws Exception{
		System.out.println(ACTUAL_RUN_TIME);
	}
}
