package com.emc.mongoose.base.logging

import org.junit.Assert.assertEquals
import org.junit.Test

class LogMessageBaseTest {
	
	@Test @throws[Exception]
	def testFormatFixedWidth(): Unit = {
		assertEquals("0", LogMessageBase formatFixedWidth(1.234e-100, 8))
		assertEquals("1.234E-7", LogMessageBase formatFixedWidth(0.000000123456789, 8))
		assertEquals("1.234E-6", LogMessageBase formatFixedWidth(0.00000123456789, 8))
		assertEquals("1.234E-5", LogMessageBase formatFixedWidth(0.0000123456789, 8))
		assertEquals("1.234E-4", LogMessageBase formatFixedWidth(0.000123456789, 8))
		assertEquals("0.001234", LogMessageBase formatFixedWidth(0.00123456789, 8))
		assertEquals("0.012345", LogMessageBase formatFixedWidth(0.0123456789, 8))
		assertEquals("0.123456", LogMessageBase formatFixedWidth(0.123456789, 8))
		assertEquals("1.234567", LogMessageBase formatFixedWidth(1.23456789, 8))
		assertEquals("12.34567", LogMessageBase formatFixedWidth(12.3456789, 8))
		assertEquals("123.4567", LogMessageBase formatFixedWidth(123.456789, 8))
		assertEquals("1234.567", LogMessageBase formatFixedWidth(1234.56789, 8))
		assertEquals("12345.67", LogMessageBase formatFixedWidth(12345.6789, 8))
		assertEquals("123456.7", LogMessageBase formatFixedWidth(123456.789, 8))
		assertEquals("1234567", LogMessageBase formatFixedWidth(1234567.89, 8))
		assertEquals("12345678", LogMessageBase formatFixedWidth(12345678.9, 8))
		assertEquals("1.2345E8", LogMessageBase formatFixedWidth(123456789, 8))
	}
}
