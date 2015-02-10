package com.emc.mongoose.util.persist;
//
import com.emc.mongoose.base.api.AsyncIOTask;
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
import org.hibernate.cfg.AnnotationConfiguration;
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
@Plugin(name="hibernate", category="Core", elementType="appender", printObject=true)
public final class HibernateAppender
extends AbstractAppender {
	//
	private final static Layout<? extends Serializable>
			DEFAULT_LAYOUT = SerializedLayout.createLayout();
	public static Session SESSION = null;
	private static Boolean ENABLED_FLAG;
	private static final String
		PERF_AVG = "perfAvg",
		MSG = "msg",
		PERF_TRACE = "perfTrace",
		ERR = "err",
		KEY_NODE_ADDR = "node.addr",
		KEY_THREAD_NUM = "thread.number",
		KEY_LOAD_NUM = "load.number",
		KEY_LOAD_TYPE = "load.type",
		KEY_API = "api",
		KEY_RUN_ID = "run.id",
		KEY_RUN_MODE = "run.mode";
	//
	private HibernateAppender(
		final String name, final Filter filter, final Layout<? extends Serializable> layout,
		final String runId, final String runMode, final boolean ignoreExceptions
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
		final @PluginAttribute("runid") String runId,
		final @PluginAttribute("runmode") String runMode,
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
				initDataBase(userName, passWord, url);
				setStatusEntity();
			}
			newAppender = new HibernateAppender(
				name, filter, DEFAULT_LAYOUT, runId, runMode, ignoreExceptions
			);
		} catch (final Exception e) {
			throw new IllegalStateException("Open DB session failed", e);
		}
		return newAppender;
	}
	// init database session with username,password and url
	private static void initDataBase(
		final String userName, final String passWord, final String url
	) {
			final SessionFactory sessionFactory = buildSessionFactory(userName, passWord, url);
			if(sessionFactory != null) {
				SESSION = sessionFactory.openSession();
			}
	}
	// append method // - really?! (kurilov) - yep! (zhavzharova)
	@Override
	public final void append(final LogEvent event) {
		if (ENABLED_FLAG){			
			final String marker = event.getMarker().toString();
			switch (marker) {
				case MSG:
				case ERR:					SESSION.beginTransaction();
					final ModeEntity modeEntity = loadModeEntity(event.getContextMap().get(KEY_RUN_MODE));
					final RunEntity runEntity = loadRunEntity(event.getContextMap().get(KEY_RUN_ID), modeEntity);
					setMessageEntity(new Date(event.getTimeMillis()), event.getLoggerName(),
							event.getLevel().toString(), event.getMessage().getFormattedMessage(), runEntity);
					SESSION.getTransaction().commit();					break;
				case PERF_TRACE:
					SESSION.beginTransaction();
					final ModeEntity mode = loadModeEntity(event.getContextMap().get(KEY_RUN_MODE));
					final RunEntity run = loadRunEntity(event.getContextMap().get(KEY_RUN_ID), mode);
					final LoadEntity loadEntity = loadLoadEntity(event.getContextMap().get(KEY_LOAD_NUM), run,
							event.getContextMap().get(KEY_LOAD_TYPE), event.getContextMap().get(KEY_API));
					final ThreadEntity threadEntity = loadThreadEntity(loadEntity, event.getContextMap().get(KEY_NODE_ADDR),
							event.getContextMap().get(KEY_THREAD_NUM));
					final String[] message = event.getMessage().getFormattedMessage().split("\\s*[,|/]\\s*");
					setTraceEntity(message[0], message[1], Long.valueOf(message[2]), Long.valueOf(message[3]),
							Long.valueOf(message[4]), threadEntity, Integer.valueOf(message[5]),
							Long.valueOf(message[6]), Long.valueOf(message[7]));
					SESSION.getTransaction().commit();
					break;
			}
		}
	}
	//
	@Deprecated
	private static SessionFactory buildSessionFactory(
		final String username, final String password,
		final String url
	) {
		SessionFactory newSessionFactory = null;
		final String DIR_ROOT = System.getProperty("user.dir");
		final Path path = Paths.get(DIR_ROOT, "conf", "hibernate.cfg.xml");
		final File hibernateConfFile = new File(path.toString());
		try {
			// Create the SessionFactory from hibernate.cfg.xml
			newSessionFactory = new AnnotationConfiguration()
					.configure(hibernateConfFile)
					.setProperty("hibernate.connection.password", password)
					.setProperty("hibernate.connection.username", username)
					.setProperty("hibernate.connection.url", url)
					.buildSessionFactory();
		}
		catch(final Exception e) {
			// Make sure you log the exception, as it might be swallowed
			throw new ExceptionInInitializerError(e);
		}
		return newSessionFactory;
	}
	//
	private static ModeEntity loadModeEntity(final String modeName){
		ModeEntity mode = getModeEntity(modeName);
		if (mode == null){
			mode = new ModeEntity(modeName);
		}
		SESSION.save(mode);
		return mode;
	}
	private static RunEntity loadRunEntity(final String runName, final ModeEntity mode){
		RunEntity run = getRunEntity(runName);
		if (run == null){
			run = new RunEntity(mode, runName);
		}
		SESSION.save(run);
		return run;
	}
	private static ApiEntity loadApiEntity(final String apiName){
		ApiEntity api = getApiEntity(apiName);
		if (api==null){
			api = new ApiEntity(apiName);
		}
		SESSION.save(api);
		return api;
	}
	//
	private static LoadTypeEntity loadLoadTypeEntity(final String typeName){
		LoadTypeEntity type = getLoadTypeEntity(typeName);
		if (type==null){
			type = new LoadTypeEntity(typeName);
		}
		SESSION.save(type);
		return type;
	}
	//If entity of run, api and load's types are known
	private static LoadEntity loadLoadEntity(final String loadNumberString, final RunEntity run,
											 final LoadTypeEntity type, final ApiEntity api){
		LoadEntity load = getLoadEntity(Long.valueOf(loadNumberString), run);
		if (load == null) {
			load = new LoadEntity(run, type, Long.valueOf(loadNumberString), api);
		}
		SESSION.save(load);
		return load;
	}
	//If api and load's types aren't known
	private static LoadEntity loadLoadEntity(final String loadNumber, final RunEntity run,
											 final String typeName, final String apiName){
		ApiEntity api = loadApiEntity(apiName);
		LoadTypeEntity type = loadLoadTypeEntity(typeName);
		LoadEntity load = loadLoadEntity(loadNumber,run,type,api);
		SESSION.save(load);
		return load;
	}
	//
	private static MessageClassEntity loadClassOfMessage(final String className){
		MessageClassEntity classMessage = getMessageClassEntity(className);
		if (classMessage==null){
			classMessage = new MessageClassEntity(className);
		}
		SESSION.save(classMessage);
		return classMessage;
	}
	//
	private static LevelEntity loadLevelEntity(final String levelName){
		LevelEntity level = getLevelEntity(levelName);
		if (level==null){
			level = new LevelEntity(levelName);
		}
		SESSION.save(level);
		return level;
	}
	//
	private static NodeEntity loadNodeEntity(final String nodeAddr){
		NodeEntity node = getNodeEntity(nodeAddr);
		if (node == null){
			node = new NodeEntity(nodeAddr);
		}
		SESSION.save(node);
		return node;
	}
	//If node is known
	private static ThreadEntity loadThreadEntity(final String threadNumberString, final LoadEntity load, final NodeEntity node){
		ThreadEntity threadEntity = getThreadEntity(Long.valueOf(threadNumberString), load);
		if(threadEntity == null) {
			threadEntity = new ThreadEntity(load, node, Long.valueOf(threadNumberString));
		}
		SESSION.save(threadEntity);
		return threadEntity;
	}
	//If node isn't known
	private static ThreadEntity loadThreadEntity(final LoadEntity load, final String nodeAddr, final String threadNumber){
		final NodeEntity node = loadNodeEntity(nodeAddr);
		final ThreadEntity threadEntity = loadThreadEntity(threadNumber,load,node);
		SESSION.save(load);
		return threadEntity;
	}
	//
	private static DataObjectEntity loadDataObjectEntity(
		final String identifier, final String ringOffset, final long size,
		final long layer, final long mask
	) {
		DataObjectEntity dataObject = getDataItemEntity(identifier, ringOffset, size);
		if (dataObject == null){
			dataObject = new DataObjectEntity(identifier, ringOffset, size, layer, mask);
		}else{
			//If DataItem update
			if (dataObject.getLayer()!=layer || dataObject.getMask()!=mask){
				dataObject.setLayer(layer);
				dataObject.setMask(mask);
			}
			//
		}
		SESSION.saveOrUpdate(dataObject);
		return dataObject;
	}
	//
	private static StatusEntity loadStatusEntity(final AsyncIOTask.Status status){
		StatusEntity statusEntity = getStatusEntity(status.code);
		if (SESSION.get(StatusEntity.class, status.code) == null){
			statusEntity = new StatusEntity(status.code, status.description);
		}else{
			if (!statusEntity.getName().equals(status.description)){
				statusEntity.setName(status.description);
			}
		}
		SESSION.saveOrUpdate(statusEntity);
		return statusEntity;
	}
	//
	private static void setMessageEntity(final Date tstamp, final String className, final String levelName, final String text, final RunEntity run){
		final MessageClassEntity classMessage = loadClassOfMessage(className);
		final LevelEntity level = loadLevelEntity(levelName);
		final MessageEntity messageEntity = new MessageEntity(run, classMessage, level, text, tstamp);
		SESSION.save(messageEntity);
	}
	//
	private static void setTraceEntity(final String identifier, final String ringOffset, final long size,
											  final long layer, final long mask, final ThreadEntity threadEntity,
											  final int status, final long reqStart, final long reqDur){
		final StatusEntity statusEntity = getStatusEntity(status);
		final DataObjectEntity dataItem = loadDataObjectEntity(identifier,ringOffset,size,layer,mask);
		final TraceEntity traceEntity = new TraceEntity(dataItem, threadEntity, statusEntity, reqStart, reqDur);
		SESSION.save(traceEntity);
	}
	//
	private static void setStatusEntity(){
		SESSION.beginTransaction();
		for (final AsyncIOTask.Status status : AsyncIOTask.Status.values()){
			StatusEntity statusEntity = loadStatusEntity(status);
		}
		SESSION.getTransaction().commit();
	}
	//
	private static RunEntity getRunEntity(final String runName){
		return (RunEntity) SESSION.createCriteria(RunEntity.class)
				.add( Restrictions.eq("name", runName))
				.uniqueResult();
	}
	private static StatusEntity getStatusEntity(final int codeStatus){
		return (StatusEntity) SESSION.get(StatusEntity.class, codeStatus);
	}
	//
	private static LoadEntity getLoadEntity(final long loadNumber, final RunEntity run){
		return (LoadEntity) SESSION.createCriteria(LoadEntity.class)
				.add( Restrictions.eq("num", loadNumber))
				.add(Restrictions.eq("run", run))
				.uniqueResult();
	}
	//
	private static ThreadEntity getThreadEntity(final long threadNumber, final LoadEntity load){
		return (ThreadEntity) SESSION.createCriteria(ThreadEntity.class)
				.add( Restrictions.eq("load", load))
				.add( Restrictions.eq("num", threadNumber))
				.uniqueResult();
	}
	//
	private static ModeEntity getModeEntity(final String modeName) {
		return (ModeEntity) SESSION.createCriteria(ModeEntity.class)
				.add(Restrictions.eq("name", modeName))
				.uniqueResult();
	}
	//
	private static ApiEntity getApiEntity(final String apiName) {
		return (ApiEntity) SESSION.createCriteria(ApiEntity.class)
				.add( Restrictions.eq("name", apiName) )
				.uniqueResult();
	}
	//
	private static LoadTypeEntity getLoadTypeEntity(final String typeName) {
		return (LoadTypeEntity) SESSION.createCriteria(LoadTypeEntity.class)
				.add( Restrictions.eq("name", typeName))
				.uniqueResult();
	}
	//
	private static MessageClassEntity getMessageClassEntity(final String className) {
		return (MessageClassEntity) SESSION.createCriteria(MessageClassEntity.class)
				.add(Restrictions.eq("name", className))
				.uniqueResult();
	}
	//
	private static LevelEntity getLevelEntity(final String levelName) {
		return (LevelEntity) SESSION.createCriteria(LevelEntity.class)
				.add(Restrictions.eq("name", levelName))
				.uniqueResult();
	}
	//
	private static NodeEntity getNodeEntity(final String nodeAddr) {
		return (NodeEntity) SESSION.createCriteria(NodeEntity.class)
				.add( Restrictions.eq("address", nodeAddr))
				.uniqueResult();
	}
	//
	private static DataObjectEntity getDataItemEntity(
		final String identifier, final String ringOffset, final long size
	) {
		return (DataObjectEntity) SESSION.createCriteria(DataObjectEntity.class)
				.add( Restrictions.eq("identifier", identifier))
				.add(Restrictions.eq("ringOffset", ringOffset))
				.add( Restrictions.eq("size", size))
				.uniqueResult();
	}
}
