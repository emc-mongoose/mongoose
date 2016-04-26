package com.emc.mongoose.webui;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.io.JsonUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.MimeTypes;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.emc.mongoose.common.conf.AppConfig.FNAME_CONF;
import static com.emc.mongoose.common.conf.BasicConfig.getRootDir;
import static java.nio.file.Files.newBufferedReader;

public final class MainServlet
		extends HttpServlet {

	private static final Logger LOG = LogManager.getLogger();
	private static final StringBuilder FULL_JSON_BUILDER = new StringBuilder();

	private static final Path PATH_TO_APP_CONFIG_DIR =
			Paths.get(getRootDir(), Constants.DIR_CONF).resolve(FNAME_CONF);
	public static final Path PATH_TO_SCENARIO_DIR =
			Paths.get(getRootDir(), Constants.DIR_SCENARIO);
	private static final String APP_CONFIG_JSON_KEY = "appConfig";
	private static final String SCENARIOS_JSON_KEY = "scenarios";

	@Override
	public final void doGet(
			final HttpServletRequest request, final HttpServletResponse response
	) {
		try {
			final String appConfigJson = JsonUtil.readFileToString(PATH_TO_APP_CONFIG_DIR);
			final String scenarioDirContentsJson = JsonUtil.jsArrayPathContent(PATH_TO_SCENARIO_DIR);
			FULL_JSON_BUILDER.setLength(0);
			FULL_JSON_BUILDER.append("{ \"").append(APP_CONFIG_JSON_KEY).append("\": ")
					.append(appConfigJson).append(", ")
					.append("\"" + SCENARIOS_JSON_KEY + "\":")
					.append(scenarioDirContentsJson).append("}");
			response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
			response.getWriter().write(FULL_JSON_BUILDER.toString());
		} catch (IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to write json response");
		}
	}

}
