package com.emc.mongoose.web.ui;

import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.DirectoryLoader;
import com.emc.mongoose.util.conf.RunTimeConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by gusakk on 12/28/14.
 */
public class SaveServlet extends HttpServlet {

	private RunTimeConfig runTimeConfig;

	@Override
	public void init() throws ServletException {
		super.init();
		runTimeConfig = Main.RUN_TIME_CONFIG.get().clone();
	}

	@Override
	public void doPost(final HttpServletRequest request, final HttpServletResponse response) {
		setupRunTimeConfig(request);
		DirectoryLoader.loadPropsToDirsFromRunTimeConfig(Paths.get(Main.DIR_ROOT, Main.DIR_CONF, Main.DIR_PROPERTIES), runTimeConfig);
		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	public void doGet(final HttpServletRequest request, final HttpServletResponse response) {

	}

	private void setupRunTimeConfig(final HttpServletRequest request) {
		for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			if (entry.getValue()[0].trim().isEmpty()) {
				continue;
			}
			if (entry.getValue().length > 1) {
				runTimeConfig.set(entry.getKey(), convertArrayToString(entry.getKey(), entry.getValue()));
				continue;
			}
			runTimeConfig.set(entry.getKey(), entry.getValue()[0].trim());
		}
	}

	private String convertArrayToString(final String key, final String[] stringArray) {
		final String resultString = Arrays.toString(stringArray)
				.replace("[", "")
				.replace("]", "")
				.replace(" ", "")
				.trim();
		if (key.equals("run.time"))
			return resultString.replace(",", ".");
		return resultString;
	}

}
