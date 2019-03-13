package com.emc.mongoose.base;

import static com.emc.mongoose.base.Constants.APP_NAME;
import static com.emc.mongoose.base.Constants.DIR_EXT;
import static com.emc.mongoose.base.Constants.MIB;
import static com.emc.mongoose.base.Constants.PATH_DEFAULTS;
import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;
import static com.emc.mongoose.base.config.CliArgUtil.allCliArgs;

import com.emc.mongoose.base.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.base.config.AliasingUtil;
import com.emc.mongoose.base.config.CliArgUtil;
import com.emc.mongoose.base.config.ConfigUtil;
import com.emc.mongoose.base.config.IllegalArgumentNameException;
import com.emc.mongoose.base.control.AddCorsHeadersRule;
import com.emc.mongoose.base.control.ConfigServlet;
import com.emc.mongoose.base.control.logs.LogServlet;
import com.emc.mongoose.base.control.run.RunImpl;
import com.emc.mongoose.base.control.run.RunServlet;
import com.emc.mongoose.base.env.CoreResourcesToInstall;
import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.base.load.step.ScenarioUtil;
import com.emc.mongoose.base.load.step.service.LoadStepManagerServiceImpl;
import com.emc.mongoose.base.load.step.service.file.FileManagerServiceImpl;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.metrics.MetricsManager;
import com.emc.mongoose.base.metrics.MetricsManagerImpl;
import com.emc.mongoose.base.svc.Service;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.exceptions.InvalidValuePathException;
import com.github.akurilov.confuse.exceptions.InvalidValueTypeException;
import io.prometheus.client.exporter.MetricsServlet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.script.ScriptEngine;
import javax.servlet.MultipartConfigElement;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public final class Main {

	public static void main(final String... args) {

		final var coreResources = new CoreResourcesToInstall();
		final var appHomePath = coreResources.appHomePath();
		final var initialStepId = "none-" + LogUtil.getDateTimeStamp();
		LogUtil.init(appHomePath.toString(), initialStepId);
		try {
			// install the core resources
			coreResources.install(appHomePath);
			// load the defaults
			final var defaultConfig = loadDefaultConfig(appHomePath);
			// extensions
			try (final var extClsLoader = Extension.extClassLoader(Paths.get(appHomePath.toString(), DIR_EXT).toFile())) {
				final var extensions = Extension.load(extClsLoader);
				// install the extensions
				installExtensions(extensions, appHomePath);
				final Config configWithArgs;
				try {
					// apply the extensions defaults
					final var fullDefaultConfig = collectDefaults(extensions, defaultConfig, appHomePath);
					// parse the CLI args and apply them to the config instance
					configWithArgs = applyArgsToConfig(args, fullDefaultConfig, initialStepId);
				} catch (final Exception e) {
					throwUncheckedIfInterrupted(e);
					LogUtil.exception(Level.ERROR, e, "Failed to load the defaults");
					throw e;
				}
				// init the metrics manager
				final MetricsManager metricsMgr = new MetricsManagerImpl(ServiceTaskExecutor.INSTANCE);
				// go on
				if (configWithArgs.boolVal("run-node")) {
					runNode(configWithArgs, extClsLoader, extensions, metricsMgr, appHomePath);
				} else {
					runScenario(configWithArgs, extensions, extClsLoader, metricsMgr, appHomePath);
				}
			}
		} catch (final InterruptedException e) {
			Loggers.MSG.debug("Interrupted", e);
		} catch (final Exception e) {
			LogUtil.trace(Loggers.ERR, Level.FATAL, e, "Unexpected failure");
		}
	}

	private static Config loadDefaultConfig(final Path appHomePath) throws Exception {
		final var mainConfigSchema = SchemaProvider.resolve(APP_NAME, Thread.currentThread().getContextClassLoader()).stream()
						.findFirst()
						.orElseThrow(IllegalStateException::new);
		// load the defaults
		return ConfigUtil.loadConfig(
						Paths.get(appHomePath.toString(), PATH_DEFAULTS).toFile(), mainConfigSchema);
	}

	private static void installExtensions(final List<Extension> extensions, final Path appHomePath) {
		final var availExtMsg = new StringBuilder("Available/installed extensions:\n");
		extensions.forEach(
						ext -> {
							ext.install(appHomePath);
							final var extId = ext.id();
							final var extFqcn = ext.getClass().getCanonicalName();
							availExtMsg
											.append('\t')
											.append(extId)
											.append(' ')
											.append(StringUtils.repeat("-", extId.length() < 30 ? 30 - extId.length() : 1))
											.append("> ")
											.append(extFqcn)
											.append('\n');
						});
		Loggers.MSG.info(availExtMsg);
	}

	private static Config collectDefaults(
					final List<Extension> extensions, final Config mainDefaults, final Path appHomePath)
					throws Exception {
		final List<Config> allDefaults = extensions.stream()
						.map(ext -> ext.defaults(appHomePath))
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
		allDefaults.add(mainDefaults);
		return ConfigUtil.merge(mainDefaults.pathSep(), allDefaults);
	}

	private static Config applyArgsToConfig(
					final String[] args, final Config config, final String initialStepId) {
		try {
			argsWithAliases(args, config).forEach(config::val);
		} catch (final IllegalArgumentNameException e) {
			final var formattedAllCliArgs = allCliArgs(config.schema(), config.pathSep()).stream()
							.collect(Collectors.joining("\n", "\t", ""));
			Loggers.ERR.fatal(
							"Invalid argument: \"{}\"\nThe list of all possible args:\n{}",
							e.getMessage(),
							formattedAllCliArgs);
		} catch (final InvalidValuePathException e) {
			Loggers.ERR.fatal("Invalid configuration option: \"{}\"", e.path());
		} catch (final InvalidValueTypeException e) {
			Loggers.ERR.fatal(
							"Invalid configuration value type for the option \"{}\", expected: {}, " + "actual: {}",
							e.path(),
							e.expectedType(),
							e.actualType());
		}
		checkAndSetStepId(config, initialStepId);
		Arrays.stream(args).forEach(Loggers.CLI::info);
		return config;
	}

	private static void checkAndSetStepId(final Config config, final String initialStepId) {
		if (null == config.val("load-step-id")) {
			config.val("load-step-id", initialStepId);
			config.val("load-step-idAutoGenerated", true);
		}
	}

	private static Map<String, String> argsWithAliases(final String[] args, final Config config) {
		final var parsedArgs = CliArgUtil.parseArgs(args);
		final List<Map<String, Object>> aliasingConfig = config.listVal("aliasing");
		return AliasingUtil.apply(parsedArgs, aliasingConfig);
	}

	private static void runNode(
					final Config fullDefaultConfig,
					final ClassLoader extClsLoader,
					final List<Extension> extensions,
					final MetricsManager metricsMgr,
					final Path appHomePath)
					throws Exception {

		// init the API server
		final var port = fullDefaultConfig.intVal("run-port");
		final var server = new Server(port);
		final var context = new ServletContextHandler();
		context.setContextPath("/");
		server.setHandler(context);
		final var addCorsHeaderHandler = new RewriteHandler();
		addCorsHeaderHandler.addRule(new AddCorsHeadersRule());
		server.insertHandler(addCorsHeaderHandler);
		context.addServlet(new ServletHolder(new ConfigServlet(fullDefaultConfig)), "/config/*");
		context.addServlet(new ServletHolder(new LogServlet()), "/logs/*");
		context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
		final var runServletHolder = new ServletHolder(
						new RunServlet(extClsLoader, extensions, metricsMgr, fullDefaultConfig, appHomePath));
		runServletHolder
						.getRegistration()
						.setMultipartConfig(new MultipartConfigElement("", 16 * MIB, 16 * MIB, 16 * MIB));
		context.addServlet(runServletHolder, "/run/*");
		try {
			server.start();
			Loggers.MSG.info("Started to serve the remote API @ port # " + port);
			final var listenPort = fullDefaultConfig.intVal("load-step-node-port");
			try (final Service fileMgrSvc = new FileManagerServiceImpl(listenPort);
							final Service scenarioStepSvc = new LoadStepManagerServiceImpl(listenPort, extensions, metricsMgr)) {
				fileMgrSvc.start();
				scenarioStepSvc.start();
				scenarioStepSvc.await();
			} catch (final InterruptedException e) {
				throw e;
			} catch (final Throwable cause) {
				LogUtil.trace(Loggers.ERR, Level.FATAL, cause, "Run node failure");
			}
		} finally {
			server.stop();
		}
	}

	private static void runScenario(
					final Config config,
					final List<Extension> extensions,
					final ClassLoader extClsLoader,
					final MetricsManager metricsMgr,
					final Path appHomePath) {
		Path scenarioPath = null;
		final var scenarioFile = config.stringVal("run-scenario");
		if (scenarioFile != null && !scenarioFile.isEmpty()) {
			scenarioPath = Paths.get(scenarioFile);
		}
		runScenarioFile(config, extensions, extClsLoader, metricsMgr, scenarioPath, appHomePath);
	}

	private static void runScenarioFile(
					final Config config,
					final List<Extension> extensions,
					final ClassLoader extClsLoader,
					final MetricsManager metricsMgr,
					final Path scenarioPath,
					final Path appHomePath) {
		final ScriptEngine scriptEngine;
		final String scenarioText;
		if (scenarioPath == null) {
			scriptEngine = ScenarioUtil.scriptEngineByDefault(extClsLoader);
			scenarioText = ScenarioUtil.defaultScenario(appHomePath);
		} else {
			scriptEngine = ScenarioUtil.scriptEngineByFilePath(scenarioPath, extClsLoader);
			final var strb = new StringBuilder();
			try {
				Files.lines(scenarioPath).forEach(line -> strb.append(line).append(System.lineSeparator()));
			} catch (final IOException e) {
				LogUtil.exception(Level.FATAL, e, "Failed to read the scenario file \"{}\"", scenarioPath);
				try {
					Files.list(scenarioPath.getParent()).forEach(System.out::println);
				} catch (final IOException ee) {
					LogUtil.trace(
									Loggers.ERR, Level.ERROR, ee, "Failed to list the scenarios parent directory");
				}
			}
			scenarioText = strb.toString();
		}
		if (scriptEngine == null) {
			Loggers.ERR.fatal("Failed to resolve the scenario engine for the file \"{}\"", scenarioPath);
		} else {
			Loggers.MSG.info(
							"Using the \"{}\" scenario engine", scriptEngine.getFactory().getEngineName());
			// expose the environment values
			System.getenv().forEach(scriptEngine::put);
			// expose the loaded configuration and the step types
			ScenarioUtil.configure(scriptEngine, extensions, config, metricsMgr);
			// go
			new RunImpl("", scenarioText, scriptEngine).run();
		}
	}
}
