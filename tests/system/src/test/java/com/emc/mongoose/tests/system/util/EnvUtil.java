package com.emc.mongoose.tests.system.util;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

/**
 Created by kurila on 28.03.17.
 http://stackoverflow.com/a/496849
 */
public interface EnvUtil {
	
	static void replace(final Map<String, String> newEnv)
	throws Exception {
		final Class[] classes = Collections.class.getDeclaredClasses();
		final Map<String, String> env = System.getenv();
		for(final Class cls : classes) {
			if("java.util.Collections$UnmodifiableMap".equals(cls.getName())) {
				final Field field = cls.getDeclaredField("m");
				field.setAccessible(true);
				final Object obj = field.get(env);
				final Map<String, String> map = (Map<String, String>) obj;
				map.clear();
				map.putAll(newEnv);
			}
		}
	}

	static void set(final String key, final String value)
	throws Exception {
		final Class[] classes = Collections.class.getDeclaredClasses();
		final Map<String, String> env = System.getenv();
		for(final Class cls : classes) {
			if("java.util.Collections$UnmodifiableMap".equals(cls.getName())) {
				final Field field = cls.getDeclaredField("m");
				field.setAccessible(true);
				final Object obj = field.get(env);
				final Map<String, String> map = (Map<String, String>) obj;
				map.put(key, value);
			}
		}
	}
}
