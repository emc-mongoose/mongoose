package com.emc.mongoose.persist;
//
// mongoose-common.jar
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.task.IOTask;
//mongoose-persistence.jar
import com.emc.mongoose.persist.entity.LevelEntity;
import com.emc.mongoose.persist.entity.MessageClassEntity;
import com.emc.mongoose.persist.entity.MessageEntity;
import com.emc.mongoose.persist.entity.ModeEntity;
import com.emc.mongoose.persist.entity.RunEntity;
import com.emc.mongoose.persist.entity.StatusEntity;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
//
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
//
/**
 * Created by olga on 24.10.14.
 */
public final class PersistDAO {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private static final String PROVIDER = RunTimeConfig.getContext().getPersistDatabaseProvider();
	private static final String DRIVER_CLASS = RunTimeConfig.getContext().getPersistDatabaseDriver();
	private static final String USER_NAME = RunTimeConfig.getContext().getPersistDatabaseUsername();
	private static final String PASSWORD = RunTimeConfig.getContext().getPersistDatabasePassword();
	private static final String ADDR = RunTimeConfig.getContext().getPersistDatabaseAddr();
	private static final String PORT = RunTimeConfig.getContext().getPersistDatabasePort();
	private static final String DATABASE_NAME = RunTimeConfig.getContext().getPersistDatabaseName();
	private static final int MAX_ACTIVE_CONNECTION = RunTimeConfig.getContext().getPersistMaxActiveConnection();
	private static final int CONNECTION_TIMEOUT = RunTimeConfig.getContext().getPersistConnectionTimeout();
	//
	private EntityManagerFactory entityManagerFactory;
	//

	public PersistDAO(
	){
		final String URL = String.format("jdbc:%s://%s:%s/%s", PROVIDER, ADDR, PORT, DATABASE_NAME);
		entityManagerFactory = buildEntityManagerFactory(
			DRIVER_CLASS, URL, MAX_ACTIVE_CONNECTION, CONNECTION_TIMEOUT, USER_NAME, PASSWORD);
		//persistStatusEntity();
	}
	//
	public final void closeEntityMF() {
		entityManagerFactory.close();
	}
	//
	/////////////////////////////////////////// Persist ////////////////////////////////////////////////////////////////
	public void persistRunEntiry(final String runName, final String modeName){
		RunEntity runEntity = (RunEntity) getEntity("name", runName, RunEntity.class);
		if (runEntity == null){
			ModeEntity modeEntity = (ModeEntity) getEntity("name", modeName, ModeEntity.class);
			if (modeEntity == null){
				modeEntity = new ModeEntity(modeName);
			}
			runEntity = new RunEntity(modeEntity, runName);
			EntityManager entityManager = entityManagerFactory.createEntityManager();
			try{
				persistEntity(entityManager, runEntity);
			}catch (final RollbackException e){
				if (entityManager != null) {
					entityManager.getTransaction().rollback();
					entityManager.close();
					persistRunEntiry(runName,modeName);
				}
			}catch (final Exception e){
				if (entityManager != null) {
					entityManager.getTransaction().rollback();
					entityManager.close();
				}
				LogUtil.failure(LOG, Level.ERROR, e, "Fail to persist status entity.");
			}
		}
	}
	//
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
				LogUtil.failure(LOG, Level.ERROR, e, "Fail to persist status entity.");
			}
		}
	}
	//
	public final void persistMessage(final PersistEvent event){
		final String
			runName = event.getRunName(),
			levelName = event.getEventLevel(),
			className = event.getEventClass(),
			message = event.getEventMessage();
		final Date timestamp = event.getEventTstamp();
		//
		RunEntity runEntity = (RunEntity) getEntity("name", runName, RunEntity.class);
		//
		LevelEntity levelEntity = (LevelEntity) getEntity("name", event.getEventLevel(), LevelEntity.class);
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
			message, timestamp);
		//
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			persistEntity(entityManager, messageEntity);
		}catch (final RollbackException exception){
			if (entityManager != null) {
				entityManager.getTransaction().rollback();
				entityManager.close();
			}
			persistMessage(event);
			LOG.trace(LogUtil.MSG, String.format("Try one more time for thread: %s", Thread.currentThread().getName()));
		}catch (final Exception e){
			if (entityManager != null) {
				entityManager.getTransaction().rollback();
				entityManager.close();
			}
			LogUtil.failure(LOG, Level.ERROR, e, "Fail to persist messages.");
		}
	}
	//
	private static void persistEntity(final EntityManager entityManager, final Object entity)
	throws RollbackException{
		entityManager.getTransaction().begin();
		entityManager.persist(entity);
		entityManager.getTransaction().commit();
		entityManager.close();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static EntityManagerFactory buildEntityManagerFactory(
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
		return Persistence.createEntityManagerFactory("openjpa", properties);
	}
	///////////////////////////// Getters //////////////////////////////////////////////////////////////////////////////
	public final Object getEntity(final String colomnName, final Object value, final Class classEntity){
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
	public final Object getEntity(
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
	private static Object getUniqueResult(final List result){
		if (result.size() > 1){
			throw new NonUniqueResultException(
				String.format("non unique result: count of results: %d", result.size()));
		}
		if (result.isEmpty()){
			return null;
		} else  return result.get(0);
	}
}
//