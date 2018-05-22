package com.emc.mongoose.load.step.type.linear;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.env.ExtensionBase;
import com.emc.mongoose.load.step.LoadStepFactory;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;

import java.util.List;
import java.util.Map;

public class LinearLoadStepFactory<T extends LinearLoadStep>
extends ExtensionBase
implements LoadStepFactory<T> {

	@Override
	public String id() {
		return LinearLoadStep.TYPE;
	}

	@Override @SuppressWarnings("unchecked")
	public T create(
		final Config baseConfig, final List<Extension> extensions,
		final List<Map<String, Object>> overrides
	) {
		return (T) new LinearLoadStep(baseConfig, extensions, overrides);
	}

	@Override
	protected final SchemaProvider schemaProvider() {
		return null;
	}

	@Override
	protected final String defaultsFileName() {
		return null;
	}
}
