package com.emc.mongoose.common.conf;
//
import java.nio.charset.StandardCharsets;
/**
 Created by kurila on 17.03.15.
 */
public interface Constants {
	String DEFAULT_ENC = StandardCharsets.UTF_8.name();
	String DIR_CONF = "conf";
	String DIR_LOG = "log";
	String STATES_FILE = ".loadStates";
	String DIR_PROPERTIES = "properties";
	String DOT = ".";
	String EMPTY = "";
	String FNAME_POLICY = "allpermissions.policy";
	String RUN_MODE_STANDALONE = "standalone";
	String RUN_MODE_CLIENT = "client";
	String RUN_MODE_COMPAT_CLIENT = "controller";
	String RUN_MODE_SERVER = "server";
	String RUN_MODE_COMPAT_SERVER = "driver";
	String RUN_MODE_WEBUI = "webui";
	String RUN_MODE_CINDERELLA = "cinderella";
	String RUN_MODE_WSMOCK = "wsmock";
	String RUN_SCENARIO_SINGLE = "single";
	String RUN_SCENARIO_CHAIN = "chain";
	String RUN_SCENARIO_RAMPUP = "rampup";
	//
	String DIR_WEBAPP = "webapp";
	String DIR_WEBINF = "WEB-INF";
	//
	int BUFF_SIZE_LO = (int) RunTimeConfig.getContext().getIOBufferSizeMin();
	int BUFF_SIZE_HI = (int) RunTimeConfig.getContext().getIOBufferSizeMax();
}
