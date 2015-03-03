package com.emc.mongoose.util.persist;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.util.conf.RunTimeConfig;
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
import javax.persistence.Persistence;
import javax.persistence.Query;
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
import java.util.logging.Level;
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
						//System.out.println(event.getMessage().getFormattedMessage()+"     "+ event.getContextMap());
						persistMessages(event);
					}
					break;
				case DATA_LIST:
					//persistDataList(message);
					break;
				case PERF_TRACE:
					//persistPerfTrace(message, event);
					break;
			}

		}
	}
	//
	private void persistMessages(final LogEvent event)
	{
		final ModeEntity modeEntity = loadModeEntity(event.getContextMap().get(RunTimeConfig.KEY_RUN_MODE));
		/*
			final RunEntity runEntity = loadRunEntity(event.getContextMap().get(RunTimeConfig.KEY_RUN_ID),
				modeEntity, getTimestamp(event.getContextMap().get(RunTimeConfig.KEY_RUN_TIMESTAMP))
			);

		System.out.println("run persist");
		/*
		final LevelEntity levelEntity = loadLevelEntity(event.getLevel().toString());
		final MessageClassEntity messageClassEntity = loadClassOfMessage(event.getLoggerName());
		loadMessageEntity(new Date(event.getTimeMillis()),
			messageClassEntity, levelEntity, event.getMessage().getFormattedMessage(), runEntity);
		*/
	}
	//
	private void buildSessionFactory(
		final String username, final String password,
		final String url) {
		final Map<String, String> properties = new HashMap<>();
		properties.put("openjpa.ConnectionUserName", username);
		properties.put("openjpa.ConnectionPassword", password);
		properties.put("openjpa.ConnectionURL", url);
		ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory("openjpa", properties);
	}
	//
	private void persistStatusEntity()
	{
		for (final AsyncIOTask.Status result:AsyncIOTask.Status.values()){
			loadStatusEntity(result);
		}
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
	////////////////////////////////////////////Load methods////////////////////////////////////////
	private StatusEntity loadStatusEntity(final AsyncIOTask.Status result)
	{
		final StatusEntity statusEntity = new StatusEntity(result.code, result.description);
		final EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
		entityManager.getTransaction().begin();
		entityManager.merge(statusEntity);
		entityManager.getTransaction().commit();
		entityManager.close();
		return statusEntity;
	}
	//
	private ModeEntity loadModeEntity(final String modeName)
	{
		ModeEntity modeEntity = (ModeEntity) getEntity("name", modeName, ModeEntity.class);
		final EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
		try {
			if (modeEntity == null) {
				modeEntity = new ModeEntity(modeName);
				entityManager.getTransaction().begin();
				entityManager.merge(modeEntity);
				entityManager.getTransaction().commit();
			}
			entityManager.close();
		}catch (final RollbackException exception){
			entityManager.close();
			loadModeEntity(modeName);
			System.out.println("Try one more time: " + Thread.currentThread().getName());
		}
		return modeEntity;
	}
	//
	/*
	private RunEntity loadRunEntity(final String runName, final ModeEntity mode, final Date timestamp)
	{
		final RunEntity runEntity = new RunEntity(mode, runName, timestamp);
		final EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
		entityManager.getTransaction().begin();
		entityManager.merge(runEntity);
		entityManager.getTransaction().commit();
		entityManager.close();
		return runEntity;
	}
	*/
	///////////////////////////// Getters ////////////////////////////////////////////////////////
	private Object getEntity(final String colomnName, final String value, final Class classEntity){
		final EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
		CriteriaBuilder queryBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery criteriaQuery = queryBuilder.createQuery();
		Root modeRoot = criteriaQuery.from(classEntity);
		criteriaQuery.select(modeRoot);
		Predicate predicate= queryBuilder.equal(modeRoot.get(colomnName), value);
		criteriaQuery.where(predicate);
		List result = entityManager.createQuery(criteriaQuery).getResultList();
		if (result.isEmpty()){
			return null;
		} else  return result.get(0);

	}
}
//