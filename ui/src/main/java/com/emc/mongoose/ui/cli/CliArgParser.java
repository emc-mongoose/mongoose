package com.emc.mongoose.ui.cli;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Loggers;
import org.apache.commons.lang.WordUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

/**
 Created by kurila on 16.08.16.
 */
public final class CliArgParser {
	
	public static final String ARG_PREFIX = "--";
	public static final String CONFIG_CLS_SUFFIX = "Config";
	public static final String FIELD_PREFIX = "KEY_";
	
	private CliArgParser() {
	}
	
	public static Map<String, Object> parseArgs(
		final List<Map<String, Object>> aliasingConfig, final String... args
	) throws IllegalArgumentException {

		final Map<String, Object> tree = new TreeMap<>();

		String argValPair[];
		String aliasArgValPair[];
		String nextAliasName;
		String nextAliasTarget;
		boolean nextDeprecatedFlag;

		for(final String arg : args) {

			argValPair = arg.split("=", 2);

			if(aliasingConfig != null) {
				for(final var aliasingNode : aliasingConfig) {
					nextAliasName = ARG_PREFIX + aliasingNode.get(Config.NAME);
					nextAliasTarget = (String) aliasingNode.get(Config.TARGET);
					nextAliasTarget = nextAliasTarget == null ? null : ARG_PREFIX + nextAliasTarget;
					nextDeprecatedFlag = aliasingNode.containsKey(Config.DEPRECATED) &&
						(boolean) aliasingNode.get(Config.DEPRECATED);
					if(arg.startsWith(nextAliasName)) {
						if(nextAliasTarget == null) {
							System.err.println(
								"The argument \"" + nextAliasName + "\" is deprecated"
							);
							break;
						} else if(nextDeprecatedFlag) {
							Loggers.ERR.warn(
								"The argument \"" + nextAliasName + "\" is deprecated, use \""
									+ nextAliasTarget + "\" instead"
							);
						}
						aliasArgValPair = nextAliasTarget.split("=", 2);
						argValPair[0] = aliasArgValPair[0];
						if(aliasArgValPair.length == 2) {
							if(argValPair.length == 2) {
								argValPair[1] = aliasArgValPair[1];
							} else {
								argValPair = new String[] { argValPair[0], aliasArgValPair[1] };
							}
						}
						break;
					}
				}
			}

			if(argValPair.length > 1) {
				parseArg(tree, argValPair[0], argValPair[1]);
			} else {
				parseArg(tree, argValPair[0]);
			}
		}

		return tree;
	}
	
	private static void parseArg(
		final Map<String, Object> tree, final String arg, final String value
	) throws IllegalArgumentException {
		if(arg.startsWith(ARG_PREFIX) && arg.length() > ARG_PREFIX.length()) {
			final String argParts[] = arg.substring(ARG_PREFIX.length()).split(Config.PATH_SEP);
			var subTree = tree;
			String argPart;
			for(var i = 0; i < argParts.length; i ++) {
				argPart = argParts[i];
				if(i < argParts.length - 1) {
					final var node = subTree.computeIfAbsent(argPart, k -> new TreeMap<>());
					subTree = (Map<String, Object>) node;
				} else { // last part
					subTree.put(argPart, value);
				}
			}
		} else {
			throw new IllegalArgumentException(arg);
		}
	}
	
	private static void parseArg(final Map<String, Object> tree, final String arg) {
		parseArg(tree, arg, Boolean.TRUE.toString());
	}
	
	public static String formatCliArgsList(final Map<String, Class> argsWithTypes) {
		final var strb = new StringBuilder();
		for(final var arg : argsWithTypes.keySet()) {
			strb
				.append('\t')
				.append(arg)
				.append("=<")
				.append(argsWithTypes.get(arg).getSimpleName())
				.append(">\n");
		}
		return strb.toString();
	}
	
	public static Map<String, Class> getAllCliArgs()
	throws ReflectiveOperationException {

		final Map<String, Class> argsWithTypes = new TreeMap<>();
		final var configRootPkg = Config.class.getPackage();
		final var configRootPkgName = configRootPkg.getName();
		final var allPkgs = Package.getPackages();

		for(final var subPkg : allPkgs) {
			final var subPkgName = subPkg.getName();
			if(subPkgName.startsWith(configRootPkgName)) {

				final var configBranchPrefix = subPkgName
					.substring(configRootPkgName.length())
					.replaceAll(Pattern.quote("."), Config.PATH_SEP);
				final String configBranchClsNamePrefix;
				if(subPkg.equals(configRootPkg)) {
					configBranchClsNamePrefix = "";
				} else {
					configBranchClsNamePrefix = WordUtils.capitalize(
						subPkgName.substring(subPkgName.lastIndexOf('.') + 1)
					);
				}

				try {

					final var configBranchCls = (Class<Serializable>) Class.forName(
						subPkgName + '.' + configBranchClsNamePrefix + CONFIG_CLS_SUFFIX
					);

					final var fields = configBranchCls.getFields();
					for(final var field : fields) {
						if(field.getType().equals(String.class)) {
							final var fieldName = field.getName();
							if(fieldName.startsWith(FIELD_PREFIX)) {

								final var rawArgName = fieldName
									.substring(FIELD_PREFIX.length())
									.toLowerCase();

								try {
									final var argNameParts = rawArgName.split("_");
									final var argNameBuilder = new StringBuilder();
									for(final var argNamePart : argNameParts) {
										argNameBuilder
											.append(toUpperCase(argNamePart.charAt(0)))
											.append(argNamePart.substring(1));
									}
									final var m = configBranchCls.getMethod(
										"get" + argNameBuilder.toString()
									);
									final Class type = m.getReturnType();
									final var argName = toLowerCase(argNameBuilder.charAt(0)) +
											argNameBuilder.substring(1);
									argsWithTypes.put(
										ARG_PREFIX + configBranchPrefix + Config.PATH_SEP + argName,
										type
									);
								} catch(final Exception ignored) {
								}
							}
						}
					}
				} catch(final ClassNotFoundException ignored) {
				}
			}
		}

		return argsWithTypes;
	}
}
