package com.emc.mongoose.util.persist;
//
import com.emc.mongoose.base.api.Request;
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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
//
import java.io.File;
import java.io.Serializable;
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
	public final void start() {
		super.start();
		Runtime.getRuntime().addShutdownHook(new ShutDownThread(this));
	}
	//
	@Override
	public final void stop() {
		super.stop();
		SESSION_FACTORY.close();
	}
	//
	private HibernateAppender(
		final String name, final Filter filter,
		final Layout<? extends Serializable> layout, final boolean ignoreExceptions
	) {
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
		final @PluginAttribute("namedatabase") String dbName
	) {
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
				//initDataBase(userName, passWord, url);
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
	public final void append(final LogEvent event) {
		if (ENABLED_FLAG){
			final String marker = event.getMarker().toString();
			final String[] message = event.getMessage().getFormattedMessage().split("\\s*[,|/]\\s*");
			switch (marker) {
				case MSG:
				case ERR:
					persistMessages(event);
					break;
				case DATA_LIST:
					//persistDataObjects(message);
					break;
				case PERF_TRACE:
					persistTraces(event, message);
					break;
			}
		}
	}
	//
	private static void buildSessionFactory(
			final String username, final String password,
			final String url
	) {
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
	private static void persistStatusEntity(){
		Session session = SESSION_FACTORY.openSession();
		session.beginTransaction();
		for (final Request.Result result:Request.Result.values()){
			StatusEntity statusEntity = loadStatusEntity(session, result);
			session.saveOrUpdate(statusEntity);
		}
		session.getTransaction().commit();
		session.close();
	}
	//
	private static void persistMessages(final LogEvent event){
		Session session = SESSION_FACTORY.openSession();
		session.beginTransaction();
		final ModeEntity modeEntity = loadModeEntity(session, event.getContextMap().get(KEY_RUN_MODE));
		final RunEntity runEntity = loadRunEntity(session, event.getContextMap().get(KEY_RUN_ID), modeEntity);
		final MessageEntity messageEntity = loadMessageEntity(session, new Date(event.getTimeMillis()),
			event.getLoggerName(), event.getLevel().toString(), event.getMessage().getFormattedMessage(), runEntity);
		session.saveOrUpdate(messageEntity);
		session.getTransaction().commit();
		session.close();
	}
	//
	private static synchronized void persistTraces(final LogEvent event, final String[] message){
		Session session = SESSION_FACTORY.openSession();
		session.beginTransaction();
		final ModeEntity modeEntity = loadModeEntity(session, event.getContextMap().get(KEY_RUN_MODE));
		final RunEntity runEntity = loadRunEntity(session, event.getContextMap().get(KEY_RUN_ID), modeEntity);
		final LoadEntity loadEntity = loadLoadEntity(session, event.getContextMap().get(KEY_LOAD_NUM), runEntity,
			event.getContextMap().get(KEY_LOAD_TYPE), event.getContextMap().get(KEY_API));
		final ThreadEntity threadEntity = loadThreadEntity(session, loadEntity,
			event.getContextMap().get(KEY_NODE_ADDR), event.getContextMap().get(KEY_THREAD_NUM));
		TraceEntity traceEntity = loadTraceEntity(session, message[0], Long.valueOf(message[1], 0x10), threadEntity,
			Integer.valueOf(message[2], 0x10), Long.valueOf(message[3], 0x10), Long.valueOf(message[4], 0x10));
		session.saveOrUpdate(traceEntity);
		session.getTransaction().commit();
		session.close();
	}
	//
	private static void persistDataObjects(final String[] message){
		Session session = SESSION_FACTORY.openSession();
		session.beginTransaction();
		loadDataObjectEntity(session, message[0], message[1],
			Long.valueOf(message[2],0x10), Long.valueOf(message[3],0x10), Long.valueOf(message[4],0x10));
		session.getTransaction().commit();
		session.close();
	}

	private static ModeEntity loadModeEntity(final Session session, final String modeName){
		ModeEntity modeEntity = getModeEntity(session, modeName);
		if (modeEntity == null){
			modeEntity = new ModeEntity(modeName);
			session.saveOrUpdate(modeEntity);
		}
		return modeEntity;
	}
	private static RunEntity loadRunEntity(final Session session, final String runName, final ModeEntity mode){
		RunEntity runEntity = getRunEntity(session, runName);
		if (runEntity == null){
			runEntity = new RunEntity(mode, runName);
			session.saveOrUpdate(runEntity);
		}
		return runEntity;
	}
	private static ApiEntity loadApiEntity(final Session session, final String apiName){
		ApiEntity apiEntity = getApiEntity(session, apiName);
		if (apiEntity==null){
			apiEntity = new ApiEntity(apiName);
			session.saveOrUpdate(apiName);
		}
		return apiEntity;
	}
	//
	private static LoadTypeEntity loadLoadTypeEntity(final Session session, final String typeName){
		LoadTypeEntity loadTypeEntity = getLoadTypeEntity(session, typeName);
		if (loadTypeEntity==null){
			loadTypeEntity = new LoadTypeEntity(typeName);
			session.saveOrUpdate(loadTypeEntity);
		}
		return loadTypeEntity;
	}
	//If entity of run, api and load's types are known
	private static LoadEntity loadLoadEntity(final Session session, final String loadNumberString, final RunEntity run,
		final LoadTypeEntity type, final ApiEntity api)
	{
		LoadEntity loadEntity = getLoadEntity(session, Long.valueOf(loadNumberString), run);
		if (loadEntity == null) {
			loadEntity = new LoadEntity(run, type, Long.valueOf(loadNumberString), api);
			session.saveOrUpdate(loadEntity);
		}
		return loadEntity;
	}
	//If api and load's types aren't known
	private static LoadEntity loadLoadEntity(final Session session, final String loadNumber, final RunEntity run,
		final String typeName, final String apiName)
	{
		ApiEntity apiEntity = loadApiEntity(session, apiName);
		LoadTypeEntity loadTypeEntity = loadLoadTypeEntity(session, typeName);
		return loadLoadEntity(session, loadNumber,run,loadTypeEntity,apiEntity);
	}
	//
	private static MessageClassEntity loadClassOfMessage(final Session session, final String className){
		MessageClassEntity messageClassEntity = getMessageClassEntity(session, className);
		if (messageClassEntity==null){
			messageClassEntity = new MessageClassEntity(className);
			session.saveOrUpdate(messageClassEntity);
		}
		return messageClassEntity;
	}
	//
	private static LevelEntity loadLevelEntity(final Session session, final String levelName){
		LevelEntity levelEntity = getLevelEntity(session, levelName);
		if (levelEntity==null){
			levelEntity = new LevelEntity(levelName);
			session.saveOrUpdate(levelEntity);
		}
		return levelEntity;
	}
	//
	private static NodeEntity loadNodeEntity(final Session session, final String nodeAddr){
		NodeEntity nodeEntity = getNodeEntity(session, nodeAddr);
		if (nodeEntity == null){
			nodeEntity = new NodeEntity(nodeAddr);
			session.saveOrUpdate(nodeEntity);
		}
		return nodeEntity;
	}
	//If node is known
	private static ThreadEntity loadThreadEntity(final Session session, final String threadNumberString,
		final LoadEntity load, final NodeEntity node
	){
		ThreadEntity threadEntity = getThreadEntity(session, Long.valueOf(threadNumberString), load);
		if (threadEntity == null){
			threadEntity = new ThreadEntity(load, node, Long.valueOf(threadNumberString));
			session.saveOrUpdate(threadEntity);
		}
		return threadEntity;
	}
	//If node isn't known
	private static ThreadEntity loadThreadEntity(final Session session, final LoadEntity load, final String nodeAddr,
		final String threadNumber
	){
		final NodeEntity node = loadNodeEntity(session, nodeAddr);
		return loadThreadEntity(session, threadNumber,load,node);
	}
	//
	private static DataObjectEntity loadDataObjectEntity(
		final Session session, final String identifier, final long size)
	{
		DataObjectEntity dataObjectEntity = getDataObjectEntity(session, identifier, size);
		if (dataObjectEntity == null){
			dataObjectEntity = new DataObjectEntity(identifier, size);
			session.saveOrUpdate(dataObjectEntity);
		}
		return dataObjectEntity;
	}
	//
	private static DataObjectEntity loadDataObjectEntity(
		final Session session, final String identifier, final String ringOffset, final long size,
		final long layer, final long mask
	) {
		DataObjectEntity dataObjectEntity = getDataObjectEntity(session, identifier, size);
		if (dataObjectEntity == null){
			dataObjectEntity = new DataObjectEntity(identifier, ringOffset, size, layer, mask);
			session.saveOrUpdate(dataObjectEntity);
		}else{
			if (dataObjectEntity.getRingOffset() == null){
				dataObjectEntity.setRingOffset(ringOffset);
				session.saveOrUpdate(dataObjectEntity);
			}
			if (dataObjectEntity.getLayer() != layer){
				dataObjectEntity.setLayer(layer);
				session.saveOrUpdate(dataObjectEntity);
			}
			if (dataObjectEntity.getMask() != mask){
				dataObjectEntity.setMask(mask);
				session.saveOrUpdate(dataObjectEntity);
			}
		}
		return dataObjectEntity;
	}
	//
	private static StatusEntity loadStatusEntity(final Session session, final Request.Result result){
		StatusEntity statusEntity = getStatusEntity(session, result.code);
		if (session.get(StatusEntity.class, result.code) == null){
			statusEntity = new StatusEntity(result.code, result.description);
		}else{
			if (!statusEntity.getName().equals(result.description)){
				statusEntity.setName(result.description);
			}
		}
		return statusEntity;
	}
	//
	private static MessageEntity loadMessageEntity(final Session session, final Date tstamp, final String className,
		final String levelName, final String text, final RunEntity run
	){
		final MessageClassEntity classMessage = loadClassOfMessage(session, className);
		final LevelEntity level = loadLevelEntity(session, levelName);
		return new MessageEntity(run, classMessage, level, text, tstamp);
	}
	//
	private static TraceEntity loadTraceEntity(final Session session, final String identifier, final long size,
		final ThreadEntity threadEntity, final int status, final long reqStart, final long reqDur)
	{
		final StatusEntity statusEntity = getStatusEntity(session, status);
		final DataObjectEntity dataItem = loadDataObjectEntity(session, identifier, size);
		return new TraceEntity(dataItem, threadEntity, statusEntity, reqStart, reqDur);
	}
	//
	private static RunEntity getRunEntity(final Session session, final String runName){
		return (RunEntity) session.createCriteria(RunEntity.class)
			.add( Restrictions.eq("name", runName))
			.uniqueResult();
	}
	private static StatusEntity getStatusEntity(final Session session, final int codeStatus){
		return (StatusEntity) session.get(StatusEntity.class, codeStatus);
	}
	//
	private static LoadEntity getLoadEntity(final Session session, final long loadNumber, final RunEntity run){
		return (LoadEntity) session.createCriteria(LoadEntity.class)
			.add( Restrictions.eq("num", loadNumber))
			.add(Restrictions.eq("run", run))
			.uniqueResult();
	}
	//
	private static ThreadEntity getThreadEntity(final Session session, final long threadNumber, final LoadEntity load){
		return (ThreadEntity) session.createCriteria(ThreadEntity.class)
			.add(Restrictions.eq("load", load))
			.add(Restrictions.eq("num", threadNumber))
			.uniqueResult();
	}
	//
	private static ModeEntity getModeEntity(final Session session, final String modeName) {
		return (ModeEntity) session.createCriteria(ModeEntity.class)
			.add(Restrictions.eq("name", modeName))
			.uniqueResult();
	}
	//
	private static ApiEntity getApiEntity(final Session session, final String apiName) {
		return (ApiEntity) session.createCriteria(ApiEntity.class)
			.add(Restrictions.eq("name", apiName))
			.uniqueResult();
	}
	//
	private static LoadTypeEntity getLoadTypeEntity(final Session session, final String typeName) {
		return (LoadTypeEntity) session.createCriteria(LoadTypeEntity.class)
			.add(Restrictions.eq("name", typeName))
			.uniqueResult();
	}
	//
	private static MessageClassEntity getMessageClassEntity(final Session session, final String className) {
		return (MessageClassEntity) session.createCriteria(MessageClassEntity.class)
			.add(Restrictions.eq("name", className))
			.uniqueResult();
	}
	//
	private static LevelEntity getLevelEntity(final Session session, final String levelName) {
		return (LevelEntity) session.createCriteria(LevelEntity.class)
			.add(Restrictions.eq("name", levelName))
			.uniqueResult();
	}
	//
	private static NodeEntity getNodeEntity(final Session session, final String nodeAddr) {
		return (NodeEntity) session.createCriteria(NodeEntity.class)
			.add(Restrictions.eq("address", nodeAddr))
			.uniqueResult();
	}
	//
	private static DataObjectEntity getDataObjectEntity(
			final Session session, final String identifier, final long size
	) {
		return (DataObjectEntity) session.createCriteria(DataObjectEntity.class)
			.add(Restrictions.eq("identifier", identifier))
			.add(Restrictions.eq("size", size))
			.uniqueResult();
	}
	/////////////////////////////////////
	private final class ShutDownThread
			extends Thread
	{
		private final AbstractAppender appender;
		//
		public ShutDownThread(final HibernateAppender appender){
			super("HibernateShutDown");
			this.appender = appender;
		}
		@Override
		public final void run() {
			appender.stop();
		}
	}
	//////////////////////////////////////
}
