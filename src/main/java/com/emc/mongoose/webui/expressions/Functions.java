package com.emc.mongoose.webui.expressions;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.TimeUtil;

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

	public static String getTimeValue(final RunTimeConfig runTimeConfig, final String key) {
		final String rawValue = runTimeConfig.getString(key);
		return Long.toString(TimeUtil.getTimeValue(rawValue));
	}

	public static String getTimeUnit(final RunTimeConfig runTimeConfig, final String key) {
		final String rawValue = runTimeConfig.getString(key);
		return TimeUtil.getTimeUnit(rawValue).toString().toLowerCase();
	}

	private static String convertArrayToString(final String[] stringArray) {
		return Arrays.toString(stringArray)
			.replace("[", "")
			.replace("]", "")
			.replace(" ", "")
			.trim();
	}

}
