package com.emc.mongoose.webui;

import com.emc.mongoose.util.conf.RunTimeConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.SendingContext.RunTime;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by gusakk on 02/10/14.
 */
public class MainServlet extends HttpServlet {

    private static final Logger LOG = LogManager.getLogger();

    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        request.setAttribute("runmodes", RunModes.values());
        request.getRequestDispatcher("index.jsp").forward(request, response);
    }

}
