package com.emc.mongoose.util.persist;
//
import com.emc.mongoose.util.threading.WorkerFactory;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.status.StatusLogger;
//
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
//
/**
 * Created by olga on 09.12.14.
 */
@Plugin(name = "OpenJpaAsync", category = "Core", elementType = "appender", printObject = true)
public final class OpenJpaAsyncAppender
extends AbstractAppender {

	private static final String POISON = "POISON";
	public static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();
	//
	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_QUEUE_SIZE = 128,
		POOL_SIZE = 1000,
		DEFAULT_THREADS_FOR_QUEUE = 10,
		TIME_OUT_SEC = 50;
	private final BlockingQueue<Serializable> queue;
	private static Boolean ENABLED_FLAG;
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, TIME_OUT_SEC, TimeUnit.SECONDS,
		new ArrayBlockingQueue<Runnable>(100),new WorkerFactory("openJPA-appender-worker"));
	//
	private final int threadsForQueue;
	//
	private final boolean blocking;
	private final Configuration config;
	private final AppenderRef[] appenderRefs;
	private final String errorRef;
	private final boolean includeLocation;
	private AppenderControl errorAppender;
	private static ThreadLocal<Boolean> isAppenderThread = new ThreadLocal<Boolean>();

	private OpenJpaAsyncAppender(final String name, final Filter filter, final AppenderRef[] appenderRefs,
								   final String errorRef, final int queueSize, final boolean blocking,
								   final boolean ignoreExceptions, final Configuration config,
								   final boolean includeLocation, final int threadsForQueue) {
		super(name, filter, null, ignoreExceptions);
		this.queue = new ArrayBlockingQueue<Serializable>(queueSize);
		//
		this.threadsForQueue = threadsForQueue;
		//
		this.blocking = blocking;
		this.config = config;
		this.appenderRefs = appenderRefs;
		this.errorRef = errorRef;
		this.includeLocation = includeLocation;
	}
	//
	@Override
	public final void start() {
		final Map<String, Appender> map = config.getAppenders();
		final List<AppenderControl> appenders = new ArrayList<AppenderControl>();
		for (final AppenderRef appenderRef : appenderRefs) {
			if (map.containsKey(appenderRef.getRef())) {
				appenders.add(new AppenderControl(map.get(appenderRef.getRef()), appenderRef.getLevel(),
					appenderRef.getFilter()));
			} else {
				LOGGER.error("No appender named {} was configured", appenderRef);
			}
		}
		if (errorRef != null) {
			if (map.containsKey(errorRef)) {
				errorAppender = new AppenderControl(map.get(errorRef), null, null);
			} else {
				LOGGER.error("Unable to set up error Appender. No appender named {} was configured", errorRef);
			}
		}
		if (ENABLED_FLAG) {
			for (int i = 0; i < threadsForQueue; i++) {
				executor.submit(new QueueProcessorTask(appenders, queue));
			}
		}
		super.start();
	}
	//
	@Override
	public final void stop() {
		super.stop();
		//Poison Pill Shutdown
		queue.offer(POISON);
		//
		if (!executor.isShutdown()){
			executor.shutdown();
		}
		if (!executor.isTerminated()){
			try {
				//problem
				LOGGER.error(String.format("There are %d tasks from openJPA appender queue",
					queue.size()));
				//
				System.out.println(String.format("There are %d tasks from openJPA appender queue",
					queue.size()));
				//
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException e) {
				LOGGER.error("Interrupted waiting for submit executor to finish");
			}
		}
	}
	//
	@Override
	public final void append(LogEvent logEvent) {
		if(ENABLED_FLAG) {
			if (!isStarted()) {
				throw new IllegalStateException("AsyncAppender " + getName() + " is not active");
			}
			if (!(logEvent instanceof Log4jLogEvent)) {
				if (!(logEvent instanceof RingBufferLogEvent)) {
					return; // only know how to Serialize Log4jLogEvents and RingBufferLogEvents
				}
				logEvent = ((RingBufferLogEvent) logEvent).createMemento();
			}
			logEvent.getMessage().getFormattedMessage(); // LOG4J2-763: ask message to freeze parameters
			final Log4jLogEvent coreEvent = (Log4jLogEvent) logEvent;
			boolean appendSuccessful = false;
			if (blocking) {
				if (isAppenderThread.get() == Boolean.TRUE && queue.remainingCapacity() == 0) {
					coreEvent.setEndOfBatch(false); // queue is definitely not empty!
				} else {
					try {
						// wait for free slots in the queue
						queue.put(Log4jLogEvent.serialize(coreEvent, includeLocation));
						appendSuccessful = true;
					} catch (final InterruptedException e) {
						LOGGER.warn("Interrupted while waiting for a free slot in the AsyncAppender LogEvent-queue {}",
							getName());
					}
				}
			} else {
				appendSuccessful = queue.offer(Log4jLogEvent.serialize(coreEvent, includeLocation));
				if (!appendSuccessful) {
					error("Appender " + getName() + " is unable to write primary appenders. queue is full");
				}
			}
			if (!appendSuccessful && errorAppender != null) {
				errorAppender.callAppender(coreEvent);
			}
		}
	}
	/**
	 * Create an AsyncAppender.
	 * @param appenderRefs The Appenders to reference.
	 * @param errorRef An optional Appender to write to if the queue is full or other errors occur.
	 * @param blocking True if the Appender should wait when the queue is full. The default is true.
	 * @param size The size of the event queue. The default is 128.
	 * @param name The name of the Appender.
	 * @param includeLocation whether to include location information. The default is false.
	 * @param filter The Filter or null.
	 * @param config The Configuration.
	 * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged;
	 *                         otherwise they are propagated to the caller.
	 * @return The AsyncAppender.
	 */
	@PluginFactory
	public static OpenJpaAsyncAppender createAppender(
		@PluginElement("AppenderRef") final AppenderRef[] appenderRefs,
		@PluginAttribute("errorRef") @PluginAliases("error-ref") final String errorRef,
		@PluginAttribute(value = "enabled", defaultBoolean = false) final Boolean enabled,
		@PluginAttribute(value = "blocking", defaultBoolean = true) final boolean blocking,
		@PluginAttribute(value = "bufferSize", defaultInt = DEFAULT_QUEUE_SIZE) final int size,
		@PluginAttribute(value = "threadsForQueue", defaultInt = DEFAULT_THREADS_FOR_QUEUE) final int threadsForQueue,
		@PluginAttribute("name") final String name,
		@PluginAttribute(value = "includeLocation", defaultBoolean = false) final boolean includeLocation,
		@PluginElement("Filter") final Filter filter,
		@PluginConfiguration final Configuration config,
		@PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions)
	{
		ENABLED_FLAG = enabled;
		if (name == null) {
			LOGGER.error("No name provided for AsyncAppender");
			return null;
		}
		if (appenderRefs == null) {
			LOGGER.error("No appender references provided to AsyncAppender {}", name);
		}
		return new OpenJpaAsyncAppender(name, filter, appenderRefs, errorRef,
			size, blocking, ignoreExceptions, config, includeLocation, threadsForQueue);
	}
	////////////////////
	//Task for thread pool executor
	///////////////////
	private final class QueueProcessorTask
	implements Runnable{
		private final List<AppenderControl> appenders;
		private final BlockingQueue<Serializable> queue;
		private boolean shutdown = false;

		public QueueProcessorTask(final List<AppenderControl> appenders, final BlockingQueue<Serializable> queue) {
			this.appenders = appenders;
			this.queue = queue;
		}

		@Override
		public final void run() {
			isAppenderThread.set(Boolean.TRUE); // LOG4J2-485
			while (!shutdown) {
				Serializable s;
				try {
					s = queue.take();
					if (s != null && s instanceof String && POISON.equals(s.toString())) {
						shutdown = true;
						queue.offer(POISON);//notify other threads to stop
						continue;
					}
				} catch (final InterruptedException ex) {
					break; // LOG4J2-830
				}
				final Log4jLogEvent event = Log4jLogEvent.deserialize(s);
				event.setEndOfBatch(queue.isEmpty());
				final boolean success = callAppenders(event);
				if (!success && errorAppender != null) {
					try {
						errorAppender.callAppender(event);
					} catch (final Exception ex) {
						// Silently accept the error.
					}
				}
			}
			// Process any remaining items in the queue.
			LOGGER.trace("AsyncAppender.AsyncThread shutting down. Processing remaining {} queue events.",
				queue.size());
			int count= 0;
			int ignored = 0;
			while (!queue.isEmpty()) {
				try {
					final Serializable s = queue.take();
					if (s != null && s instanceof String && POISON.equals(s.toString())) {
						queue.offer(POISON);//notify other threads to stop
						break;
					}
					if (Log4jLogEvent.canDeserialize(s)) {
						final Log4jLogEvent event = Log4jLogEvent.deserialize(s);
						event.setEndOfBatch(queue.isEmpty());
						callAppenders(event);
						count++;
					} else {
						ignored++;
						LOGGER.trace("Ignoring event of class {}", s.getClass().getName());
					}
				} catch (final InterruptedException ex) {
					// May have been interrupted to shut down.
					// Here we ignore interrupts and try to process all remaining events.
				}
			}
			LOGGER.trace("AsyncAppender.AsyncThread stopped. Queue has {} events remaining. " +
					"Processed {} and ignored {} events since shutdown started.",
				queue.size(), count, ignored);
		}

		/**
		 * Calls {@link AppenderControl#callAppender(LogEvent) callAppender} on
		 * all registered {@code AppenderControl} objects, and returns {@code true}
		 * if at least one appender call was successful, {@code false} otherwise.
		 * Any exceptions are silently ignored.
		 *
		 * @param event the event to forward to the registered appenders
		 * @return {@code true} if at least one appender call succeeded, {@code false} otherwise
		 */
		private boolean callAppenders(final Log4jLogEvent event) {
			boolean success = false;
			for (final AppenderControl control : appenders) {
				try {
					control.callAppender(event);
					success = true;
				} catch (final Exception ex) {
					// If no appender is successful the error appender will get it.
				}
			}
			return success;
		}
	}
}