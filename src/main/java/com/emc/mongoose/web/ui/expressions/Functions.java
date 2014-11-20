package com.emc.mongoose.web.ui.expressions;

import com.emc.mongoose.util.conf.RunTimeConfig;

import java.util.Arrays;

/**
 * Created by gusakk on 10/18/14.
 */
public class Functions {

	public static String getString(final RunTimeConfig runTimeConfig, final String key) {
		if (key.equals("scenario.chain.load")) {
			return convertArrayToString(runTimeConfig, key);
		}
        return runTimeConfig.getString(key);
    }

	private static String convertArrayToString(final RunTimeConfig runTimeConfig, final String key) {
		return Arrays.toString(runTimeConfig.getStringArray(key))
												.replace("[", "")
												.replace("]", "")
												.replace(" ", "")
												.trim();
	}

}
