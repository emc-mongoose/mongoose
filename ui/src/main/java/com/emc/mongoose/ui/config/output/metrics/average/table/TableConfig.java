package com.emc.mongoose.ui.config.output.metrics.average.table;

import com.emc.mongoose.ui.config.output.metrics.average.table.header.HeaderConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class TableConfig
implements Serializable {

	public static final String KEY_HEADER = "header";

	public final void setHeaderConfig(final HeaderConfig headerConfig) {
		this.headerConfig = headerConfig;
	}

	@JsonProperty(KEY_HEADER) private HeaderConfig headerConfig;

	public TableConfig() {
	}

	public TableConfig(final TableConfig other) {
		this.headerConfig = new HeaderConfig(other.getHeaderConfig());
	}

	public final HeaderConfig getHeaderConfig() {
		return headerConfig;
	}
}