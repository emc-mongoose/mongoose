package com.emc.mongoose.webui.expressions;

import com.emc.mongoose.util.conf.RunTimeConfig;

import java.util.Arrays;

/**
 * Created by gusakk on 10/18/14.
 */
public class Functions {

	public static String getString(final RunTimeConfig runTimeConfig, final String key) {
		String[] stringArray = runTimeConfig.getStringArray(key);
		if (runTimeConfig.getStringArray(key).length > 1) {
			return convertArrayToString(stringArray);
		}
		if (stringArray.length == 0) {
			return "";
		}
		return stringArray[0];
    }

	private static String convertArrayToString(final String[] stringArray) {
		return Arrays.toString(stringArray)
								.replace("[", "")
								.replace("]", "")
								.replace(" ", "")
								.trim();
	}

}
