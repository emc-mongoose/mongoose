package com.emc.mongoose.common.net.http.request.format;

import com.emc.mongoose.common.generator.AsyncDoubleGenerator;
import com.emc.mongoose.common.generator.AsyncLongGenerator;
import com.emc.mongoose.common.generator.ValueGenerator;

public class HeaderValueGeneratorFactory {

	private HeaderValueGeneratorFactory() {
	}

	public ValueGenerator createGenerator(char type, boolean rangePresence) {
		switch (type) {
			case 'f':
				if (rangePresence) {
					return null;
				} else {
					return new AsyncDoubleGenerator(47.0);
				}
			case 'd':
				if (rangePresence) {
					return null;
				} else {
					return new AsyncLongGenerator(47l);
				}
			case 'D':
				if (rangePresence) {
					return null;
				} else {
					return null;
				}
			default:
				return null;
		}
	}
}
