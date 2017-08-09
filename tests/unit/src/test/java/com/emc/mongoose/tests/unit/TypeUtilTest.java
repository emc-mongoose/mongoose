package com.emc.mongoose.tests.unit;

import static com.emc.mongoose.api.common.reflection.TypeUtil.typeEquals;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

/**
 Created by andrey on 13.11.16.
 */
public class TypeUtilTest {

	public boolean mBooleanType() {
		return true;
	}

	public Boolean mBooleanClass() {
		return true;
	}

	public int mIntType() {
		return 0;
	}

	public Integer mIntClass() {
		return 0;
	}

	public long mLongType() {
		return 0;
	}

	public Long mLongClass() {
		return 0L;
	}

	public double mDoubleType() {
		return 0;
	}

	public Double mDoubleClass() {
		return 0.0;
	}

	public Object mObject() {
		return TypeUtilTest.class.getName();
	}

	public String mString() {
		return TypeUtilTest.class.getName();
	}

	@Test
	public void typeEqualsTest()
	throws Exception {

		final Method mBooleanType = TypeUtilTest.class.getMethod("mBooleanType");
		final Method mBooleanClass = TypeUtilTest.class.getMethod("mBooleanClass");
		assertTrue(typeEquals(mBooleanType.getReturnType(), mBooleanClass.getReturnType()));
		assertTrue(typeEquals(mBooleanClass.getReturnType(), mBooleanType.getReturnType()));

		final Method mIntType = TypeUtilTest.class.getMethod("mIntType");
		final Method mIntClass = TypeUtilTest.class.getMethod("mIntClass");
		assertTrue(typeEquals(mIntType.getReturnType(), mIntClass.getReturnType()));
		assertTrue(typeEquals(mIntClass.getReturnType(), mIntType.getReturnType()));

		final Method mLongType = TypeUtilTest.class.getMethod("mLongType");
		final Method mLongClass = TypeUtilTest.class.getMethod("mLongClass");
		assertTrue(typeEquals(mLongType.getReturnType(), mLongClass.getReturnType()));
		assertTrue(typeEquals(mLongClass.getReturnType(), mLongType.getReturnType()));

		final Method mDoubleType = TypeUtilTest.class.getMethod("mDoubleType");
		final Method mDoubleClass = TypeUtilTest.class.getMethod("mDoubleClass");
		assertTrue(typeEquals(mDoubleType.getReturnType(), mDoubleClass.getReturnType()));
		assertTrue(typeEquals(mDoubleClass.getReturnType(), mDoubleType.getReturnType()));

		final Method mObject = TypeUtilTest.class.getMethod("mObject");
		final Method mString = TypeUtilTest.class.getMethod("mString");
		assertFalse(typeEquals(mObject.getReturnType(), mString.getReturnType()));
		assertFalse(typeEquals(mString.getReturnType(), mObject.getReturnType()));

		assertFalse(typeEquals(mIntClass.getReturnType(), mLongClass.getReturnType()));
		assertFalse(typeEquals(mLongType.getReturnType(), mIntType.getReturnType()));
	}
}