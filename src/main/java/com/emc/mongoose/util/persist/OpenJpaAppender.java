package com.emc.mongoose.util.persist;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.threading.DataObjectWorkerFactory;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.SerializedLayout;
//
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.openjpa.persistence.RollbackException;
import org.apache.openjpa.util.StoreException;
//
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//
/**
 * Created by olga on 24.10.14.
 */
@Plugin(name="OpenJPA", category="Core", elementType="appender", printObject=true)
public final class OpenJpaAppender
extends AbstractAppender {
	//
	private static EntityManagerFactory ENTITY_MANAGER_FACTORY = null;
	//
	private final static Layout<? extends Serializable>
		DEFAULT_LAYOUT = SerializedLayout.createLayout();
	private static Boolean ENABLED_FLAG;
	private static final String
		MSG = "msg",
		PERF_TRACE = "perfTrace",
		ERR = "err",
		DATA_LIST = "dataList";

	//
	@Override
	public final void start() {
		super.start();
	}

	//
	@Override
	public final void stop() {
		super.stop();
		if (ENABLED_FLAG) {
			ENTITY_MANAGER_FACTORY.close();
		}
	}

	//
	private OpenJpaAppender(
		final String name, final Filter filter,
		final Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions);
	}

	//
	@PluginFactory
	public static OpenJpaAppender createAppender(
		final @PluginAttribute("name") String name,
		final @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
		final @PluginElement("Filters") Filter filter,
		final @PluginAttribute("enabled") Boolean enabled,
		final @PluginAttribute("database") String provider,
		final @PluginAttribute("username") String userName,
		final @PluginAttribute("password") String passWord,
		final @PluginAttribute("addr") String addr,
		final @PluginAttribute("port") String port,
		final @PluginAttribute("namedatabase") String dbName) {
		OpenJpaAppender newAppender = null;
		ENABLED_FLAG = enabled;
		if (name == null) {
			throw new IllegalArgumentException("No name provided for HibernateAppender");
		}
		final String url = String.format("jdbc:%s://%s:%s/%s", provider, addr, port, dbName);
		try {
			newAppender = new OpenJpaAppender(name, filter, DEFAULT_LAYOUT, ignoreExceptions);
			if (ENABLED_FLAG) {
				// init database session with username,password and url
				newAppender.buildSessionFactory(userName, passWord, url);
				newAppender.persistStatusEntity();
			}
		} catch (final Exception e) {
			throw new IllegalStateException("Open DB session failed", e);
		}
		return newAppender;
	}

	// append method // - really?! (kurilov) - yep! (zhavzharova)
	@Override
	public final void append(final LogEvent event) {
		if (ENABLED_FLAG) {
			final String marker = event.getMarker().toString();
			final String[] message = event.getMessage().getFormattedMessage().split("\\s*[,|/]\\s*");
			switch (marker) {
				case MSG:
				case ERR:
					if (!event.getContextMap().isEmpty()) {
						persistMessages(event);
					}
					break;
				case DATA_LIST:
					persistDataList(message);
					break;
				case PERF_TRACE:
					if (!event.getContextMap().isEmpty()) {
						persistPerfTrace(message, event);
					}
					break;
			}
		}
	}
	/////////////////////////////////////////// Persist ///////////////////////////////////////////////
	private void persistStatusEntity()
	{
		for (final AsyncIOTask.Status result:AsyncIOTask.Status.values()){
			final StatusEntity statusEntity = new StatusEntity(result.code, result.description);
			try {
				final EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
				entityManager.getTransaction().begin();
				entityManager.merge(statusEntity);
				entityManager.getTransaction().commit();
				entityManager.close();
			}catch (final Exception e){
				e.printStackTrace();
			}
		}
	}
	//
	private void persistMessages(final LogEvent event)
	{
		MessageEntity messageEntity = null;
		EntityManager entityManager = null;
		//
		try {
			final String modeName = event.getContextMap().get(RunTimeConfig.KEY_RUN_MODE);
			ModeEntity modeEntity = (ModeEntity) getEntity("name", modeName,
				ModeEntity.class);
			if (modeEntity == null) {
				modeEntity = new ModeEntity(modeName);
			}
			//
			final String runName = event.getContextMap().get(RunTimeConfig.KEY_RUN_ID);
			final Date timestamp = getTimestamp(event.getContextMap().get(RunTimeConfig.KEY_RUN_TIMESTAMP));
			RunEntity runEntity = (RunEntity) getEntity("name", runName, "timestamp", timestamp, RunEntity.class);
			if (runEntity == null) {
				runEntity = new RunEntity(modeEntity, runName, timestamp);
			}
			//
			final String levelName = event.getLevel().toString();
			LevelEntity levelEntity = (LevelEntity) getEntity("name", levelName, LevelEntity.class);
			if (levelEntity == null) {
				levelEntity = new LevelEntity(levelName);
			}
			//
			final String className = event.getLoggerName();
			MessageClassEntity messageClassEntity = (MessageClassEntity) getEntity("name", className, MessageClassEntity.class);
			if (messageClassEntity == null) {
				messageClassEntity = new MessageClassEntity(className);
			}
			//
			messageEntity = new MessageEntity(runEntity, messageClassEntity, levelEntity,
				event.getMessage().getFormattedMessage(), new Date(event.getTimeMillis()));
			//
			entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.persist(messageEntity);
			entityManager.getTransaction().commit();
			entityManager.close();
		}catch (final RollbackException exception) {
			entityManager.close();
			persistMessages(event);
			System.out.println("Try one more time " + Thread.currentThread().getName());
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	//
	private void persistDataList(final String[] message){
		final EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
		DataObjectEntityPK dataObjectEntityPK = new DataObjectEntityPK(message[0], Long.valueOf(message[2]));
		DataObjectEntity dataObjectEntity = entityManager.find(DataObjectEntity.class, dataObjectEntityPK);
		if (dataObjectEntity == null){
			dataObjectEntity = new DataObjectEntity( message[0], message[1],
				Long.valueOf(message[2]), Long.valueOf(message[3], 0x10),
				Long.valueOf(message[4], 0x10));
		}
		try {
			entityManager.getTransaction().begin();
			entityManager.persist(dataObjectEntity);
			entityManager.getTransaction().commit();
			entityManager.close();
		}catch (final Exception e){
			//delete!
			entityManager.close();
			e.printStackTrace();
		}
	}
	//
	private void persistPerfTrace(final String[] message, final LogEvent event){
		//final DataObjectEntity dataObjectEntity = new DataObjectEntity(message[1], Integer.valueOf(message[2]));
		//
		System.out.println("run");
		ConnectionEntity connectionEntity = null;
		EntityManager entityManager = null;
		LoadEntity loadEntity = null;
		try {
			final String runName = event.getContextMap().get(RunTimeConfig.KEY_RUN_ID);
			final Date timestamp = getTimestamp(event.getContextMap().get(RunTimeConfig.KEY_RUN_TIMESTAMP));
			final RunEntity runEntity = (RunEntity) getEntity("name", runName, "timestamp", timestamp, RunEntity.class);
			if (runEntity == null) {
				System.out.println("run null");
			}
			//
			System.out.println("LOAD type");
			final String loadTypeName = event.getContextMap().get(DataObjectWorkerFactory.KEY_LOAD_TYPE);
			LoadTypeEntity loadTypeEntity = (LoadTypeEntity) getEntity("name", loadTypeName, LoadTypeEntity.class);
			if (loadTypeEntity == null) {
				loadTypeEntity = new LoadTypeEntity(loadTypeName);
			}
			//
			System.out.println("API");
			final String apiName = event.getContextMap().get(DataObjectWorkerFactory.KEY_API);
			ApiEntity apiEntity = (ApiEntity) getEntity("name", apiName, ApiEntity.class);
			if (apiEntity == null){
				apiEntity = new ApiEntity(apiName);
			}
			//
			System.out.println("Node addrs");
			final String nodeAddrs = message[0];
			NodeEntity nodeEntity = (NodeEntity) getEntity("address", nodeAddrs, NodeEntity.class);
			if (nodeEntity == null) {
				nodeEntity = new NodeEntity(nodeAddrs);
			}

			//
			entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
			entityManager.getTransaction().begin();
			//
			System.out.println("Load");
			final long loadNumber = Long.valueOf(event.getContextMap().get(DataObjectWorkerFactory.KEY_LOAD_NUM));
			final LoadEntityPK loadEntityPK = new LoadEntityPK(loadNumber, runEntity.getId());
			loadEntity = entityManager.find(LoadEntity.class, loadEntityPK);
			if (loadEntity == null) {
				loadEntity = new LoadEntity(runEntity, loadTypeEntity, loadNumber, apiEntity);
			}
			entityManager.persist(loadEntity);
			//
			final StatusEntity statusEntity = entityManager.find(StatusEntity.class, Integer.valueOf(message[3], 0x10));
			//entityManager.persist(statusEntity);
			//
			System.out.println("Connection");
			final long connectionNumber = Long.valueOf(
				event.getContextMap().get(DataObjectWorkerFactory.KEY_CONNECTION_NUM)
			);
			//
			ConnectionEntityPK connectionEntityPK = new ConnectionEntityPK(connectionNumber, loadEntityPK);
			//connectionEntity = new ConnectionEntity(loadEntity, nodeEntity, threadNumber);
			System.out.println(connectionEntityPK.getNum());
			connectionEntity = (ConnectionEntity) entityManager.find(ConnectionEntity.class, connectionEntityPK);
			if (connectionEntity == null) {
				connectionEntity = new ConnectionEntity(loadEntity, nodeEntity, connectionNumber);
			}
			entityManager.persist(connectionEntity);
			//
			System.out.println("data object");
			DataObjectEntityPK dataObjectEntityPK = new DataObjectEntityPK(message[1], Integer.valueOf(message[2]));
			DataObjectEntity dataObjectEntity = entityManager.find(DataObjectEntity.class, dataObjectEntityPK);
			if (dataObjectEntity == null){
				dataObjectEntity = new DataObjectEntity(message[1], Integer.valueOf(message[2]));
			}
			entityManager.merge(dataObjectEntity);
			//
			System.out.println("trace");
			TraceEntity traceEntity = new TraceEntity(dataObjectEntity, connectionEntity, statusEntity,
				Long.valueOf(message[4]), Long.valueOf(message[5]), Long.valueOf(message[6]));
			entityManager.persist(traceEntity);
			entityManager.getTransaction().commit();
			System.out.println("commit");
			entityManager.close();
		} catch (final RollbackException exception) {
			try {
				assert entityManager != null;
				entityManager.close();
			}catch (NullPointerException e){
				System.out.println("session is not close");
			}
			persistPerfTrace(message,event);
			System.out.println("Try one more time persist perfTrace " + Thread.currentThread().getName());
		} catch (final Exception e){
			e.printStackTrace();
		}


		/*
		loadTraceEntity(dataObjectEntity, connectionEntity, statusEntity,
			Long.valueOf(message[4]), Long.valueOf(message[5]), Long.valueOf(message[6]));
			*/
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////
	private void buildSessionFactory(
		final String username, final String password,
		final String url) {
		final Map<String, String> properties = new HashMap<>();
		properties.put("openjpa.ConnectionUserName", username);
		properties.put("openjpa.ConnectionPassword", password);
		properties.put("openjpa.ConnectionURL", url);
		ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory("openjpa", properties);
	}
	//parse String to Date
	private Date getTimestamp(final String stringTimestamp){
		Date runTimestamp = null;
		try {
			runTimestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(stringTimestamp);
		} catch (final ParseException e) {
			throw new IllegalStateException("Parse run timestamp is failed", e);
		}
		return runTimestamp;
	}
	///////////////////////////// Getters ////////////////////////////////////////////////////////
	private Object getEntity(final String colomnName, final Object value, final Class classEntity){
		final EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
		CriteriaBuilder queryBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery criteriaQuery = queryBuilder.createQuery();
		Root modeRoot = criteriaQuery.from(classEntity);
		criteriaQuery.select(modeRoot);
		Predicate predicate= queryBuilder.equal(modeRoot.get(colomnName), value);
		criteriaQuery.where(predicate);
		List result = entityManager.createQuery(criteriaQuery).getResultList();
		entityManager.close();
		if (result.size() > 1){
			throw new NonUniqueResultException(
				String.format("non unique result: count of results: %d",result.size()));
		}
		if (result.isEmpty()){
			return null;
		} else  return result.get(0);
	}

	//
	private Object getEntity(
		final String colomnName1, final Object value1,
		 final String colomnName2, final Object value2,
		 final Class classEntity)
	{
		final EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
		CriteriaBuilder queryBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery criteriaQuery = queryBuilder.createQuery();
		Root modeRoot = criteriaQuery.from(classEntity);
		criteriaQuery.select(modeRoot);
		Predicate predicate1= queryBuilder.equal(modeRoot.get(colomnName1), value1);
		Predicate predicate2 = queryBuilder.equal(modeRoot.get(colomnName2), value2);
		criteriaQuery.where(queryBuilder.and(predicate1,predicate2));
		List result = entityManager.createQuery(criteriaQuery).getResultList();
		entityManager.close();
		if (result.size() > 1){
			throw new NonUniqueResultException(
				String.format("non unique result: count of results: %d",result.size()));
		}
		if (result.isEmpty()){
			return null;
		} else  return result.get(0);
	}
	//
}
//