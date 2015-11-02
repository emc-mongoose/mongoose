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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
/**
 Created by kurila on 02.11.15.
 */
public class CambridgeLabDistributedTestBase
extends CambridgeLabViprTestBase {
	//
	private static String LOAD_SVC_ADDRS_DEFAULT, RUN_MODE_DEFAULT;
	protected final static String LOAD_SVC_ADDRS_CUSTOM[] = {"10.249.237.76", "10.249.237.77"};
	private final static String
		GOOSE_NAME = RunTimeConfig.getContext().getRunName(),
		GOOSE_VERSION = RunTimeConfig.getContext().getRunVersion(),
		GOOSE_REMOTE_PATH = "/workspace/" + GOOSE_NAME + "-" + GOOSE_VERSION + ".tgz";
	private final static File
		GOOSE_TGZ_FILE = Paths.get("build", "dist", GOOSE_NAME + "-" + GOOSE_VERSION + ".tgz").toFile(),
		GOOSE_JAR_FILE = Paths.get(GOOSE_NAME + "-" + GOOSE_VERSION, GOOSE_NAME + ".jar").toFile();
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		CambridgeLabViprTestBase.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		LOAD_SVC_ADDRS_DEFAULT = rtConfig.getString(RunTimeConfig.KEY_LOAD_SERVER_ADDRS);
		RUN_MODE_DEFAULT = rtConfig.getRunMode();
		rtConfig.set(
			RunTimeConfig.KEY_LOAD_SERVER_ADDRS,
			LOAD_SVC_ADDRS_CUSTOM[0] + "," + LOAD_SVC_ADDRS_CUSTOM[1]
		);
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		if(!GOOSE_TGZ_FILE.exists()) {
			Assert.fail("Mongoose tgz file not found @ " + GOOSE_TGZ_FILE.getAbsolutePath());
		}
		for(final String loadSvcAddr : LOAD_SVC_ADDRS_CUSTOM) {
			deployLoadSvc(loadSvcAddr);
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		CambridgeLabViprTestBase.tearDownClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_LOAD_SERVER_ADDRS, LOAD_SVC_ADDRS_DEFAULT);
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_DEFAULT);
	}
	//
	private static void deployLoadSvc(final String loadSvcAddr)
	throws IOException, InterruptedException {
		//
		Thread t = new Thread() {
			@Override
			public final void run() {
				try {
					final String cmd = "scp " + GOOSE_TGZ_FILE.getAbsolutePath() + " root@" + loadSvcAddr + ":" + GOOSE_REMOTE_PATH;
					LOG.info(Markers.MSG, cmd);
					final Process p = Runtime.getRuntime().exec(cmd);
					try {
						p.waitFor();
						if(0 != p.exitValue()) {
							try(
								final BufferedReader
									in = new BufferedReader(
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
			}
		};
		t.start();
		t.join(100000);
		t.interrupt();
		//
		t = new Thread() {
			@Override
			public final void run() {
				try {
					final String cmd = "ssh root@" + loadSvcAddr + " \"killall screen; killall java\"";
					LOG.info(Markers.MSG, cmd);
					final Process p = Runtime.getRuntime().exec(cmd);
					try {
						p.waitFor();
						if(0 != p.exitValue()) {
							try(
								final BufferedReader
									in = new BufferedReader(
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
			}
		};
		t.start();
		t.join(100000);
		t.interrupt();
		//
		t = new Thread() {
			@Override
			public final void run() {
				try {
					final String cmd = "ssh root@" + loadSvcAddr + " \"cd /workspace; tar xvf " + GOOSE_REMOTE_PATH + "\"";
					LOG.info(Markers.MSG, cmd);
					final Process p = Runtime.getRuntime().exec(cmd);
					try {
						p.waitFor();
						if(0 != p.exitValue()) {
							try(
								final BufferedReader
									in = new BufferedReader(
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
			}
		};
		t.start();
		t.join(100000);
		t.interrupt();
		//
		t = new Thread() {
			@Override
			public final void run() {
				try {
					final String cmd = "ssh root@" + loadSvcAddr + " \"screen -d -m bash -c 'cd /workspace; /usr/bin/java -jar /workspace/ " +
						GOOSE_NAME + "-" + GOOSE_VERSION + "/" + GOOSE_JAR_FILE.getName() + " server'\"";
					LOG.info(Markers.MSG, cmd);
					final Process p = Runtime.getRuntime().exec(cmd);
					try {
						p.waitFor();
						if(0 != p.exitValue()) {
							try(
								final BufferedReader
									in = new BufferedReader(
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
			}
		};
		t.start();
		t.join(100000);
		t.interrupt();
	}
}
