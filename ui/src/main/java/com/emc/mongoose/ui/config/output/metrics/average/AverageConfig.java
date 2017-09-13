package com.emc.mongoose.ui.config.output.metrics.average;

import com.emc.mongoose.ui.config.TimeStrToLongDeserializer;
import com.emc.mongoose.ui.config.output.metrics.average.table.TableConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class AverageConfig
implements Serializable {

	public static final String KEY_PERIOD = "period";
	public static final String KEY_PERSIST = "persist";
	public static final String KEY_TABLE = "table";

	public final void setPeriod(final long period) {
		this.period = period;
	}

	public final void setPersist(final boolean persistFlag) {
		this.persistFlag = persistFlag;
	}

	public final void setTableConfig(final TableConfig tableConfig) {
		this.tableConfig = tableConfig;
	}

	@JsonDeserialize(using = TimeStrToLongDeserializer.class)
	@JsonProperty(KEY_PERIOD)
	private long period;

	@JsonProperty(KEY_PERSIST) private boolean persistFlag;
	@JsonProperty(KEY_TABLE) private TableConfig tableConfig;

	public AverageConfig() {
	}

	public AverageConfig(final AverageConfig other) {
		this.period = other.getPeriod();
		this.persistFlag = other.getPersist();
		this.tableConfig = new TableConfig(other.getTableConfig());
	}

	public final long getPeriod() {
		return period;
	}

	public final boolean getPersist() {
		return persistFlag;
	}

	public final TableConfig getTableConfig() {
		return tableConfig;
	}
}