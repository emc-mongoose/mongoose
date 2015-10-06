package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.DataItemDst;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.model.CSVFileItemDst;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.integ.base.WSMockTestBase;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_AVG_CLIENT;

/**
 * Created by gusakk on 06.10.15.
 */
public class CircularReadTest
extends StandaloneClientTestBase {
	//
	private static final int WRITE_COUNT = 12345;
	private static final int READ_COUNT = 123450;
	//
	private static long COUNT_WRITTEN, COUNT_READ;
	private static byte[] STD_OUT_CONTENT;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(
			RunTimeConfig.KEY_RUN_ID, CircularReadTest.class.getCanonicalName()
		);
		WSMockTestBase.setUpClass();
		//
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_DATA_SRC_CIRCULAR, true);
		RunTimeConfig.setContext(rtConfig);
		//
		CLIENT_BUILDER = new BasicWSClientBuilder<>()
			.setClientMode(null);
		//
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setAPI("s3")
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(WRITE_COUNT)
				.build()
		) {
			final DataItemDst<WSObject> writeOutput = new CSVFileItemDst<WSObject>(
					BasicWSObject.class
			);
			COUNT_WRITTEN = client.write(
				null, writeOutput, WRITE_COUNT, 1, SizeUtil.toSize("1MB")
			);
			TimeUnit.SECONDS.sleep(10);
			//
			try (
				final BufferingOutputStream
					stdOutInterceptorStream = StdOutInterceptorTestSuite.getStdOutBufferingStream()
			) {
				stdOutInterceptorStream.reset();
				if (COUNT_WRITTEN > 0) {
					COUNT_READ = client.read(writeOutput.getDataItemSrc(), null, READ_COUNT, 1, true);
				} else {
					throw new IllegalStateException("Failed to write");
				}
				TimeUnit.SECONDS.sleep(1);
				STD_OUT_CONTENT = stdOutInterceptorStream.toByteArray();
			}
			System.out.println("Successfully read " + COUNT_READ);
		}
	}
	//
	@AfterClass
	public static void tearDown()
	throws Exception {
		StandaloneClientTestBase.tearDownClass();
	}
	//
	@Test
	public void checkItemsFileContainsDuplicates()
	throws  Exception {
		/*Assert.assertTrue(
			"Data items' log file \"" + FILE_LOG_DATA_ITEMS + "\" doesn't exist",
			FILE_LOG_DATA_ITEMS.exists()
		);
		//
		final Map<String, Integer> items = new HashMap<>();
		try(
			final BufferedReader
				in = Files.newBufferedReader(FILE_LOG_DATA_ITEMS.toPath(), StandardCharsets.UTF_8)
		) {
			//if (items.containsKey())
		}*/
	}
	//

}
