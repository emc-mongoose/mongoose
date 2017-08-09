package com.emc.mongoose.ui.config.output.metrics.summary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class SummaryConfig
implements Serializable {

	public static final String KEY_PERFDBRESULTSFILE = "perfDbResultsFile";
	public static final String KEY_PERSIST = "persist";

	public final void setPerfDbResultsFile(final boolean perfDbResultsFileFlag) {
		this.perfDbResultsFileFlag = perfDbResultsFileFlag;
	}

	public final void setPersist(final boolean persistFlag) {
		this.persistFlag = persistFlag;
	}

	@JsonProperty(KEY_PERFDBRESULTSFILE) private boolean perfDbResultsFileFlag;
	@JsonProperty(KEY_PERSIST) private boolean persistFlag;

	public SummaryConfig() {
	}

	public SummaryConfig(final SummaryConfig other) {
		this.perfDbResultsFileFlag = other.getPerfDbResultsFileFlag();
		this.persistFlag = other.getPersist();
	}

	public final boolean getPerfDbResultsFileFlag() {
		return perfDbResultsFileFlag;
	}

	public final boolean getPersist() {
		return persistFlag;
	}
}