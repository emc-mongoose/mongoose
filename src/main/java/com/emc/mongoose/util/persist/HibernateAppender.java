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
		for (final Request.Result result:Request.Result.values()){
			loadStatusEntity(result);
		}
	}
	//
	private void persistMessages(final LogEvent event)
	{
		final ModeEntity modeEntity = loadModeEntity(event.getContextMap().get(KEY_RUN_MODE));
		final RunEntity runEntity = loadRunEntity(event.getContextMap().get(KEY_RUN_ID), modeEntity);
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
		final DataObjectEntity dataObjectEntity = new DataObjectEntity(message[0], Integer.valueOf(message[1], 0x10));
		loadDataObjectEntity(dataObjectEntity);
		final StatusEntity statusEntity = getStatusEntity(Integer.valueOf(message[2], 0x10));
		final RunEntity runEntity = getRunEntity(event.getContextMap().get(KEY_RUN_ID));
		final LoadTypeEntity loadTypeEntity = loadLoadTypeEntity(event.getContextMap().get(KEY_LOAD_TYPE));
		final ApiEntity apiEntity = loadApiEntity(event.getContextMap().get(KEY_API));
		final LoadEntity loadEntity = loadLoadEntity(event.getContextMap().get(KEY_LOAD_NUM),
				runEntity, loadTypeEntity, apiEntity);
		final NodeEntity nodeEntity = loadNodeEntity(event.getContextMap().get(KEY_NODE_ADDR));
		final ConectionEntity conectionEntity = loadConnectionEntity(event.getContextMap().get(KEY_THREAD_NUM), loadEntity, nodeEntity);
		/*



		loadTraceEntity(session, dataObjectEntity, conectionEntity, statusEntity, Long.valueOf(message[3], 0x10), Long.valueOf(message[4], 0x10));
		*/
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
			}
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return getModeEntity(modeName);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return modeEntity;
	}
	//
	private RunEntity loadRunEntity(final String runName, final ModeEntity mode)
	{
		RunEntity runEntity = getRunEntity(runName);
		Session session = SESSION_FACTORY.openSession();
		try {
			if (runEntity == null){
				runEntity = new RunEntity(mode, runName);
				session.beginTransaction();
				session.save(runEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return getRunEntity(runName);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return runEntity;
	}
	//
	private ApiEntity loadApiEntity(final String apiName)
	{
		ApiEntity apiEntity = null;
		Session session = SESSION_FACTORY.openSession();
		try {
			apiEntity = getApiEntity(session, apiName);
			if (apiEntity == null) {
				apiEntity = new ApiEntity(apiName);
				session.beginTransaction();
				session.save(apiEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			return getApiEntity(session, apiName);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return apiEntity;
	}
	//
	private LoadTypeEntity loadLoadTypeEntity(final String typeName)
	{
		LoadTypeEntity loadTypeEntity = null;
		Session session = SESSION_FACTORY.openSession();
		try {
			loadTypeEntity = getLoadTypeEntity(session, typeName);
			if (loadTypeEntity == null) {
				loadTypeEntity = new LoadTypeEntity(typeName);
				session.beginTransaction();
				session.save(loadTypeEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			return getLoadTypeEntity(session, typeName);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
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
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return loadEntity;
	}
	//
	private MessageClassEntity loadClassOfMessage(final String className)
	{
		MessageClassEntity messageClassEntity = null;
		Session session = SESSION_FACTORY.openSession();
		try {
			messageClassEntity = getMessageClassEntity(session, className);
			if (messageClassEntity == null) {
				messageClassEntity = new MessageClassEntity(className);
				session.beginTransaction();
				session.save(messageClassEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			return getMessageClassEntity(session, className);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return messageClassEntity;
	}
	//
	private LevelEntity loadLevelEntity(final String levelName)
	{
		LevelEntity levelEntity = null;
		Session session = SESSION_FACTORY.openSession();
		try {
			levelEntity = getLevelEntity(session, levelName);
			if (levelEntity == null) {
				levelEntity = new LevelEntity(levelName);
				session.beginTransaction();
				session.save(levelEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			return getLevelEntity(session, levelName);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return levelEntity;
	}
	//
	private NodeEntity loadNodeEntity(final String nodeAddr)
	{
		NodeEntity nodeEntity = null;
		Session session = SESSION_FACTORY.openSession();
		try {
			nodeEntity = getNodeEntity(session, nodeAddr);
			if (nodeEntity == null){
				nodeEntity = new NodeEntity(nodeAddr);
				session.beginTransaction();
				session.save(nodeEntity);
				session.getTransaction().commit();
			}
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			//??
			session.close();
			return getNodeEntity(session, nodeAddr);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return nodeEntity;
	}
	//If node is known
	private ConectionEntity loadConnectionEntity(final String threadNumberString,
														final LoadEntity load, final NodeEntity node)
	{
		ConectionEntity conectionEntity = new ConectionEntity(load, node, Long.valueOf(threadNumberString));
		Session session = SESSION_FACTORY.openSession();
		try {
			session.beginTransaction();
			session.save(conectionEntity);
			session.getTransaction().commit();
		}catch (final ConstraintViolationException e){
			session.getTransaction().rollback();
			session.close();
			return conectionEntity;
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return conectionEntity;
	}
	//
	private DataObjectEntity loadDataObjectEntity(final DataObjectEntity dataObjectEntity)
	{
		Session session = SESSION_FACTORY.openSession();
		try{
			session.beginTransaction();
			session.saveOrUpdate(dataObjectEntity);
			session.getTransaction().commit();
		}catch(final ConstraintViolationException e) {
			session.getTransaction().rollback();
			session.close();
			loadDataObjectEntity(dataObjectEntity);
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return dataObjectEntity;
	}
	//
	private StatusEntity loadStatusEntity(final Request.Result result)
	{
		StatusEntity statusEntity = new StatusEntity(result.code, result.description);
		Session session = SESSION_FACTORY.openSession();
		try {
			session.beginTransaction();
			session.saveOrUpdate(statusEntity);
			session.getTransaction().commit();
		}catch (Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
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
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return messageEntity;
	}
	//
	private TraceEntity loadTraceEntity(final DataObjectEntity dataObjectEntity,
			final ConectionEntity conectionEntity, final StatusEntity statusEntity,
			final long reqStart, final long reqDur)
	{
		TraceEntity traceEntity = new TraceEntity(dataObjectEntity, conectionEntity, statusEntity, reqStart, reqDur);
		Session session = SESSION_FACTORY.openSession();
		try {
			session.beginTransaction();
			session.save(traceEntity);
			session.getTransaction().commit();
		}catch (final Exception e){
			session.getTransaction().rollback();
			e.printStackTrace();
		}finally {
			session.flush();
			session.close();
		}
		return traceEntity;
	}
	//
	private RunEntity getRunEntity(final String runName)
	{
		Session session = SESSION_FACTORY.openSession();
		RunEntity runEntity = (RunEntity) session.createCriteria(RunEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", runName))
				.uniqueResult();
		session.close();
		return runEntity;
	}
	private static StatusEntity getStatusEntity(final int codeStatus)
	{
		Session session = SESSION_FACTORY.openSession();
		StatusEntity statusEntity =(StatusEntity) session.get(StatusEntity.class, codeStatus);
		session.close();
		return statusEntity;
	}
	//
	private ModeEntity getModeEntity(final String modeName)
	{
		Session session = SESSION_FACTORY.openSession();
		ModeEntity modeEntity = (ModeEntity) session.createCriteria(ModeEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", modeName))
				.uniqueResult();
		session.close();
		return modeEntity;
	}
	//
	private ApiEntity getApiEntity(final Session session, final String apiName)
	{
		return (ApiEntity) session.createCriteria(ApiEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", apiName))
				.uniqueResult();
	}
	//
	private LoadTypeEntity getLoadTypeEntity(final Session session, final String typeName)
	{
		return (LoadTypeEntity) session.createCriteria(LoadTypeEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", typeName))
				.uniqueResult();
	}
	//
	private MessageClassEntity getMessageClassEntity(final Session session, final String className)
	{
		return (MessageClassEntity) session.createCriteria(MessageClassEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", className))
				.uniqueResult();
	}
	//
	private LevelEntity getLevelEntity(final Session session, final String levelName)
	{
		return (LevelEntity) session.createCriteria(LevelEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("name", levelName))
				.uniqueResult();
	}
	//
	private NodeEntity getNodeEntity(final Session session, final String nodeAddr)
	{
		return (NodeEntity) session.createCriteria(NodeEntity.class)
				.setCacheable(true)
				.add(Restrictions.eq("address", nodeAddr))
				.uniqueResult();
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
