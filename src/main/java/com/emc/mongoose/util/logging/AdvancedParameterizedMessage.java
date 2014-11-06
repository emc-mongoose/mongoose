package com.emc.mongoose.util.logging;

import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gusakk on 11/2/14.
 */
public class AdvancedParameterizedMessage extends ParameterizedMessage {

	private final MapMessage map;

	public AdvancedParameterizedMessage(String messagePattern, String[] stringArgs, Throwable throwable) {
		super(messagePattern, stringArgs, throwable);
		map = new MapMessage();
	}

	public AdvancedParameterizedMessage(String messagePattern, Object[] objectArgs, Throwable throwable) {
		super(messagePattern, objectArgs, throwable);
		map = new MapMessage();
	}

	public AdvancedParameterizedMessage(String messagePattern, Object[] arguments) {
		super(messagePattern, arguments);
		map = new MapMessage();
	}

	public AdvancedParameterizedMessage(String messagePattern, Object arg) {
		super(messagePattern, arg);
		map = new MapMessage();
	}

	public AdvancedParameterizedMessage(String messagePattern, Object arg1, Object arg2) {
		super(messagePattern, arg1, arg2);
		map = new MapMessage();
	}

	public MapMessage getMapMessage() {
		return map;
	}

	public void putAll(HashMap<String, String> hashMap) {
		map.putAll(hashMap);
	}

	public void put(String key, String value) {
		map.put(key, value);
	}

}
