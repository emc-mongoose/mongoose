package com.emc.mongoose.base.env;

import static com.emc.mongoose.base.Constants.APP_NAME;
import static com.emc.mongoose.base.Constants.USER_HOME;
import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;
import static com.emc.mongoose.base.config.CliArgUtil.ARG_PATH_SEP;

import com.emc.mongoose.base.config.BundledDefaultsProvider;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public final class CoreResourcesToInstall extends InstallableJarResources {

	private static final List<String> RES_INSTALL_FILES = Collections.unmodifiableList(
					Arrays.asList(
									// initial configuration/defaults file
									"config/defaults.yaml",
									// custom content example files
									"example/content/textexample",
									"example/content/zerobytes",
									// custom groovy scenario examples
									"example/scenario/groovy/types/additional/copy_load_using_env_vars.groovy",
									"example/scenario/groovy/types/additional/load_type.groovy",
									"example/scenario/groovy/types/additional/update_and_read_variants.groovy",
									"example/scenario/groovy/types/pipeline.groovy",
									"example/scenario/groovy/types/pipeline_with_delay_using_env_vars.groovy",
									"example/scenario/groovy/types/parallel_shell_commands.groovy",
									"example/scenario/groovy/types/weighted.groovy",
									"example/scenario/groovy/default.groovy",
									"example/scenario/groovy/rampup.groovy",
									// javascript scenario examples including the default one
									"example/scenario/js/backward_compatibility.js",
									"example/scenario/js/infinite_loop.js",
									"example/scenario/js/types/additional/copy_load_using_env_vars.js",
									"example/scenario/js/types/additional/load_type.js",
									"example/scenario/js/types/additional/update_and_read_variants.js",
									"example/scenario/js/types/pipeline.js",
									"example/scenario/js/types/pipeline_with_delay_using_env_vars.js",
									"example/scenario/js/types/parallel_shell_commands.js",
									"example/scenario/js/types/weighted.js",
									"example/scenario/js/rampup.js",
									// the default scenario which is invoked if no scenario is specified
									"example/scenario/js/default.js",
									// tests scenario files
									"example/scenario/js/endurance/infinite_loop_test.js",
									"example/scenario/js/endurance/parallel_pipeline_and_infinite_loop_test.js",
									"example/scenario/js/endurance/pipeline_test.js",
									"example/scenario/js/system/circular_append_test.js",
									"example/scenario/js/system/copy_using_input_path_test.js",
									"example/scenario/js/system/read_using_variable_path_test.js",
									"example/scenario/js/system/circular_read_limit_by_time_test.js",
									"example/scenario/js/system/multipart_create_test.js",
									"example/scenario/js/system/multiple_fixed_update_and_single_fixed_read_test.js",
									"example/scenario/js/system/multiple_random_update_and_multiple_fixed_read_test.js",
									"example/scenario/js/system/pipeline_with_delay_test.js",
									"example/scenario/js/system/read_custom_content_verification_fail_test.js",
									"example/scenario/js/system/read_verification_after_circular_update_test.js",
									"example/scenario/js/system/single_fixed_update_and_single_random_read_test.js",
									"example/scenario/js/system/single_random_update_and_multiple_random_read_test.js",
									"example/scenario/js/system/tls_read_using_input_file_test.js",
									"example/scenario/js/system/unlimited_concurrency_limit_by_rate_test.js",
									"example/scenario/js/system/weighted_load_test.js",
									// custom scenario examples in python
									"example/scenario/py/types/additional/copy_load_using_env_vars.py",
									"example/scenario/py/types/additional/load_type.py",
									"example/scenario/py/types/additional/update_and_read_variants.py",
									"example/scenario/py/types/pipeline.py",
									"example/scenario/py/types/pipeline_with_delay_using_env_vars.py",
									"example/scenario/py/types/parallel_shell_commands.py",
									"example/scenario/py/types/weighted.py",
									"example/scenario/py/default.py",
									"example/scenario/py/rampup.py",
									// provided extensions
									"ext/mongoose-load-step-linear.jar",
									"ext/mongoose-load-step-pipeline.jar",
									"ext/mongoose-load-step-weighted.jar",
									"ext/mongoose-storage-driver-coop.jar",
									"ext/mongoose-storage-driver-coop-netty.jar",
									"ext/mongoose-storage-driver-coop-netty-http.jar",
									"ext/mongoose-storage-driver-coop-netty-http-atmos.jar",
									"ext/mongoose-storage-driver-coop-netty-http-s3.jar",
									"ext/mongoose-storage-driver-coop-netty-http-swift.jar",
									"ext/mongoose-storage-driver-coop-nio.jar",
									"ext/mongoose-storage-driver-coop-nio-fs.jar",
									"ext/mongoose-storage-driver-preempt.jar"));
	private final Path appHomePath;

	public CoreResourcesToInstall() {
		final Config bundledDefaults;
		try {
			final var schema = SchemaProvider.resolveAndReduce(APP_NAME, Thread.currentThread().getContextClassLoader());
			bundledDefaults = new BundledDefaultsProvider().config(ARG_PATH_SEP, schema);
		} catch (final Exception e) {
			throwUncheckedIfInterrupted(e);
			throw new IllegalStateException("Failed to load the bundled default config from the resources", e);
		}
		final var appVersion = bundledDefaults.stringVal("run-version");
		final var msg = " " + APP_NAME + " v " + appVersion + " ";
		final var pad = StringUtils.repeat("#", (120 - msg.length()) / 2);
		System.out.println(pad + msg + pad);
		appHomePath = Paths.get(USER_HOME, "." + APP_NAME, appVersion);
	}

	public final Path appHomePath() {
		return appHomePath;
	}

	@Override
	protected final List<String> resourceFilesToInstall() {
		return RES_INSTALL_FILES;
	}
}
