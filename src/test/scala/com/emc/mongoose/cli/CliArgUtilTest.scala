package com.emc.mongoose.cli

import com.emc.mongoose.config.CliArgUtil

import org.junit.Assert.assertEquals
import org.junit.Test

import java.lang.Boolean.TRUE

final class CliArgUtilTest {

	@Test @throws[Exception]
	def test(): Unit = {
		val parsedArgs = CliArgUtil parseArgs(
			"--name=goose",
			"--io-buffer-size=1KB-4MB",
			"--storage-node-http-headers=key0:val0",
			"--storage-node-http-headers=key1:val1",
			"--enable-some-feature",
		)
		assertEquals("goose", parsedArgs get "name")
		assertEquals("1KB-4MB", parsedArgs get "io-buffer-size")
		val httpHeaders = new java.util.HashMap[String, String]()
		httpHeaders put("key0", "val0")
		httpHeaders put("key1", "val1")
		assertEquals(httpHeaders, parsedArgs get "storage-node-http-headers")
		assertEquals(TRUE toString, parsedArgs get "enable-some-feature")
	}
}
