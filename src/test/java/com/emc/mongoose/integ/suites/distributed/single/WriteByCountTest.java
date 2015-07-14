package com.emc.mongoose.integ.suites.distributed.single;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.integ.tools.SavedOutputStream;
//
import com.emc.mongoose.run.cli.ModeDispatcher;
import com.emc.mongoose.server.api.load.builder.WSLoadBuilderSvc;
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
import org.junit.AfterClass;
import org.junit.BeforeClass;
//
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
/**
 Created by kurila on 14.07.15.
 */
public final class WriteByCountTest {
	//
	private final static OutputStream
		STREAM_STD_INTERCEPTOR = new SavedOutputStream(System.out);
	private final static int DATA_COUNT = 100000;
	private static Thread
		WSMOCK_THREAD, LOAD_SERVER_THREAD;
	private static RunTimeConfig RT_CONFIG;
	//
	private static final String
		LOG_CONF_PROPERTY_KEY = "log4j.configurationFile",
		LOG_FILE_NAME = "logging.json",
		USER_DIR_PROPERTY_NAME = "user.dir",
		SUMMARY_INDICATOR = "summary:",
		SCENARIO_END_INDICATOR = "Scenario end",
		//
		MESSAGE_FILE_NAME = "messages.log",
		PERF_AVG_FILE_NAME = "perf.avg.csv",
		PERF_SUM_FILE_NAME = "perf.sum.csv",
		PERF_TRACE_FILE_NAME = "perf.trace.csv",
		DATA_ITEMS_FILE_NAME = "data.items.csv";
	//
}
