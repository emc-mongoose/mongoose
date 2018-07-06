package com.emc.mongoose.logging

import org.junit.Test

class LogMessageBaseTest {
	
	@Test @throws[Exception]
	def testFormatFixedWidth(): Unit = {
		System.out.println(LogMessageBase formatFixedWidth(0.123456789, 8))
		System.out.println(LogMessageBase formatFixedWidth(1.23456789, 8))
		System.out.println(LogMessageBase formatFixedWidth(12.3456789, 8))
		System.out.println(LogMessageBase formatFixedWidth(123.456789, 8))
		System.out.println(LogMessageBase formatFixedWidth(1234.56789, 8))
		System.out.println(LogMessageBase formatFixedWidth(12345.6789, 8))
		System.out.println(LogMessageBase formatFixedWidth(123456.789, 8))
		System.out.println(LogMessageBase formatFixedWidth(1234567.89, 8))
		System.out.println(LogMessageBase formatFixedWidth(12345678.9, 8))
		System.out.println(LogMessageBase formatFixedWidth(123456789, 8))
	}
}
