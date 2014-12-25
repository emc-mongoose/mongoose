package com.emc.mongoose.web.ui.enums;

/**
 * Created by gusakk on 02/10/14.
 */
public enum RunModes {

	VALUE_RUN_MODE_STANDALONE("standalone"),
	VALUE_RUN_MODE_WSMOCK("wsmock"),
	VALUE_RUN_MODE_SERVER("server"),
	VALUE_RUN_MODE_CLIENT("client");

	private final String value;

	RunModes(final String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static RunModes getRunModeConstantByRequest(final String runMode) {
		for (final RunModes runModeConstant : values()) {
			if (runModeConstant.getValue().equals(runMode))
				return runModeConstant;
		}
		return null;
	}
}
