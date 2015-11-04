package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.11.15.
 */
public class CambridgeLabDistributedTestBase
extends CambridgeLabViprTestBase {
	//
	protected final static String LOAD_SVC_ADDRS_CUSTOM[] = {"10.249.237.76", "10.249.237.77"};
	private final static String
		GOOSE_NAME = RunTimeConfig.getContext().getRunName(),
		GOOSE_VERSION = RunTimeConfig.getContext().getRunVersion(),
		GOOSE_REMOTE_PATH = "/workspace/" + GOOSE_NAME + "-" + GOOSE_VERSION + ".tgz",
		SECRET_DEFAULT = "TLMer+7YMPKCNwS6VzSTbJBP173orXP7Pop2J8+e";
	private final static File
		GOOSE_TGZ_FILE = Paths.get("build", "dist", GOOSE_NAME + "-" + GOOSE_VERSION + ".tgz").toFile(),
		GOOSE_JAR_FILE = Paths.get(GOOSE_NAME + "-" + GOOSE_VERSION, GOOSE_NAME + ".jar").toFile();
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		CambridgeLabViprTestBase.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		final StringBuilder sb = new StringBuilder();
		for(final String loadSvcAddr : LOAD_SVC_ADDRS_CUSTOM) {
			if(sb.length() > 0) {
				sb.append(',');
			}
			sb.append(loadSvcAddr);
		}
		rtConfig.set(RunTimeConfig.KEY_LOAD_SERVER_ADDRS, sb.toString());
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		rtConfig.set(RunTimeConfig.KEY_AUTH_SECRET, SECRET_DEFAULT);
		if(!GOOSE_TGZ_FILE.exists()) {
			Assert.fail("Mongoose tgz file not found @ " + GOOSE_TGZ_FILE.getAbsolutePath());
		}
		applyDeploymentOutputIfAny(rtConfig);
		final ExecutorService deployExecutor = Executors.newFixedThreadPool(
			LOAD_SVC_ADDRS_CUSTOM.length
		);
		for(final String loadSvcAddr : LOAD_SVC_ADDRS_CUSTOM) {
			deployExecutor.submit(
				new Runnable() {
					@Override
					public void run() {
						deployLoadSvc(loadSvcAddr);
					}
				}
			);
		}
		deployExecutor.shutdown();
		deployExecutor.awaitTermination(1, TimeUnit.MINUTES);
		deployExecutor.shutdownNow();
		TimeUnit.SECONDS.sleep(1);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		CambridgeLabViprTestBase.tearDownClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_LOAD_SERVER_ADDRS, "127.0.0.1");
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_STANDALONE);
	}
	//
	private static void applyDeploymentOutputIfAny(final RunTimeConfig rtConfig) {
		// copy paste from HumanFriendly follows below
		final String fileName = System.getenv("DevBranch");
		final File file = new File(fileName + "/tools/cli/python/DeploymentOutput");
		final Properties props = new Properties();
		try(final FileInputStream stream = new FileInputStream(file)){
			props.load(stream);
			LOG.info(Markers.MSG, "Using custom auth id: \"{}\"", props.getProperty("user"));
			rtConfig.set(RunTimeConfig.KEY_AUTH_ID, props.getProperty("user"));
			LOG.info(Markers.MSG, "Using custom auth secret: \"{}\"", props.getProperty("secretkey"));
			rtConfig.set(RunTimeConfig.KEY_AUTH_SECRET, props.getProperty("secretkey"));
			final String dataNodes = System.getenv("DataNodes")
				.replace('(', ' ').replace(')', ' ').trim().replace(' ', ',');
			LOG.info(Markers.MSG, "Using custom nodes: \"{}\"", dataNodes);
			rtConfig.set(RunTimeConfig.KEY_STORAGE_ADDRS, dataNodes);
		} catch(final FileNotFoundException e) {
			LOG.info(Markers.ERR, "Deployment output file not found");
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to load the deployment output file");
		}
	}
	//
	private static void deployLoadSvc(final String loadSvcAddr) {
		try {
			final String
				cmd = "scp " + GOOSE_TGZ_FILE.getAbsolutePath() + " root@" + loadSvcAddr +
					":" + GOOSE_REMOTE_PATH;
			LOG.info(Markers.MSG, cmd);
			final Process p = Runtime.getRuntime().exec(cmd);
			try {
				p.waitFor();
				if(0 != p.exitValue()) {
					try(
						final BufferedReader in = new BufferedReader(
							new InputStreamReader(p.getErrorStream())
						)
					) {
						String l;
						do {
							l = in.readLine();
							if(l != null) {
								LOG.warn(Markers.ERR, l);
							}
						} while(true);
					}
				}
			} catch(final InterruptedException e) {
			} finally {
				p.destroy();
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Process start failure");
		}
		//
		try {
			final String cmd[] = {
				"/bin/sh", "-c",
				"echo \"killall java; killall screen; cd /workspace; tar xvf " +
					GOOSE_REMOTE_PATH + "; screen -d -m bash -c 'java -jar /workspace/" +
					GOOSE_NAME + "-" + GOOSE_VERSION + "/" + GOOSE_JAR_FILE.getName() +
					" server'\" | ssh root@" + loadSvcAddr +
					" -- /bin/bash"
			};
			LOG.info(Markers.MSG, cmd[2]);
			final Process p = Runtime.getRuntime().exec(cmd);
			final ProcessBuilder pb = new ProcessBuilder();
			pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
			try {
				p.waitFor();
				if(0 != p.exitValue()) {
					try(
						final BufferedReader in = new BufferedReader(
							new InputStreamReader(p.getErrorStream())
						)
					) {
						String l;
						do {
							l = in.readLine();
							if(l != null) {
								LOG.warn(Markers.ERR, loadSvcAddr + ": " + l);
							}
						} while(true);
					}
				} else {
					try(
						final BufferedReader in = new BufferedReader(
							new InputStreamReader(p.getInputStream())
						)
					) {
						String l;
						do {
							l = in.readLine();
							if(l != null) {
								LOG.info(Markers.MSG, loadSvcAddr + ": " + l);
							}
						} while(true);
					}
				}
			} catch(final InterruptedException e) {
			} finally {
				p.destroy();
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Process start failure");
		}
	}
	//
}
