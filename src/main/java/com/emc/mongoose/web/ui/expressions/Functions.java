package com.emc.mongoose.web.ui.expressions;

import com.emc.mongoose.util.conf.RunTimeConfig;

/**
 * Created by gusakk on 10/18/14.
 */
public class Functions {

    public static String getString(RunTimeConfig runTimeConfig, String key) {
        return runTimeConfig.getString(key);
    }
}
