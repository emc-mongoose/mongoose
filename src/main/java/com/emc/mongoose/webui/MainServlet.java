package com.emc.mongoose.webui;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.MimeTypes;
//
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by gusakk on 02/10/14.
 */
public final class MainServlet
extends HttpServlet {
	//
	private static final Logger LOG = LogManager.getLogger();
 	//
	@Override
	public final void doGet(
		final HttpServletRequest request, final HttpServletResponse response
	) {

		final ObjectNode rootNode = (ObjectNode) RunTimeConfig.getContext().getJsonNode();
		final JsonNode aliasingSection = rootNode.findValue(RunTimeConfig.PREFIX_KEY_ALIASING);
		if (aliasingSection != null) {
			walkTree(rootNode);
		}

		response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString());
		try {
			response.getWriter().write(rootNode.toString());
		} catch (final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to write json response");
		}
	}

	private void walkTree(final ObjectNode rootNode) {
		final Iterator<String> fieldNames = rootNode.fieldNames();
		while (fieldNames.hasNext()) {
			final String field = fieldNames.next();
			if (field.equals(RunTimeConfig.PREFIX_KEY_ALIASING)) {
				rootNode.remove(field);
				break;
			} else {
				walkTree((ObjectNode) rootNode.get(field));
			}
		}
	}

}
