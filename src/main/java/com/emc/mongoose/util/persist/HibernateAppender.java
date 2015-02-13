package com.emc.mongoose.util.persist;
//

import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.run.Main;
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
//
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
//
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
//
/**
 * Created by olga on 24.10.14.
 */
@Plugin(name="Hibernate", category="Core", elementType="appender", printObject=true)
public final class HibernateAppender
		extends AbstractAppender {
	//
	private static SessionFactory SESSION_FACTORY = null;
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
	public final void start()
	{
		super.start();
	}
	//
	@Override
	public final void stop()
	{
		super.stop();
		SESSION_FACTORY.close();
	}
	//
	private HibernateAppender(
			final String name, final Filter filter,
			final Layout<? extends Serializable> layout, final boolean ignoreExceptions)
	{
		super(name, filter, layout, ignoreExceptions);
	}
	//
	@PluginFactory
	public static HibernateAppender createAppender(
			final @PluginAttribute("name") String name,
			final @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
			final @PluginElement("Filters") Filter filter,
			final @PluginAttribute("enabled") Boolean enabled,
			final @PluginAttribute("database") String provider,
			final @PluginAttribute("username") String userName,
			final @PluginAttribute("password") String passWord,
			final @PluginAttribute("addr") String addr,
			final @PluginAttribute("port") String port,
			final @PluginAttribute("namedatabase") String dbName)
	{
		java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.OFF);
		HibernateAppender newAppender = null;
		ENABLED_FLAG = enabled;
		if(name == null) {
			throw new IllegalArgumentException("No name provided for HibernateAppender");
		}
		final String url = String.format("jdbc:%s://%s:%s/%s", provider, addr, port, dbName);
		try {
			newAppender = new HibernateAppender(name, filter, DEFAULT_LAYOUT, ignoreExceptions);
			if(ENABLED_FLAG) {
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
	public final void append(final LogEvent event)
	{
		if (ENABLED_FLAG){
			final String marker = event.getMarker().toString();
			final String[] message = event.getMessage().getFormattedMessage().split("\\s*[,|/]\\s*");
			switch (marker) {
				case MSG:
				case ERR:
					persistMessages(event);
					break;
				case DATA_LIST:
					persistDataList(message);
					break;
				case PERF_TRACE:
					persistPerfTrace(message, event);
					break;
			}

		}
	}
	//
	private void buildSessionFactory(
			final String username, final String password,
			final String url)
	{
		final String DIR_ROOT = System.getProperty("user.dir");
		final Path path = Paths.get(DIR_ROOT, "conf", "hibernate.cfg.xml");
		final File hibernateConfFile = new File(path.toString());
		try {
			// Create the SessionFactory from hibernate.cfg.xml
			final Configuration configuration = new Configuration()
					.configure(hibernateConfFile)
					.setProperty("hibernate.connection.password", password)
					.setProperty("hibernate.connection.username", username)
					.setProperty("hibernate.connection.url", url);
			final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder()
					.applySettings(configuration.getProperties());
			SESSION_FACTORY = configuration.buildSessionFactory(ssrb.build());
		}
		catch(final Exception e) {
			// Make sure you log the exception, as it might be swallowed
			throw new ExceptionInInitializerError(e);
		}
	}
	//
	private void persistStatusEntity()
	{
		for (final AsyncIOTask.Status result:AsyncIOTask.Status.values()){
			loadStatusEntity(result);
		}
	}
	//
	private void persistMessages(final LogEvent event)
	{
		final ModeEntity modeEntity = loadModeEntity(event.getContextMap().get(RunTimeConfig.KEY_RUN_MODE));
		final RunEntity runEntity = loadRunEntity(event.getContextMap().get(RunTimeConfig.KEY_RUN_ID),
			modeEntity,getTimestamp(event.getContextMap().get(Main.KEY_RUN_TIMESTAMP))
		);
		final LevelEntity levelEntity = loadLevelEntity(event.getLevel().toString());
		final MessageClassEntity messageClassEntity = loadClassOfMessage(event.getLoggerName());
		loadMessageEntity(new Date(event.getTimeMillis()),
				messageClassEntity, levelEntity, event.getMessage().getFormattedMessage(), runEntity);
	}
	//
	private void persistDataList(final String[] message)
	{
		DataObjectEntity dataObjectEntity = new DataObjectEntity( message[0], message[1],
				Long.valueOf(message[2], 0x10), Long.valueOf(message[3], 0x10),
				Long.valueOf(message[4], 0x10));
		loadDataObjectEntity(dataObjectEntity);


	}
	//
	private void persistPerfTrace(final String[] message, final LogEvent event)
	{
		final DataObjectEntity dataObjectEntity = new DataObjectEntity(message[1], Integer.valueOf(message[2]));
		loadDataObjectEntity(dataObjectEntity);
		final StatusEntity statusEntity = getStatusEntity(Integer.valueOf(message[3], 0x10));
		final RunEntity runEntity = getRunEntity(getTimestamp(
			event.getContextMap().get(Main.KEY_RUN_TIMESTAMP)));
		final LoadTypeEntity loadTypeEntity = loadLoadTypeEntity(
			event.getContextMap().get(DataObjectWorkerFactory.KEY_LOAD_TYPE));
		final ApiEntity apiEntity = loadApiEntity(
			event.getContextMap().get(DataObjectWorkerFactory.KEY_API));
		final LoadEntity loadEntity = loadLoadEntity(
			event.getContextMap().get(DataObjectWorkerFactory.KEY_LOAD_NUM),
			runEntity, loadTypeEntity, apiEntity);
		final NodeEntity nodeEntity = loadNodeEntity(message[0]);
		final ConnectionEntity connectionEntity = loadConnectionEntity(
			event.getContextMap().get(DataObjectWorkerFactory.KEY_THREAD_NUM), loadEntity, nodeEntity);
		loadTraceEntity(dataObjectEntity, connectionEntity, statusEntity,
			Long.valueOf(message[5]), Long.valueOf(message[6]), Long.valueOf(message[7]));
	}
	//parse String to Date
	private Date getTimestamp(final String stringTimestamp){
		Date runTimestamp = null;
		try {
			runTimestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(stringTimestamp);
		} catch (final ParseException e) {
			e.printStackTrace();
		}
		return runTimestamp;
	}
	// Load methods
	private ModeEntity loadModeEntity(final String modeName)
	{
		ModeEntity modeEntity = getModeEntity(modeName);
		Session session = SESSION_FACTORY.openSession();
		try {
			if (modeEntity == null) {
				modeEntity = new ModeEntity(modeName);
				session.beginTransaction();
				session.save(modeEntity);
				session.getTransaction().commit();
				session.flush();
			}
			session.close();
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return getModeEntity(modeName);
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			session.close();
		}
		return modeEntity;
	}
	//
	private RunEntity loadRunEntity(final String runName, final ModeEntity mode, final Date timestamp)
	{
		RunEntity runEntity = getRunEntity(timestamp);
		Session session = SESSION_FACTORY.openSession();
		try {
			if (runEntity == null) {
				runEntity = new RunEntity(mode, runName, timestamp);
				session.beginTransaction();
				session.save(runEntity);
				session.getTransaction().commit();
				session.flush();
			}
			session.close();
		} catch (final ConstraintViolationException e) {
			session.getTransaction().rollback();
			session.close();
			return getRunEntity(timestamp);
		} catch (final HibernateException e) {
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return runEntity;
	}
	//
	private ApiEntity loadApiEntity(final String apiName)
	{
		ApiEntity apiEntity = getApiEntity(apiName);
		Session session = SESSION_FACTORY.openSession();
		try {
			if (apiEntity == null) {
				apiEntity = new ApiEntity(apiName);
				session.beginTransaction();
				session.save(apiEntity);
				session.getTransaction().commit();
				session.flush();
			}
			session.close();
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return getApiEntity(apiName);
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return apiEntity;
	}
	//
	private LoadTypeEntity loadLoadTypeEntity(final String typeName)
	{
		LoadTypeEntity loadTypeEntity = getLoadTypeEntity(typeName);
		Session session = SESSION_FACTORY.openSession();
		try {
			if (loadTypeEntity == null) {
				loadTypeEntity = new LoadTypeEntity(typeName);
				session.beginTransaction();
				session.save(loadTypeEntity);
				session.getTransaction().commit();
				session.flush();
			}
			session.close();
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return getLoadTypeEntity(typeName);
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return loadTypeEntity;
	}
	//
	private LoadEntity loadLoadEntity(final String loadNumberString, final RunEntity run,
		final LoadTypeEntity type, final ApiEntity api)
	{
		LoadEntity loadEntity = new LoadEntity(run, type, Long.valueOf(loadNumberString), api);
		Session session = SESSION_FACTORY.openSession();
		try {
			session.beginTransaction();
			session.save(loadEntity);
			session.getTransaction().commit();
			session.flush();
			session.close();
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return loadEntity;
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return loadEntity;
	}
	//
	private MessageClassEntity loadClassOfMessage(final String className)
	{
		MessageClassEntity messageClassEntity = getMessageClassEntity(className);
		Session session = SESSION_FACTORY.openSession();
		try {
			if (messageClassEntity == null) {
				messageClassEntity = new MessageClassEntity(className);
				session.beginTransaction();
				session.save(messageClassEntity);
				session.getTransaction().commit();
				session.flush();
			}
			session.close();
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return getMessageClassEntity(className);
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return messageClassEntity;
	}
	//
	private LevelEntity loadLevelEntity(final String levelName)
	{
		LevelEntity levelEntity = getLevelEntity(levelName);
		Session session = SESSION_FACTORY.openSession();
		try {
			if (levelEntity == null) {
				levelEntity = new LevelEntity(levelName);
				session.beginTransaction();
				session.save(levelEntity);
				session.getTransaction().commit();
				session.flush();
			}
			session.close();
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return getLevelEntity(levelName);
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return levelEntity;
	}
	//
	private NodeEntity loadNodeEntity(final String nodeAddr)
	{
		NodeEntity nodeEntity = getNodeEntity(nodeAddr);
		Session session = SESSION_FACTORY.openSession();
		try {
			if (nodeEntity == null){
				nodeEntity = new NodeEntity(nodeAddr);
				session.beginTransaction();
				session.save(nodeEntity);
				session.getTransaction().commit();
				session.flush();
			}
			session.close();
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return getNodeEntity(nodeAddr);
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return nodeEntity;
	}
	//If node is known
	private ConnectionEntity loadConnectionEntity(final String threadNumberString,
														final LoadEntity load, final NodeEntity node)
	{
		ConnectionEntity connectionEntity = new ConnectionEntity(load, node, Long.valueOf(threadNumberString));
		Session session = SESSION_FACTORY.openSession();
		try {
			session.beginTransaction();
			session.save(connectionEntity);
			session.getTransaction().commit();
			session.flush();
			session.close();
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return connectionEntity;
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return connectionEntity;
	}
	//
	private DataObjectEntity loadDataObjectEntity(final DataObjectEntity dataObjectEntity)
	{
		Session session = SESSION_FACTORY.openSession();
		final DataObjectEntityPK dataObjectEntityPK = new DataObjectEntityPK(
			dataObjectEntity.getIdentifier(),dataObjectEntity.getSize());
		try{
			session.beginTransaction();
			final DataObjectEntity equalDataObject =(DataObjectEntity) session.get(
				DataObjectEntity.class, dataObjectEntityPK);
			if (equalDataObject == null) {
				session.save(dataObjectEntity);
			} else {
				if (dataObjectEntity.getRingOffset() != null) {
					session.merge(dataObjectEntity);
				}
			}
			session.getTransaction().commit();
			session.flush();
			session.close();
		}catch(final ConstraintViolationException e) {
			session.getTransaction().rollback();
			session.close();
			loadDataObjectEntity(dataObjectEntity);
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return dataObjectEntity;
	}
	//
	private StatusEntity loadStatusEntity(final AsyncIOTask.Status result)
	{
		StatusEntity statusEntity = new StatusEntity(result.code, result.description);
		Session session = SESSION_FACTORY.openSession();
		try {
			session.beginTransaction();
			session.saveOrUpdate(statusEntity);
			session.getTransaction().commit();
			session.flush();
			session.close();
		}catch (HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return statusEntity;
	}
	//
	private MessageEntity loadMessageEntity(final Date tstamp, final MessageClassEntity messageClassEntity,
			final LevelEntity levelEntity, final String text, final RunEntity run)
	{
		MessageEntity messageEntity = new MessageEntity(run, messageClassEntity, levelEntity, text, tstamp);
		Session session = SESSION_FACTORY.openSession();
		try {
			session.beginTransaction();
			session.save(messageEntity);
			session.getTransaction().commit();
			session.flush();
			session.close();
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return messageEntity;
	}
	//
	private TraceEntity loadTraceEntity(final DataObjectEntity dataObjectEntity,
			final ConnectionEntity connectionEntity, final StatusEntity statusEntity,
			final long reqStart, final long latency, final long reqDur)
	{
		TraceEntity traceEntity = new TraceEntity(dataObjectEntity, connectionEntity, statusEntity, reqStart, latency, reqDur);
		Session session = SESSION_FACTORY.openSession();
		try {
			session.beginTransaction();
			session.save(traceEntity);
			session.getTransaction().commit();
			session.flush();
			session.close();
		}catch (final HibernateException e){
			session.getTransaction().rollback();
			e.printStackTrace();
			session.close();
		}
		return traceEntity;
	}
	//
	private RunEntity getRunEntity(final Date timestamp)
	{
		Session session = SESSION_FACTORY.openSession();
		RunEntity runEntity = null;
		try {
			runEntity = (RunEntity) session.createCriteria(RunEntity.class)
					.setCacheable(true)
					.add(Restrictions.eq("timestamp", timestamp))
					.uniqueResult();
			session.close();
		} catch (final HibernateException e) {
			e.printStackTrace();
			session.close();
		}
		return runEntity;
	}
	private static StatusEntity getStatusEntity(final int codeStatus)
	{
		Session session = SESSION_FACTORY.openSession();
		StatusEntity statusEntity = null;
		try {
			statusEntity = (StatusEntity) session.get(StatusEntity.class, codeStatus);
			session.close();
		}catch (final HibernateException e){
			e.printStackTrace();
			session.close();
		}
		return statusEntity;
	}
	//
	private ModeEntity getModeEntity(final String modeName)
	{
		Session session = SESSION_FACTORY.openSession();
		ModeEntity modeEntity = null;
		try {
			modeEntity = (ModeEntity) session.createCriteria(ModeEntity.class)
					.setCacheable(true)
					.add(Restrictions.eq("name", modeName))
					.uniqueResult();
			session.close();
		}catch (final HibernateException e){
			e.printStackTrace();
			session.close();
		}
		return modeEntity;
	}
	//
	private ApiEntity getApiEntity(final String apiName)
	{
		Session session = SESSION_FACTORY.openSession();
		ApiEntity apiEntity = null;
		try {
			apiEntity = (ApiEntity) session.createCriteria(ApiEntity.class)
					.setCacheable(true)
					.add(Restrictions.eq("name", apiName))
					.uniqueResult();
			session.close();
		}catch (final HibernateException e){
			e.printStackTrace();
			session.close();
		}
		return apiEntity;
	}
	//
	private LoadTypeEntity getLoadTypeEntity(final String typeName)
	{
		Session session = SESSION_FACTORY.openSession();
		LoadTypeEntity loadTypeEntity = null;
		try {
			loadTypeEntity = (LoadTypeEntity) session.createCriteria(LoadTypeEntity.class)
					.setCacheable(true)
					.add(Restrictions.eq("name", typeName))
					.uniqueResult();
			session.close();
		}catch (final HibernateException e){
			e.printStackTrace();
			session.close();
		}
		return loadTypeEntity;
	}
	//
	private MessageClassEntity getMessageClassEntity(final String className)
	{
		Session session = SESSION_FACTORY.openSession();
		MessageClassEntity messageClassEntity = null;
		try {
			messageClassEntity = (MessageClassEntity) session.createCriteria(MessageClassEntity.class)
					.setCacheable(true)
					.add(Restrictions.eq("name", className))
					.uniqueResult();
			session.close();
		}catch (final HibernateException e){
			e.printStackTrace();
			session.close();
		}
		return messageClassEntity;
	}
	//
	private LevelEntity getLevelEntity(final String levelName)
	{
		Session session = SESSION_FACTORY.openSession();
		LevelEntity levelEntity = null;
		try {
			levelEntity = (LevelEntity) session.createCriteria(LevelEntity.class)
					.setCacheable(true)
					.add(Restrictions.eq("name", levelName))
					.uniqueResult();
			session.close();
		}catch (final HibernateException e){
			e.printStackTrace();
			session.close();
		}
		return levelEntity;
	}
	//
	private NodeEntity getNodeEntity(final String nodeAddr)
	{
		Session session = SESSION_FACTORY.openSession();
		NodeEntity nodeEntity = null;
		try {
			nodeEntity = (NodeEntity) session.createCriteria(NodeEntity.class)
					.setCacheable(true)
					.add(Restrictions.eq("address", nodeAddr))
					.uniqueResult();
			session.close();
		}catch (final HibernateException e){
			e.printStackTrace();
			session.close();
		}
		return  nodeEntity;
	}
}
