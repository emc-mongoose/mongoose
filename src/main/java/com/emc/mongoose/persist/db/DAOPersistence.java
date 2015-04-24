package com.emc.mongoose.persist.db;
//
// mongoose-common.jar
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.task.IOTask;
//mongoose-persistence.jar
import com.emc.mongoose.persist.db.entity.ApiEntity;
import com.emc.mongoose.persist.db.entity.LevelEntity;
import com.emc.mongoose.persist.db.entity.LoadEntity;
import com.emc.mongoose.persist.db.entity.LoadEntityPK;
import com.emc.mongoose.persist.db.entity.LoadTypeEntity;
import com.emc.mongoose.persist.db.entity.MessageClassEntity;
import com.emc.mongoose.persist.db.entity.MessageEntity;
import com.emc.mongoose.persist.db.entity.ModeEntity;
import com.emc.mongoose.persist.db.entity.PerfomanceEntity;
import com.emc.mongoose.persist.db.entity.RunEntity;
import com.emc.mongoose.persist.db.entity.StatusEntity;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
//
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
//
import java.io.Serializable;
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
public final class DAOPersistence {
	//
	private Logger logger;
	//
	private EntityManagerFactory entityManagerFactory = null;
	private final ThreadPoolExecutor executor;
	//
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
		FIFTEEN_MIN_BW = "fifteenMinBW",
		//
		KEY_LOAD_NUM = "load.number",
		KEY_API = "api",
		KEY_LOAD_TYPE = "load.type",
		KEY_THREAD_NUM = "thread.number";
	//
	public final void stop() {
		if (!executor.isShutdown()){
			executor.shutdown();
		}
		if (!executor.isTerminated()) {
			try {
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException e) {
				LogUtil.failure(logger, Level.ERROR, e, "Interrupted waiting for submit executor to finish");
			}
		}
		entityManagerFactory.close();
	}
	//
	private DAOPersistence(
		final int poolSize, final int poolTimeout,
		final int queueSize, final String provider, final String addr,
		final String port, final String dbName, final String driverClass,
		final int maxActiveConnection, final int connectionTimeout,
		final String userName, final String password) {
		executor = new ThreadPoolExecutor(poolSize, poolSize, poolTimeout, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(queueSize),new NamingWorkerFactory("persistence-worker"));
		logger = LogManager.getLogger();
		final String url = String.format("jdbc:%s://%s:%s/%s", provider, addr, port, dbName);
		try {

			buildEntityManagerFactory(
				driverClass, url, maxActiveConnection, connectionTimeout, userName, password);
			//?!
			persistStatusEntity();
		} catch (final Exception e) {
			LogUtil.failure(StatusLogger.getLogger(), Level.ERROR, e,
				"DB session failed");
		}
	}
	//
	public final void submit(final String event) {
		try {
			executor.submit(new PersistenceTask(event));
		} catch (final RejectedExecutionException e){
			LogUtil.failure(logger, Level.ERROR, e, String.format(
				"Task for event %s doesn't submit.", event));
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
				LogUtil.failure(logger, Level.ERROR, e, "Fail to persist status entity.");
			}
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
			LogUtil.failure(StatusLogger.getLogger(), Level.ERROR, e, "Parse run timestamp is failed");
		}catch (final NullPointerException e){
			LogUtil.failure(StatusLogger.getLogger(), Level.ERROR, e, "Parse run timestamp is failed. " +
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
	private final class PersistenceTask
	implements Runnable{
		private final String event;

		public PersistenceTask(final String event){
			this.event = event;
		}

		@Override
		public final void run() {
			//TODO
		}

	}
}
//