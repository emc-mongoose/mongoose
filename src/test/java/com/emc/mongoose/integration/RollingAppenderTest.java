package com.emc.mongoose.integration;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.SimpleLayout;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RollingAppenderTest {

	private static final int LIMIT = 5;
	private static String fileName = "/home/user/mongoose/log4jFileTest.txt";
	public static Logger logger = Logger.getLogger(RollingAppenderTest.class);
	public static Appender appender;

	@Test
	public void test()
	throws InterruptedException, IOException {
		appender = new RollingFileAppender(new SimpleLayout(), fileName);
		logger.addAppender(appender);
		for(int counter = 0; counter < LIMIT; ++ counter) {
			TimeUnit.SECONDS.sleep(1);
			System.out.println(counter);
			RollingAppenderTest.logger.info(counter);
		}
		new File(fileName).delete();
		System.out.println("<File is deleted>");
		//logger.removeAllAppenders();
		//appender = new RollingFileAppender(new SimpleLayout(), fileName);
		//logger.addAppender(appender);

		for(int counter = LIMIT; counter < LIMIT * 2; ++ counter) {
			TimeUnit.SECONDS.sleep(1);
			System.out.println(counter);
			RollingAppenderTest.logger.info(counter);
		}
	}
}


