package com.emc.mongoose.ui.cli;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Loggers;
import org.apache.commons.lang.WordUtils;

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
				for(final Map<String, Object> aliasingNode : aliasingConfig) {
					nextAliasName = ARG_PREFIX + aliasingNode.get(Config.NAME);
					nextAliasTarget = (String) aliasingNode.get(Config.TARGET);
					nextAliasTarget = nextAliasTarget == null ? null : ARG_PREFIX + nextAliasTarget;
					nextDeprecatedFlag = aliasingNode.containsKey(Config.DEPRECATED) &&
						(boolean) aliasingNode.get(Config.DEPRECATED);
					if(arg.startsWith(nextAliasName)) {
						if(nextAliasTarget == null) {
							Loggers.ERR.fatal("The argument \"{}\" is deprecated", nextAliasName);
						} else if(nextDeprecatedFlag) {
							Loggers.ERR.warn(
								"The argument \"{}\" is deprecated, use \"{}\" instead",
								nextAliasName, nextAliasTarget
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
			Map<String, Object> subTree = tree;
			String argPart;
			for(int i = 0; i < argParts.length; i ++) {
				argPart = argParts[i];
				if(i < argParts.length - 1) {
					Object node = subTree.get(argPart);
					if(node == null) {
						node = new TreeMap<>();
						subTree.put(argPart, node);
					}
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
		final StringBuilder strb = new StringBuilder();
		for(final String arg : argsWithTypes.keySet()) {
			strb
				.append('\t')
				.append(arg)
				.append("=<")
				.append(argsWithTypes.get(arg).getSimpleName())
				.append(">\n");
		}
		return strb.toString();
	}
	
	public static Map<String, Class> getAllCliArgs(final Config config)
	throws ReflectiveOperationException {
		final Map<String, Class> argsWithTypes = new TreeMap<>();
		dumpCliArgsRecursively(argsWithTypes);
		return argsWithTypes;
	}
	
	private static void dumpCliArgsRecursively(final Map<String, Class> argsWithTypes)
	throws ReflectiveOperationException {

		final String configPkgName = Config.class.getPackage().getName() + ".";
		final Package[] allPkgs = Package.getPackages();

		for(final Package subPkg : allPkgs) {
			final String subPkgName = subPkg.getName();
			if(subPkgName.startsWith(configPkgName)) {

				final String configBranchPrefix = (
					ARG_PREFIX + subPkgName.substring(configPkgName.length())
				).replace(Pattern.quote("."), Config.PATH_SEP) + Config.PATH_SEP;
				final String configBranchClsNamePrefix = WordUtils.capitalize(
					subPkgName.substring(subPkgName.lastIndexOf(".") + 1)
				);

				boolean configLeaf = true;
				for(final Package otherPkg : allPkgs) {
					if(!subPkg.equals(otherPkg)) {
						final String otherPkgName = otherPkg.getName();
						if(subPkgName.startsWith(otherPkgName)) {
							configLeaf = false;
							break;
						}
					}
				}

				try {

					final Class configBranchCls = Class.forName(
						configBranchClsNamePrefix + CONFIG_CLS_SUFFIX
					);

					final Field[] fields = configBranchCls.getFields();
					for(final Field field : fields) {
						if(field.getType().equals(String.class)) {
							final String fieldName = field.getName();
							if(fieldName.startsWith(FIELD_PREFIX)) {

								final String rawArgName = fieldName
									.substring(FIELD_PREFIX.length())
									.toLowerCase();

								if(configLeaf) {
									try {
										final String[] argNameParts = rawArgName.split("_");
										final StringBuilder argNameBuilder = new StringBuilder();
										for(final String argNamePart : argNameParts) {
											argNameBuilder
												.append(toUpperCase(argNamePart.charAt(0)))
												.append(argNamePart.substring(1));
										}
										final Method m = configBranchCls.getMethod(
											"get" + argNameBuilder.toString()
										);
										final Class type = m.getReturnType();
										final String argName =
											toLowerCase(argNameBuilder.charAt(0)) +
												argNameBuilder.substring(1);
										argsWithTypes.put(
											configBranchClsNamePrefix + argName, type
										);
									} catch(final Exception ignored) {
									}
								}
							}
						}
					}
				} catch(final ClassNotFoundException ignored) {
				}
			}
		}
	}
}
