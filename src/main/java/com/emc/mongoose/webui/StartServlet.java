package com.emc.mongoose.webui;

import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.object.data.WSDataObject;
import com.emc.mongoose.object.load.impl.WSLoadBuilder;
import com.emc.mongoose.object.load.impl.WSLoadExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by gusakk on 01/10/14.
 */

public class StartServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        Request.Type loadType = Request.Type.valueOf(RunTimeConfig.getString("scenario.single.load").toUpperCase());
        WSLoadBuilder<WSDataObject, WSLoadExecutor<WSDataObject>> loadBuilder = new WSLoadBuilder<>();
        loadBuilder.setLoadType(loadType);

        WSLoadExecutor loadExecutor = loadBuilder.build();
        loadExecutor.start();

        String timeOutString = RunTimeConfig.getString("run.time");
        String[] timeOutArray = timeOutString.split("\\.");

        try {
            loadExecutor.join(TimeUnit.valueOf(timeOutArray[1].toUpperCase()).toMillis(Integer.valueOf(timeOutArray[0])));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        loadExecutor.close();
    }

}
