package com.emc.mongoose.load.step.type.chain;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.env.ExtensionBase;
import com.emc.mongoose.load.step.LoadStepFactory;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;

import java.util.List;
import java.util.Map;

public class ChainLoadStepFactory<T extends ChainLoadStep>
extends ExtensionBase
implements LoadStepFactory<T> {

	@Override
	public String id() {
		return ChainLoadStep.TYPE;
	}

	@Override @SuppressWarnings("unchecked")
	public T create(
		final Config baseConfig, final List<Extension> extensions,
		final List<Map<String, Object>> overrides
	) {
		return (T) new ChainLoadStep(baseConfig, extensions, overrides);
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
