package com.emc.mongoose.base.load.step.client;

import com.emc.mongoose.base.load.step.LoadStep;
import java.util.Map;

public interface LoadStepClient extends LoadStep {

	int OUTPUT_PROGRESS_PERIOD_MILLIS = 10_000;

	/**
	 * Configure the step
	 *
	 * @param config a dictionary of the configuration values to override the inherited config
	 * @return <b>new/copied</b> step instance with the applied config values
	 * @throws IllegalStateException if was called after any append(...) call
	 */
	<T extends LoadStepClient> T config(final Map<String, Object> config)
					throws IllegalStateException;

	/**
	 * Append the load step context. The actual behavior depends on the particular step type
	 *
	 * @param context a dictionary of the additional parameters handled by the particular load step
	 *     implementation
	 * @return <b>new/copied</b> step instance with the appended context
	 */
	<T extends LoadStepClient> T append(final Map<String, Object> context);
}
