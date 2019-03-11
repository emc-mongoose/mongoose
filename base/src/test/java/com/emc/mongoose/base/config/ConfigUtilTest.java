package com.emc.mongoose.base.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ConfigUtilTest {

	@Test
	public void testFlatten() throws Exception {

		final Map<String, Object> srcMap = new HashMap<String, Object>() {
			{
				put(
								"a",
								new HashMap<String, Object>() {
									{
										put("aa", null);
										put("bb", 123);
									}
								});
				put(
								"b",
								new HashMap<String, Object>() {
									{
										put("aa", "yohoho");
										put("bb", true);
									}
								});
			}
		};

		final String sep = "-";
		final Map<String, String> dstMap = new HashMap<>();
		ConfigUtil.flatten(srcMap, dstMap, sep, null);

		assertNull(dstMap.get("a-aa"));
		assertEquals("123", dstMap.get("a-bb"));
		assertEquals("yohoho", dstMap.get("b-aa"));
		assertEquals("true", dstMap.get("b-bb"));
	}
}
