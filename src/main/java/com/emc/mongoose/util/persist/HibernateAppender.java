package com.emc.mongoose.util.persist;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.object.data.DataObject;
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
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
//
import java.io.File;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	private volatile boolean flag = false;
	private static final String
			MSG = "msg",
			PERF_TRACE = "perfTrace",
			ERR = "err",
			DATA_LIST = "dataList",
			KEY_NODE_ADDR = "node.addr",
			KEY_THREAD_NUM = "thread.number",
			KEY_LOAD_NUM = "load.number",
			KEY_LOAD_TYPE = "load.type",
			KEY_API = "api",
			KEY_RUN_ID = "run.id",
			KEY_RUN_MODE = "run.mode";
	//
	@Override
	public final void start()
	{
		super.start();
		Runtime.getRuntime().addShutdownHook(new ShutDownThread(this));
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
			if(ENABLED_FLAG) {
				// init database session with username,password and url
				buildSessionFactory(userName, passWord, url);
				persistStatusEntity();
			}
			newAppender = new HibernateAppender(name, filter, DEFAULT_LAYOUT, ignoreExceptions);
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
			Session session = SESSION_FACTORY.openSession();
			final String marker = event.getMarker().toString();
			final String[] message = event.getMessage().getFormattedMessage().split("\\s*[,|/]\\s*");
			switch (marker) {
				case MSG:
				case ERR:
					persistMessages(event, session);
					session.flush();
					session.close();
					break;
				case DATA_LIST:
					persistDataList(message, session);
					session.flush();
					session.close();
					break;
				case PERF_TRACE:
					//persistPerfTrace(message, session, event);
					session.flush();
					session.close();
					break;
			}

		}
	}
	//
	private static void buildSessionFactory(
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
	private static void persistStatusEntity()
	{
		final Session session = SESSION_FACTORY.openSession();
		for (final Request.Result result:Request.Result.values()){
			loadStatusEntity(session, result);
		}
		session.close();
	}
	//
	private void persistMessages(final LogEvent event, final Session session)
	{
		final ModeEntity modeEntity = loadModeEntity(session, event.getContextMap().get(KEY_RUN_MODE));
		final RunEntity runEntity = loadRunEntity(session, event.getContextMap().get(KEY_RUN_ID), modeEntity);
		final LevelEntity levelEntity = loadLevelEntity(session, event.getLevel().toString());
		final MessageClassEntity messageClassEntity = loadClassOfMessage(session, event.getLoggerName());
		loadMessageEntity(session, new Date(event.getTimeMillis()),
				messageClassEntity, levelEntity, event.getMessage().getFormattedMessage(), runEntity);
	}
	//
	private static void persistDataList(final String[] message, final Session session)
	{
		DataObjectEntity dataObjectEntity = new DataObjectEntity( message[0], message[1],
				Long.valueOf(message[2], 0x10), Long.valueOf(message[3], 0x10),
				Long.valueOf(message[4], 0x10));
		//loadDataObjectEntity(session, dataObjectEntity);
		saveOrUpdate(session,dataObjectEntity);

	}
	//
	private static void persistPerfTrace(final String[] message, final Session session, final LogEvent event)
	{
		final DataObjectEntity dataObjectEntity = new DataObjectEntity(message[0], Integer.valueOf(message[1], 0x10));
		saveOrUpdate(session,dataObjectEntity);
		//loadDataObjectEntity(session, dataObjectEntity);
		/*
		final StatusEntity statusEntity = getStatusEntity(session, Integer.valueOf(message[2], 0x10));
		final RunEntity runEntity = getRunEntity(session, event.getContextMap().get(KEY_RUN_ID));
		final LoadTypeEntity loadTypeEntity = loadLoadTypeEntity(session,
				event.getContextMap().get(KEY_LOAD_TYPE));
		final ApiEntity apiEntity = loadApiEntity(session, event.getContextMap().get(KEY_API));
		final LoadEntity loadEntity = loadLoadEntity(session, event.getContextMap().get(KEY_LOAD_NUM),
				runEntity, loadTypeEntity, apiEntity);
		final NodeEntity nodeEntity = loadNodeEntity(session, event.getContextMap().get(KEY_NODE_ADDR));
		final ConectionEntity conectionEntity = loadConnectionEntity(session,
				event.getContextMap().get(KEY_THREAD_NUM), loadEntity, nodeEntity);
		loadTraceEntity(session, dataObjectEntity, conectionEntity, statusEntity, Long.valueOf(message[3], 0x10), Long.valueOf(message[4], 0x10));
		*/
	}
	// Load methods
	private static ModeEntity loadModeEntity(final Session session, final String modeName)
	{
		ModeEntity modeEntity = null;
		try {
			modeEntity = getModeEntity(session, modeName);
			if (modeEntity == null) {
				modeEntity = new ModeEntity(modeName);
				session.beginTransaction();
				session.save(modeEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			return getModeEntity(session, modeName);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return modeEntity;
	}
	//
	private static RunEntity loadRunEntity(final Session session, final String runName, final ModeEntity mode)
	{

		RunEntity runEntity = null;
		try {
			runEntity = getRunEntity(session, runName);
			if (runEntity == null){
				runEntity = new RunEntity(mode, runName);
				session.beginTransaction();
				session.saveOrUpdate(runEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			return getRunEntity(session, runName);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return runEntity;
	}
	//
	private static ApiEntity loadApiEntity(final Session session, final String apiName)
	{
		ApiEntity apiEntity = null;
		try {
			apiEntity = getApiEntity(session, apiName);
			if (apiEntity == null) {
				apiEntity = new ApiEntity(apiName);
				session.beginTransaction();
				session.saveOrUpdate(apiEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			return getApiEntity(session, apiName);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return apiEntity;
	}
	//
	private static LoadTypeEntity loadLoadTypeEntity(final Session session, final String typeName)
	{
		LoadTypeEntity loadTypeEntity = null;
		try {
			loadTypeEntity = getLoadTypeEntity(session, typeName);
			if (loadTypeEntity == null) {
				loadTypeEntity = new LoadTypeEntity(typeName);
				session.beginTransaction();
				session.saveOrUpdate(loadTypeEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			return getLoadTypeEntity(session, typeName);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return loadTypeEntity;
	}
	//???
	private static LoadEntity loadLoadEntity(final Session session, final String loadNumberString, final RunEntity run,
											 final LoadTypeEntity type, final ApiEntity api)
	{
		LoadEntity loadEntity = new LoadEntity(run, type, Long.valueOf(loadNumberString), api);
		try {
			session.beginTransaction();
			session.saveOrUpdate(loadEntity);
			session.getTransaction().commit();
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return loadEntity;
	}
	//
	private static MessageClassEntity loadClassOfMessage(final Session session, final String className)
	{
		MessageClassEntity messageClassEntity = null;
		try {
			messageClassEntity = getMessageClassEntity(session, className);
			if (messageClassEntity == null) {
				messageClassEntity = new MessageClassEntity(className);
				session.beginTransaction();
				session.saveOrUpdate(messageClassEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			return getMessageClassEntity(session, className);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return messageClassEntity;
	}
	//
	private static LevelEntity loadLevelEntity(final Session session, final String levelName)
	{
		LevelEntity levelEntity = null;
		try {
			levelEntity = getLevelEntity(session, levelName);
			if (levelEntity == null) {
				levelEntity = new LevelEntity(levelName);
				session.beginTransaction();
				session.saveOrUpdate(levelEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			return getLevelEntity(session, levelName);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return levelEntity;
	}
	//
	private static NodeEntity loadNodeEntity(final Session session, final String nodeAddr)
	{
		NodeEntity nodeEntity = null;
		try {
			nodeEntity = getNodeEntity(session, nodeAddr);
			if (nodeEntity == null){
				nodeEntity = new NodeEntity(nodeAddr);
				session.beginTransaction();
				session.saveOrUpdate(nodeEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			return getNodeEntity(session, nodeAddr);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return nodeEntity;
	}
	//If node is known
	private static ConectionEntity loadConnectionEntity(final Session session, final String threadNumberString,
														final LoadEntity load, final NodeEntity node)
	{
		ConectionEntity conectionEntity = new ConectionEntity(load, node, Long.valueOf(threadNumberString));
		try {
			session.beginTransaction();
			session.saveOrUpdate(conectionEntity);
			session.getTransaction().commit();
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return conectionEntity;
	}
	//
	private static DataObjectEntity loadDataObjectEntity(final Session session,
														 final DataObjectEntity dataObjectEntity)
	{
		saveOrUpdate(session, dataObjectEntity);
		return dataObjectEntity;
	}
	//
	private static StatusEntity loadStatusEntity(final Session session, final Request.Result result)
	{
		StatusEntity statusEntity = new StatusEntity(result.code, result.description);
		try {
			session.beginTransaction();
			session.saveOrUpdate(statusEntity);
			session.getTransaction().commit();
		}catch (Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return statusEntity;
	}
	//
	private static MessageEntity loadMessageEntity(final Session session, final Date tstamp,
												   final MessageClassEntity messageClassEntity, final LevelEntity levelEntity,
												   final String text, final RunEntity run)
	{
		MessageEntity messageEntity = new MessageEntity(run, messageClassEntity, levelEntity, text, tstamp);
		try {
			session.beginTransaction();
			session.save(messageEntity);
			session.getTransaction().commit();
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return messageEntity;
	}
	//
	private static TraceEntity loadTraceEntity(final Session session, final DataObjectEntity dataObjectEntity,
											   final ConectionEntity conectionEntity, final StatusEntity statusEntity, final long reqStart, final long reqDur)
	{
		TraceEntity traceEntity = new TraceEntity(dataObjectEntity, conectionEntity, statusEntity, reqStart, reqDur);
		try {
			session.beginTransaction();
			session.saveOrUpdate(traceEntity);
			session.getTransaction().commit();
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
		return traceEntity;
	}
	//
	private static RunEntity getRunEntity(final Session session,final String runName)
	{
		return (RunEntity) session.createCriteria(RunEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", runName))
				.uniqueResult();
	}
	private static StatusEntity getStatusEntity(final Session session, final int codeStatus)
	{
		return (StatusEntity) session.get(StatusEntity.class, codeStatus);
	}
	//
	private static ModeEntity getModeEntity(final Session session, final String modeName)
	{
		return (ModeEntity) session.createCriteria(ModeEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", modeName))
				.uniqueResult();
	}
	//
	private static ApiEntity getApiEntity(final Session session, final String apiName)
	{
		return (ApiEntity) session.createCriteria(ApiEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", apiName))
				.uniqueResult();
	}
	//
	private static LoadTypeEntity getLoadTypeEntity(final Session session, final String typeName)
	{
		return (LoadTypeEntity) session.createCriteria(LoadTypeEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", typeName))
				.uniqueResult();
	}
	//
	private static MessageClassEntity getMessageClassEntity(final Session session, final String className)
	{
		return (MessageClassEntity) session.createCriteria(MessageClassEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", className))
				.uniqueResult();
	}
	//
	private static LevelEntity getLevelEntity(final Session session, final String levelName)
	{
		return (LevelEntity) session.createCriteria(LevelEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", levelName))
				.uniqueResult();
	}
	//
	private static NodeEntity getNodeEntity(final Session session, final String nodeAddr)
	{
		return (NodeEntity) session.createCriteria(NodeEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("address", nodeAddr))
				.uniqueResult();
	}
	private static void saveOrUpdate(final Session session, final DataObjectEntity dataObjectEntity){
		try {
			session.beginTransaction();
			Query query = session.createQuery("select merge_do(:identifier,:size,:ringoffset,:layer,:mask)")
				.setParameter("identifier", dataObjectEntity.getIdentifier())
				.setParameter("size", dataObjectEntity.getSize())
				.setString("ringoffset", dataObjectEntity.getRingOffset())
				.setParameter("layer", dataObjectEntity.getLayer())
				.setParameter("mask", dataObjectEntity.getMask());
			session.getTransaction().commit();
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}
	}
	/////////////////////////////////////
	private final class ShutDownThread
			extends Thread
	{
		private final AbstractAppender appender;
		//
		public ShutDownThread(final HibernateAppender appender)
		{
			super("HibernateShutDown");
			this.appender = appender;
		}

		@Override
		public final void run()
		{
			try {
				getThread("main").join(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			appender.stop();
		}

		private Thread[] getAllThreads( )
		{
			final ThreadGroup tg = Thread.currentThread( ).getThreadGroup();
			final ThreadGroup parentThread = tg.getParent();
			final ThreadMXBean thbean = ManagementFactory.getThreadMXBean();
			int nAlloc = thbean.getThreadCount( );
			int n = 0;
			Thread[] threads;
			do {
				nAlloc *= 2;
				threads = new Thread[ nAlloc ];
				n = parentThread.enumerate( threads, true );
			} while ( n == nAlloc );
			return java.util.Arrays.copyOf( threads, n );
		}

		private Thread getThread( final String name )
		{
			if ( name == null )
				throw new NullPointerException( "Null name" );
			final Thread[] threads = getAllThreads( );
			for ( Thread thread : threads )
				if ( thread.getName( ).equals( name ) )
					return thread;
			return null;
		}
	}
	//////////////////////////////////////
}
