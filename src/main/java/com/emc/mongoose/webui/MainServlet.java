package com.emc.mongoose.webui;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.MimeTypes;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static com.emc.mongoose.common.conf.AppConfig.FNAME_CONF;
import static com.emc.mongoose.common.conf.BasicConfig.getRootDir;

public final class MainServlet
extends HttpServlet {

	private static final Logger LOG = LogManager.getLogger();

	@Override
	public final void doGet(
		final HttpServletRequest request, final HttpServletResponse response
	) {
		final String appConfigPathString =
				Paths.get(getRootDir(), Constants.DIR_CONF).resolve(FNAME_CONF).toString();

		try {
			final String appConfigJson = new ObjectMapper().readTree(new File(appConfigPathString))
					.toString();
			response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
			response.getWriter().write(appConfigJson);
		} catch (IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to write json response");
		}
	}

}
