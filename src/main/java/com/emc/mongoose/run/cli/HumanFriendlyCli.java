package com.emc.mongoose.run.cli;
// mongoose-common.jar

import com.emc.mongoose.common.conf.AppConfig;
//
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
//
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 Date:   12/10/14
 Time:   4:37 PM
 */
public final class HumanFriendlyCli {

	private enum CliOption {
		IP(
			"i", "Comma-separated list of ip addresses to write to", true,
			AppConfig.KEY_STORAGE_ADDRS
		),
		USER("u", "User", true, AppConfig.KEY_AUTH_ID),
		SECRET("s", "Secret", true, AppConfig.KEY_AUTH_SECRET),
		BUCKET("b", "Bucket to write data to", true, AppConfig.KEY_ITEM_DST_CONTAINER),
		READ("r", "Perform object read", true,
			new CompositeOptionConverter(AppConfig.KEY_LOAD_TYPE, "read",
				AppConfig.KEY_ITEM_SRC_FILE
			)
		),
		WRITE(
			"w", "Perform object write", false,
			new CompositeOptionConverter(AppConfig.KEY_LOAD_TYPE, "create")
		),
		DELETE("d", "Perform object delete", false,
			new CompositeOptionConverter(AppConfig.KEY_LOAD_TYPE, "delete",
				AppConfig.KEY_ITEM_SRC_FILE
			)
		),
		LENGTH("l", "Size of the object to write", true, AppConfig.KEY_ITEM_DATA_SIZE),
		COUNT("c", "Count of objects to write", true, AppConfig.KEY_LOAD_LIMIT_COUNT),
		CONNS(
			"n", "Number of concurrent connections per storage node", true,
			AppConfig.KEY_LOAD_THREADS
		),
		HELP("h", "Displays this message", false, new NullOptionConverter()),
		RUN_ID("z", "Sets run id", true, new SystemOptionConverter(AppConfig.KEY_RUN_ID)),
		USE_DEPLOYMENT_OUTPUT("o", "Use deployment output", false, new DeploymentOutputConverter()),
		FILE("f", "Json scenario input file", true, AppConfig.KEY_RUN_FILE),
		INPUT(
			"I", "Read the scenario from standard input", false,
			new CompositeOptionConverter(AppConfig.KEY_SCENARIO_FROM_STDIN, "true")
		);

		private final String shortName;
		private final String description;
		private final boolean hasArg;
		private final OptionConverter optionConverter;

		CliOption(
			final String shortName, final String description, final boolean hasArg,
			final OptionConverter converter
		) {
			this.shortName = shortName;
			this.description = description;
			this.hasArg = hasArg;
			this.optionConverter = converter;
		}

		CliOption(
			final String shortName, final String description, final boolean hasArg,
			final String... commonPropertyName
		) {
			this.shortName = shortName;
			this.description = description;
			this.hasArg = hasArg;
			this.optionConverter = new DefaultOptionConverter(commonPropertyName);
		}

		public OptionConverter converter() {
			return optionConverter;
		}

		public final Option toOption() {
			return new Option(
				shortName, this.name().toLowerCase().replace('_', '-'), hasArg, description
			);
		}

		public static CliOption fromOption(final Option option) {
			return CliOption.valueOf(option.getLongOpt().toUpperCase().replace('-', '_'));
		}
	}

	public static void main(final String[] args) {
		System.out.println(parseCli(args));
	}

	public static Map<String, String> parseCli(final String[] args) {
		final Options options = new Options();
		for(final CliOption opt : CliOption.values()) {
			options.addOption(opt.toOption());
		}
		try {
			final CommandLineParser commandLineParser = new GnuParser();
			final CommandLine cmdLine = commandLineParser.parse(options, args);
			if(cmdLine.hasOption(CliOption.HELP.toString().toLowerCase())) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Mongoose", options);
				System.exit(0);
			}
			final Map<String, String> values = new HashMap<>();
			for(final Option option : cmdLine.getOptions()) {
				values.putAll(
					CliOption.fromOption(option).converter().convertOption(option.getValue())
				);
			}
			return values;
		} catch(final ParseException e) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Mongoose", options);
			System.exit(1);
		} catch(final Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
		return null;
	}

	private interface OptionConverter {

		Map<String, String> convertOption(String value)
		throws Exception;
	}

	private static class DefaultOptionConverter
	implements OptionConverter {

		private final String[] commonPropertyNames;

		public DefaultOptionConverter(final String... commonPropertyNames) {
			this.commonPropertyNames = commonPropertyNames;
		}

		@Override
		public final Map<String, String> convertOption(final String value) {
			final Map<String, String> result = new HashMap<>();
			for(final String property : commonPropertyNames) {
				result.put(property, value);
			}
			return result;
		}
	}

	private static class CompositeOptionConverter
	implements OptionConverter {

		private final String key;
		private final String staticValue;
		private final String[] configuredValueKeys;

		public CompositeOptionConverter(
			final String key, final String staticValue, final String... configuredValueKeys
		) {
			this.key = key;
			this.staticValue = staticValue;
			this.configuredValueKeys = configuredValueKeys;
		}

		@Override
		public final Map<String, String> convertOption(final String value)
		throws Exception {
			final Map<String, String> result = new HashMap<>();
			result.put(key, staticValue);
			for(final String configuredKey : configuredValueKeys) {
				result.put(configuredKey, value);
			}
			return result;
		}
	}

	private static class DeploymentOutputConverter
	implements OptionConverter {

		@Override
		public Map<String, String> convertOption(final String value)
		throws Exception {
			final String fileName = System.getenv("DevBranch");
			File file = new File(fileName + "/tools/cli/python/DeploymentOutput");
			if(!file.exists()) {
				file = new File(fileName + "/tools/cli/python/StandaloneDeploymentOutput");
			}
			final Properties props = new Properties();
			try(final FileInputStream stream = new FileInputStream(file)) {
				props.load(stream);
			}
			final Map<String, String> result = new HashMap<>();
			result.put(AppConfig.KEY_AUTH_ID, props.getProperty("user"));
			result.put(AppConfig.KEY_AUTH_SECRET, props.getProperty("secretkey"));
			result.put(AppConfig.KEY_ITEM_DST_CONTAINER, props.getProperty("bucket").split(" ")[0]);
			final String dataNodes =
				System.getenv("DataNodes").replace('(', ' ').replace(')', ' ').trim().replace(' ',
					','
				);
			final String s3Ports = props.getProperty("s3UnSecurePort");
			if(s3Ports != null) {
				//Looks like we are working with StandaloneDeploymentOutput with custom port config
				final String firstDataNode = dataNodes.split(",")[0];
				final List<String> address = new ArrayList<>();
				for(final String port : s3Ports.split(",")) {
					address.add(firstDataNode + ":" + port);
				}
				result.put(AppConfig.KEY_STORAGE_ADDRS, StringUtils.join(address, ','));
			} else {
				result.put(AppConfig.KEY_STORAGE_ADDRS, dataNodes);
			}
			return result;
		}
	}

	private static class NullOptionConverter
	implements OptionConverter {

		@Override
		public final Map<String, String> convertOption(final String value)
		throws Exception {
			return Collections.emptyMap();
		}
	}

	private static class SystemOptionConverter
	implements OptionConverter {

		private final String systemPropertyKey;

		public SystemOptionConverter(final String systemProperty) {
			this.systemPropertyKey = systemProperty;
		}

		@Override
		public final Map<String, String> convertOption(final String value)
		throws Exception {
			System.setProperty(systemPropertyKey, value);
			return Collections.singletonMap(systemPropertyKey, value);
		}
	}
}
