package com.emc.mongoose.api.common.reflection;

/**
 Created by andrey on 13.11.16.
 */
public interface TypeUtil {

	static boolean typeEquals(final Class cls1, final Class cls2) {
		if(cls1.equals(cls2)) {
			return true;
		} else {
			Object primitiveType1 = null;
			try {
				primitiveType1 = cls1.getField("TYPE").get(cls1);
			} catch(final NoSuchFieldException | IllegalAccessException ignored) {
			}

			Object primitiveType2 = null;
			try {
				primitiveType2 = cls2.getField("TYPE").get(cls2);
			} catch(final NoSuchFieldException | IllegalAccessException ignored) {
			}

			if(primitiveType1 == null) {
				if(primitiveType2 == null) {
					return false;
				} else {
					return cls1.equals(primitiveType2);
				}
			} else if(primitiveType2 == null) {
				return cls2.equals(primitiveType1);
			} else {
				return primitiveType1.equals(primitiveType2);
			}
		}
	}
}
