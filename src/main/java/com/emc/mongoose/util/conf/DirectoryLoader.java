package com.emc.mongoose.util.conf;
//

import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
//
/**
 Created by kurila on 04.07.14.
 A property loader using some directory as a root of property tree.
 */
public final class DirectoryLoader
extends SimpleFileVisitor<Path> {
	//
	private final static Set<String> mongooseKeys = new HashSet<>();
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private LinkedList<String> prefixTokens = new LinkedList<>();
	private final RunTimeConfig tgtConfig;
	//
	private boolean isUpload;
	private boolean isUpdate;

	public DirectoryLoader(final RunTimeConfig tgtConfig) {
		this.tgtConfig = tgtConfig;
	}
	//
	public static void loadPropsFromDir(final Path rootDir, final RunTimeConfig tgtConfig) {
		final DirectoryLoader dirLoader = new DirectoryLoader(tgtConfig);
		try {
			LOG.debug(Markers.MSG, "Load system properties from directory \"{}\"", rootDir);
			Files.walkFileTree(rootDir, dirLoader);
			tgtConfig.setMongooseKeys(mongooseKeys);
		} catch (final IOException e) {
			LOG.error(Markers.ERR, e.toString(), e.getCause());
		}
	}
	//
	public static void updatePropertiesFromDir(final Path rootDir, final RunTimeConfig tgtConfig) {
		updatePropertiesFromDir(rootDir, tgtConfig, false);
	}
	//
	public static void updatePropertiesFromDir(final Path rootDir, final RunTimeConfig tgtConfig, final boolean isUpload) {
		final DirectoryLoader dirLoader = new DirectoryLoader(tgtConfig);
		dirLoader.isUpload = isUpload;
		dirLoader.isUpdate = true;
		try {
			Files.walkFileTree(rootDir, dirLoader);
		} catch (final IOException e) {
			LOG.error(Markers.ERR, e.toString(), e.getCause());
		}
	}
	//
	@Override
	public final FileVisitResult visitFile(
		final Path file, final BasicFileAttributes attrs
	) throws IOException {
		// get prefix
		final StrBuilder currPrefixBuilder = new StrBuilder();
		for(final String nextToken: prefixTokens) {
			if(!nextToken.equals(Main.DIR_PROPERTIES)) {
				currPrefixBuilder.append(nextToken).append(Main.DOT);
			}
		}
		final String currPrefix = currPrefixBuilder
			.append(file.getName(file.getNameCount() - 1)).append(Main.DOT).toString();
		// get the properties
		PropertiesConfiguration currProps = null;
		try {
			currProps = new PropertiesConfiguration(file.toFile());
			LOG.trace(Markers.MSG, "Loaded the properties {} from file {}", currProps, file);
		} catch(final ConfigurationException e) {
			ExceptionHandler.trace(
				LOG, Level.ERROR, e,
				String.format("Failed to load the properties from file \"%s\"", file.toString())
			);
		}
		// set the properties
		final List<DefaultMapEntry<String, Object>> props = new ArrayList<>();
		if(currProps!=null) {
			String key;
			//
			if (isUpdate) {
				if (file.getFileName().toString().equals("run")) {
					props.add(new DefaultMapEntry<String, Object>(Main.KEY_RUN_ID,
						new DefaultMapEntry<>("id", tgtConfig.getProperty(Main.KEY_RUN_ID))));
				}
				for (final Iterator<String> keyIter = currProps.getKeys(); keyIter.hasNext(); ) {
					key = keyIter.next();
					final String fullKeyName = currPrefix + key;
					currProps.setProperty(key, tgtConfig.getProperty(fullKeyName));
					props.add(new DefaultMapEntry<String, Object>(fullKeyName,
						new DefaultMapEntry<>(key, tgtConfig.getProperty(fullKeyName))));
				}
				if (isUpload) {
					try (final FileWriter writer = new FileWriter(file.toFile())) {
						currProps.save(writer);
					} catch (final Exception e) {
						ExceptionHandler.trace(LOG, Level.ERROR, e,
							String.format("Failed to write in property file \"%s\"", file.getFileName()));
					}
				}
			} else {
				if (file.getFileName().toString().equals("run")) {
					props.add(new DefaultMapEntry<String, Object>(Main.KEY_RUN_ID,
						new DefaultMapEntry<>("id", currProps.getProperty("id"))));
				}
				//
				for (final Iterator<String> keyIter = currProps.getKeys(); keyIter.hasNext(); ) {
					key = keyIter.next();
					LOG.trace(
						Markers.MSG, "File property: \"{}\" = \"{}\"",
						currPrefix + key, currProps.getProperty(key)
					);
					final String fullKeyName = currPrefix + key;
					mongooseKeys.add(fullKeyName);
					//
					props.add(new DefaultMapEntry<String, Object>(fullKeyName,
						new DefaultMapEntry<>(key, currProps.getProperty(key))));
					tgtConfig.setProperty(currPrefix + key, currProps.getProperty(key));
				}
			}
			tgtConfig.put(prefixTokens, file.getFileName().toString(), props);
		}
		//
		return FileVisitResult.CONTINUE;
	}
	//
	@Override
	public final FileVisitResult visitFileFailed(
		final Path file, final IOException e
	) throws IOException {
		LOG.warn(Markers.ERR, e.toString(), e.getCause());
		return FileVisitResult.CONTINUE;
	}
	//
	@Override
	public final FileVisitResult preVisitDirectory(
		final Path dir, final BasicFileAttributes attrs
	) throws IOException {
		final String currDirName = dir.getName(dir.getNameCount()-1).toString();
		LOG.trace(Markers.MSG, "{}: enter directory", currDirName);
		prefixTokens.addLast(currDirName);
		return FileVisitResult.CONTINUE;
	}
	//
	@Override
	public final FileVisitResult postVisitDirectory(
		final Path dir, final IOException e
	) throws IOException {
		if (e == null) {
			LOG.trace(Markers.MSG, "{}: exit directory", prefixTokens.removeLast());
			return FileVisitResult.CONTINUE;
		} else {
			// directory iteration failed
			throw e;
		}
	}
}
