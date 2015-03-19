package com.emc.mongoose.common.logging.db;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.util.log.Markers;
import com.emc.mongoose.core.impl.load.executor.util.DataObjectWorkerFactory;
import com.emc.mongoose.core.impl.util.RunTimeConfig;
import com.emc.mongoose.core.impl.util.WorkerFactory;
import com.emc.mongoose.core.impl.util.log.TraceLogger;
import com.emc.mongoose.common.logging.db.entity.ApiEntity;
import com.emc.mongoose.common.logging.db.entity.LevelEntity;
import com.emc.mongoose.common.logging.db.entity.LoadEntity;
import com.emc.mongoose.common.logging.db.entity.LoadEntityPK;
import com.emc.mongoose.common.logging.db.entity.LoadTypeEntity;
import com.emc.mongoose.common.logging.db.entity.MessageClassEntity;
import com.emc.mongoose.common.logging.db.entity.MessageEntity;
import com.emc.mongoose.common.logging.db.entity.ModeEntity;
import com.emc.mongoose.common.logging.db.entity.PerfomanceEntity;
import com.emc.mongoose.common.logging.db.entity.RunEntity;
import com.emc.mongoose.common.logging.db.entity.StatusEntity;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.SerializedLayout;
//
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.openjpa.enhance.InstrumentationFactory;
import org.apache.openjpa.persistence.RollbackException;
//
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import java.io.Serializable;
import java.lang.instrument.Instrumentation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
//
/**
 * Created by olga on 24.10.14.
 */
@Plugin(name="OpenJPA", category="Core", elementType="appender", printObject=true)
public final class OpenJpaAppender
extends AbstractAppender {
	//
	private Logger logger;
	//
	private EntityManagerFactory entityManagerFactory = null;
	private final ThreadPoolExecutor executor;
	private final Boolean enabledFlag;
	//
	private final static Layout<? extends Serializable>
		DEFAULT_LAYOUT = SerializedLayout.createLayout();
	public final static String
		MSG = "msg",
		ERR = "err",
		PERF_AVG = "perfAvg",
		PERF_SUM = "perfSum",
		//
		COUNT_REQ_SUCC = "countReqSucc",
		COUNT_REQ_QUEUE = "countReqQueue",
		COUNT_REQ_FAIL = "countReqFail",
		LATENCY_AVG = "latencyAvg",
		LATENCY_MIN = "latencyMin",
		LATENCY_MED = "latencyMed",
		LATENCY_MAX = "latencyMax",
		MEAN_TP = "meanTP",
		ONE_MIN_TP = "oneMinTP",
		FIVE_MIN_TP = "fiveMinTP",
		FIFTEEN_MIN_TP = "fifteenMinTP",
		MEAN_BW = "meanBW",
		ONE_MIN_BW = "oneMinBW",
		FIVE_MIN_BW = "fiveMinBW",
		FIFTEEN_MIN_BW = "fifteenMinBW";

	//
	@Override
	public final void start() {
		super.start();
	}
	//
	@Override
	public final void stop() {
		super.stop();
		if (enabledFlag) {
			if (!executor.isShutdown()){
				executor.shutdown();
			}
			if (!executor.isTerminated()) {
				try {
					executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
				} catch (final InterruptedException e) {
					TraceLogger.failure(logger, Level.ERROR, e, "Interrupted waiting for submit executor to finish");
				}
			}
			entityManagerFactory.close();
		}
	}
	//
	private OpenJpaAppender(
		final String name, final Filter filter,	final Layout<? extends Serializable> layout,
		final boolean ignoreExceptions, final boolean enabled,
		final int appenderPoolSize, final int appenderPoolTimeout, final int openJpaQueueSize) {
		super(name, filter, layout, ignoreExceptions);
		executor = new ThreadPoolExecutor(appenderPoolSize, appenderPoolSize, appenderPoolTimeout, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(openJpaQueueSize),new WorkerFactory("openJPA-appender-worker"));
		enabledFlag = enabled;
		logger = LogManager.getLogger();
	}
	//
	@PluginFactory
	public static OpenJpaAppender createAppender(
		final @PluginAttribute(value = "name") String name,
		final @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) boolean ignoreExceptions,
		final @PluginElement(value = "Filters") Filter filter,
		final @PluginAttribute(value = "enabled", defaultBoolean = false) Boolean enabled,
		final @PluginAttribute(value = "database", defaultString = "postgresql") String provider,
		final @PluginAttribute(value = "username", defaultString = "mongoose") String userName,
		final @PluginAttribute(value = "password", defaultString = "mongoose") String password,
		final @PluginAttribute(value = "addr", defaultString = "localhost") String addr,
		final @PluginAttribute(value = "port", defaultString = "5432") String port,
		final @PluginAttribute(value = "namedatabase", defaultString = "mongoose") String dbName,
		final @PluginAttribute(value = "DriverClassName", defaultString = "org.postgresql.Driver") String driverClass,
		final @PluginAttribute(value = "maxActiveConnection", defaultInt = 100) int maxActiveConnection,
		final @PluginAttribute(value = "connectionTimeout", defaultInt = 10) int connectionTimeout,
		final @PluginAttribute(value = "appenderQueueSize", defaultInt = 1000) int appenderQueueSize,
		final @PluginAttribute(value = "appenderPoolSize", defaultInt = 1000) int appenderPoolSize,
		final @PluginAttribute(value = "appenderPoolTimeout", defaultInt = 0) int appenderPoolTimeout) {
		OpenJpaAppender newAppender = null;
		if (name == null) {
			throw new IllegalArgumentException("No name provided for HibernateAppender");
		}
		final String url = String.format("jdbc:%s://%s:%s/%s", provider, addr, port, dbName);
		try {
			newAppender = new OpenJpaAppender(
				name, filter, DEFAULT_LAYOUT, ignoreExceptions,
				enabled, appenderPoolSize, appenderPoolTimeout, appenderQueueSize);
			if (enabled) {
				newAppender.buildEntityManagerFactory(
					driverClass, url, maxActiveConnection, connectionTimeout, userName, password);
				newAppender.persistStatusEntity();
			}
		} catch (final Exception e) {
			TraceLogger.failure(StatusLogger.getLogger(), Level.ERROR, e,
				"Open DB session failed");
		}
		return newAppender;
	}
	//
	@Override
	public final void append(final LogEvent event) {
		if (enabledFlag) {
			try {
				executor.submit(new AppendTask(event));
			} catch (final RejectedExecutionException e){
				TraceLogger.failure(logger, Level.ERROR, e, String.format(
					"Task for event %s doesn't submit.", event.getMessage().getFormattedMessage()));
			}
		}
	}
	/////////////////////////////////////////// Persist ////////////////////////////////////////////////////////////////
	private void persistStatusEntity()
	{
		EntityManager entityManager = null;
		for (final IOTask.Status result:IOTask.Status.values()){
			final StatusEntity statusEntity = new StatusEntity(result.code, result.description);
			try {
				entityManager = entityManagerFactory.createEntityManager();
				entityManager.getTransaction().begin();
				entityManager.merge(statusEntity);
				entityManager.getTransaction().commit();
				entityManager.close();
			}catch (final Exception e){
				if (entityManager != null) {
					entityManager.getTransaction().rollback();
					entityManager.close();
				}
				TraceLogger.failure(logger, Level.ERROR, e, "Fail to persist status entity.");
			}
		}
	}
	//
	private void persistPerf(final LogEvent event, final String marker){
		final String
			modeName = event.getContextMap().get(RunTimeConfig.KEY_RUN_MODE),
			runName = event.getContextMap().get(RunTimeConfig.KEY_RUN_ID),
			loadTypeName = event.getContextMap().get(DataObjectWorkerFactory.KEY_LOAD_TYPE),
			apiName = event.getContextMap().get(DataObjectWorkerFactory.KEY_API);
		final long
			countReqSucc = Long.valueOf(event.getContextMap().get(COUNT_REQ_SUCC)),
			countReqFail = Long.valueOf(event.getContextMap().get(COUNT_REQ_FAIL)),
			countReqInQueue = Long.valueOf(event.getContextMap().get(COUNT_REQ_QUEUE)),
			loadNumber = Long.valueOf(event.getContextMap().get(DataObjectWorkerFactory.KEY_LOAD_NUM));
		final double
			meanTP = Double.valueOf(event.getContextMap().get(MEAN_TP)),
			oneMinTP = Double.valueOf(event.getContextMap().get(ONE_MIN_TP)),
			fiveMinTP = Double.valueOf(event.getContextMap().get(FIVE_MIN_TP)),
			fifteenMinTP = Double.valueOf(event.getContextMap().get(FIFTEEN_MIN_TP)),
			meanBW = Double.valueOf(event.getContextMap().get(MEAN_BW)),
			oneMinBW = Double.valueOf(event.getContextMap().get(ONE_MIN_BW)),
			fiveMinBW = Double.valueOf(event.getContextMap().get(FIVE_MIN_BW)),
			fifteenMinBW = Double.valueOf(event.getContextMap().get(FIFTEEN_MIN_BW));
		final Date
			runTimestamp = getTimestamp(event.getContextMap().get(RunTimeConfig.KEY_RUN_TIMESTAMP)),
			timestamp = new Date(event.getTimeMillis());
		final int
			latencyAvg = Integer.valueOf(event.getContextMap().get(LATENCY_AVG)),
			latencyMin = Integer.valueOf(event.getContextMap().get(LATENCY_MIN)),
			latencyMed = Integer.valueOf(event.getContextMap().get(LATENCY_MED)),
			latencyMax = Integer.valueOf(event.getContextMap().get(LATENCY_MAX));
		EntityManager entityManager = null;
		try {
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			/*
			ModeEntity modeEntity = (ModeEntity) getEntity("name", modeName,
				ModeEntity.class);
			if (modeEntity == null) {
				modeEntity = new ModeEntity(modeName);
			}
			*/
			//
			RunEntity runEntity = (RunEntity) getEntity("name", runName, "timestamp", runTimestamp, RunEntity.class);
			/*
			if (runEntity == null) {
				runEntity = new RunEntity(modeEntity, runName, timestamp);
			}
			entityManager.merge(runEntity);
			*/
			//
			LoadTypeEntity loadTypeEntity = (LoadTypeEntity) getEntity("name", loadTypeName, LoadTypeEntity.class);
			if (loadTypeEntity == null) {
				loadTypeEntity = new LoadTypeEntity(loadTypeName);
			}
			//
			ApiEntity apiEntity = (ApiEntity) getEntity("name", apiName, ApiEntity.class);
			if (apiEntity == null){
				apiEntity = new ApiEntity(apiName);
			}

			//
			final LoadEntityPK loadEntityPK = new LoadEntityPK(loadNumber, runEntity.getId());
			LoadEntity loadEntity = entityManager.find(LoadEntity.class, loadEntityPK);
			if (loadEntity == null) {
				if (marker.equals(PERF_AVG)){
					loadEntity = new LoadEntity(runEntity, loadTypeEntity, loadNumber, apiEntity);
				} else {
					loadEntity = new LoadEntity(
						runEntity, loadTypeEntity, loadNumber, apiEntity,
						countReqSucc, countReqFail,
						latencyAvg, latencyMin, latencyMed, latencyMax,
						meanTP, oneMinTP, fiveMinTP, fifteenMinTP,
						meanBW, oneMinBW, fiveMinBW, fifteenMinBW);
				}
				entityManager.persist(loadEntity);
			}else {
				if (marker.equals(PERF_SUM)){
					loadEntity.setPerfomance(
						countReqSucc, countReqFail,
						latencyAvg, latencyMin, latencyMed, latencyMax,
						meanTP, oneMinTP, fiveMinTP, fifteenMinTP,
						meanBW, oneMinBW, fiveMinBW, fifteenMinBW
					);
					entityManager.merge(loadEntity);
				}else{
					entityManager.persist(loadEntity);
				}
			}

			//
			if (marker.equals(PERF_AVG)){
				final PerfomanceEntity perfomanceEntity = new PerfomanceEntity(
					loadEntity, timestamp,
					countReqSucc, countReqInQueue, countReqFail,
					latencyAvg, latencyMin, latencyMed, latencyMax,
					meanTP,oneMinTP,fiveMinTP,fifteenMinTP,
					meanBW,oneMinBW,fiveMinBW,fifteenMinBW);
				entityManager.persist(perfomanceEntity);
			}
			//
			entityManager.getTransaction().commit();
			entityManager.close();
		} catch (final RollbackException exception) {
			if (entityManager != null) {
				entityManager.getTransaction().rollback();
				entityManager.close();
			}
			persistPerf(event, marker);
			logger.trace(Markers.MSG, String.format("Try one more time persist perfAvg for thread: %s",
				Thread.currentThread().getName()));
		} catch (final Exception e){
			if (entityManager != null) {
				entityManager.getTransaction().rollback();
				entityManager.close();
			}
			TraceLogger.failure(logger, Level.ERROR, e, "Fail to persist perf.avg.");
			e.printStackTrace();
		}
	}
	//
	private void persistMessages(final LogEvent event)
	{
		final String
			modeName = event.getContextMap().get(RunTimeConfig.KEY_RUN_MODE),
			runName = event.getContextMap().get(RunTimeConfig.KEY_RUN_ID),
			levelName = event.getLevel().toString(),
			className = event.getLoggerName();
		final Date
			timestamp = getTimestamp(event.getContextMap().get(RunTimeConfig.KEY_RUN_TIMESTAMP)),
			timestampMessage = new Date(event.getTimeMillis());
		//
		EntityManager entityManager = null;
		//
		try {
			ModeEntity modeEntity = (ModeEntity) getEntity("name", modeName,
				ModeEntity.class);
			if (modeEntity == null) {
				modeEntity = new ModeEntity(modeName);
			}
			//
			RunEntity runEntity = (RunEntity) getEntity("name", runName, "timestamp", timestamp, RunEntity.class);
			if (runEntity == null) {
				runEntity = new RunEntity(modeEntity, runName, timestamp);
			}
			//
			LevelEntity levelEntity = (LevelEntity) getEntity("name", levelName, LevelEntity.class);
			if (levelEntity == null) {
				levelEntity = new LevelEntity(levelName);
			}
			//
			MessageClassEntity messageClassEntity = (MessageClassEntity) getEntity("name", className, MessageClassEntity.class);
			if (messageClassEntity == null) {
				messageClassEntity = new MessageClassEntity(className);
			}
			//
			final MessageEntity messageEntity = new MessageEntity(runEntity, messageClassEntity, levelEntity,
				event.getMessage().getFormattedMessage(), timestampMessage);
			//
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.persist(messageEntity);
			entityManager.getTransaction().commit();
			entityManager.close();
		}catch (final RollbackException exception) {
			if (entityManager != null) {
				entityManager.getTransaction().rollback();
				entityManager.close();
			}
			persistMessages(event);
			logger.trace(Markers.MSG, String.format("Try one more time for thread: %s", Thread.currentThread().getName()));
		}catch (Exception e){
			if (entityManager != null) {
				entityManager.getTransaction().rollback();
				entityManager.close();
			}
			TraceLogger.failure(logger, Level.ERROR, e, "Fail to persist messages.");
			e.printStackTrace();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private void buildEntityManagerFactory(

		final String driverClass, final String url, final int maxActiveConnection,
		final int connectionTimeout, final String username, final String password) {
		final Map<String, String> properties = new HashMap<>();

		final String connectionPropertiesValue = String.format(
			"DriverClassName = %s , " +
			"Url = %s, MaxActive = %d, MaxWait = %d, " +
			"TestOnBorrow = true, Username = %s, Password = %s",
			driverClass, url, maxActiveConnection, connectionTimeout, username, password
		);
		//
		properties.put("openjpa.ConnectionProperties", connectionPropertiesValue);
		entityManagerFactory = Persistence.createEntityManagerFactory("openjpa", properties);
	}
	//parse String to Date
	private Date getTimestamp(final String stringTimestamp){
		Date runTimestamp = null;
		try {
				runTimestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(stringTimestamp);
		} catch (final ParseException e) {
			TraceLogger.failure(StatusLogger.getLogger(), Level.ERROR, e, "Parse run timestamp is failed");
		}catch (final NullPointerException e){
			TraceLogger.failure(StatusLogger.getLogger(), Level.ERROR, e, "Parse run timestamp is failed. " +
				"Timestamp equals null.");
			e.printStackTrace();
		}
		return runTimestamp;
	}
	///////////////////////////// Getters //////////////////////////////////////////////////////////////////////////////
	private Object getEntity(final String colomnName, final Object value, final Class classEntity){
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		CriteriaBuilder queryBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery criteriaQuery = queryBuilder.createQuery();
		Root modeRoot = criteriaQuery.from(classEntity);
		criteriaQuery.select(modeRoot);
		Predicate predicate= queryBuilder.equal(modeRoot.get(colomnName), value);
		criteriaQuery.where(predicate);
		List result = entityManager.createQuery(criteriaQuery).getResultList();
		entityManager.close();
		return getUniqueResult(result);
	}
	//
	private Object getEntity(
		final String colomnName1, final Object value1,
		 final String colomnName2, final Object value2,
		 final Class classEntity)
	{
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		CriteriaBuilder queryBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery criteriaQuery = queryBuilder.createQuery();
		Root modeRoot = criteriaQuery.from(classEntity);
		criteriaQuery.select(modeRoot);
		Predicate predicate1= queryBuilder.equal(modeRoot.get(colomnName1), value1);
		Predicate predicate2 = queryBuilder.equal(modeRoot.get(colomnName2), value2);
		criteriaQuery.where(queryBuilder.and(predicate1,predicate2));
		List result = entityManager.createQuery(criteriaQuery).getResultList();
		entityManager.close();
		return getUniqueResult(result);
	}
	//
	private Object getUniqueResult(final List result){
		if (result.size() > 1){
			throw new NonUniqueResultException(
				String.format("non unique result: count of results: %d", result.size()));
		}
		if (result.isEmpty()){
			return null;
		} else  return result.get(0);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Worker OpenJPA
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private final class AppendTask
	implements Runnable{

		private final LogEvent event;

		public AppendTask(final LogEvent event){
			this.event = event;
		}

		@Override
		public final void run() {
			final String marker = event.getMarker().toString();
			if (!event.getContextMap().isEmpty()) {
				switch (marker) {
					case PERF_AVG:
					case PERF_SUM:
						if (event.getLevel().equals(Level.INFO)) {
							//persistPerf(event, marker);
						}
						break;
					case MSG:
					case ERR:
						persistMessages(event);
						break;
				}
			}else{
				logger.debug(Markers.ERR, String.format("There is event with empty Context Map for logger %s . Event message: %s",
					event.getLoggerName(), event.getMessage()));
			}
		}

	}
}
//