package com.emc.mongoose.core.impl.util.log;
//
import com.emc.mongoose.core.impl.util.RunTimeConfig;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Booleans;
//
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
/**
 Created by andrey on 13.03.15.
 */
@Plugin(name = "RunIdFile", category = "Core", elementType = "appender", printObject = true)
public final class RunIdFileAppender
extends AbstractOutputStreamAppender<RunIdFileManager> {
	private final int bufferSize;
	{
		long longBuffSize = RunTimeConfig.getContext().getDataPageSize();
		bufferSize = longBuffSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) longBuffSize;
	}
	private final String fileNamePrefix;
	private final Advertiser advertiser;
	private Object advertisement;
	/**
	 Instantiate a WriterAppender and set the output destination to a
	 new {@link java.io.OutputStreamWriter} initialized with <code>os</code>
	 as its {@link java.io.OutputStream}.
	 @param name The name of the Appender.
	 @param layout The layout to format the message.
	 @param filter filter
	 @param ignoreExceptions ignore exceptions
	 @param manager The OutputStreamManager. */
	protected RunIdFileAppender(
		final String name,
		final Layout<? extends Serializable> layout,
		final Filter filter,
		final boolean ignoreExceptions,
		final RunIdFileManager manager,
		final String fileNamePrefix,
		final Advertiser advertiser
	) {
		super(name, layout, filter, ignoreExceptions, false, manager);
		if (advertiser != null) {
			final Map<String, String> configuration = new HashMap<>(layout.getContentFormat());
			configuration.putAll(manager.getContentFormat());
			configuration.put("contentType", layout.getContentType());
			configuration.put("name", name);
			advertisement = advertiser.advertise(configuration);
		}
		this.fileNamePrefix = fileNamePrefix;
		this.advertiser = advertiser;
	}
	//
	@Override
	public void stop() {
		super.stop();
		if (advertiser != null) {
			advertiser.unadvertise(advertisement);
		}
	}
	//
	public final String getFileNamePrefix() {
		return fileNamePrefix;
	}
	//
	@PluginFactory
	public static RunIdFileAppender createAppender(
		@PluginAttribute("fileNamePrefix") final String fileNamePrefix,
		@PluginAttribute("locking") final String locking,
		@PluginAttribute("name") final String name,
		@PluginElement("Layout") Layout<? extends Serializable> layout,
		@PluginElement("Filter") final Filter filter,
		@PluginAttribute("ignoreExceptions") final String ignore,
		@PluginAttribute("advertise") final String advertise,
		@PluginAttribute("advertiseUri") final String advertiseUri,
		@PluginConfiguration final Configuration config
	) {
		final boolean
			ignoreExceptions = Booleans.parseBoolean(ignore, true),
			isAdvertise = Boolean.parseBoolean(advertise);
		if(layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}
		if(config == null) {
			throw new IllegalArgumentException("Null config");
		}
		return new RunIdFileAppender(
			name, layout, filter, ignoreExceptions, manager, fileNamePrefix,
			isAdvertise ? config.getAdvertiser() : null
		);
	}
}
