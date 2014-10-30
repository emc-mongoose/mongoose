package com.emc.mongoose.util.persist;
//
import com.emc.mongoose.base.api.Request;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.criterion.Restrictions;
//
import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
//
/**
 * Created by olga on 24.10.14.
 */
@Plugin(name="Hibernate", category="Core", elementType="appender", printObject=true)
public final class HibernateAppender
extends AbstractAppender{
	//
	private final static Layout<? extends Serializable>
			DEFAULT_LAYOUT = SerializedLayout.createLayout();
	public static Session session;
	private static RunEntity run;
	private static Boolean databaseInit;
	private static final String PERF_AVG = "perfAvg",
								MSG = "msg",
								PERF_TRACE = "perfTrace",
								ERR = "err",
								KEY_NODE_ADDR = "node.addr",
								KEY_THREAD_NUM = "thread.number",
								KEY_LOAD_NUM = "load.number",
								KEY_LOAD_TYPE = "load.type",
								KEY_API = "api";
	//
	private HibernateAppender(
			final String name, final Filter filter,
			final Layout<? extends Serializable> layout,
			final String runid, final String runmode
	) {
		super(name, filter, layout);
	}
	//
	@PluginFactory
	public static HibernateAppender createAppender(
			final @PluginAttribute("name") String name,
			final @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
			final @PluginElement("Filters") Filter filter,
			final @PluginAttribute("option") Boolean option,
			final @PluginAttribute("runid") String runid,
			final @PluginAttribute("runmode") String runmode,
			final @PluginAttribute("username") String username,
			final @PluginAttribute("password") String password,
			final @PluginAttribute("addr") String addr,
			final @PluginAttribute("port") String port,
			final @PluginAttribute("namedatabase") String namedatabase
	) {
		databaseInit = option;
		if (name == null) {
			LOGGER.error("No name provided for CustomAppender");
			return null;
		}
		final String url = "jdbc:postgresql://"+addr+":"+port+"/"+namedatabase;
		//
		if (databaseInit){
			initDataBase(username,password,url);
			setRun(runid,runmode);
			setStatus();
		}
		return new HibernateAppender(name, filter, DEFAULT_LAYOUT, runid, runmode);
	}
	//Init Database with username,password and url
	private static void initDataBase(final String username,
									 final String password, final String url){
			final SessionFactory sessionFactory = buildSessionFactory(username, password, url);
			session = sessionFactory.openSession();
	}
	//Append method
	@Override
	public final void append(final LogEvent event) {
		final Date date = new Date();
		final String marker = event.getMarker().toString();
		switch (marker){
			case MSG:
			case ERR:
				setMessage(date, event.getLoggerName(), event.getLevel().toString(), event.getMessage().getFormattedMessage());
				break;
			case PERF_TRACE:
				setLoad(event.getContextMap().get(KEY_API),
						event.getContextMap().get(KEY_LOAD_TYPE),
						event.getContextMap().get(KEY_LOAD_NUM));
				setThread(event.getContextMap().get(KEY_LOAD_NUM),
						event.getContextMap().get(KEY_NODE_ADDR),
						event.getContextMap().get(KEY_THREAD_NUM));
				final String[] message = event.getMessage().getFormattedMessage().split("\\s*[,|/]\\s*");
				setTrace(message[0],message[1],getValueFromMessage(message,2), getValueFromMessage(message,3),
						getValueFromMessage(message,4), new BigInteger(event.getContextMap().get(KEY_THREAD_NUM)),
						new BigInteger(event.getContextMap().get(KEY_LOAD_NUM)),Integer.valueOf(message[5]),
						getValueFromMessage(message,6), getValueFromMessage(message,7));
				break;
		}
	}
	//
	private static BigInteger getValueFromMessage( final String[] message, final int index){
		return new BigInteger(message[index]);
	}
	//
	private static SessionFactory buildSessionFactory(
			final String username, final String password,
			final String url) {
		final String DIR_ROOT = System.getProperty("user.dir");
		final Path path = Paths.get(DIR_ROOT, "conf","hibernate.cfg.xml");
		File hibernateConfFile = new File(path.toString());
		try {
			// Create the SessionFactory from hibernate.cfg.xml
			return new AnnotationConfiguration()
					.configure(hibernateConfFile)
					.setProperty("hibernate.connection.password", password)
					.setProperty("hibernate.connection.username", username)
					.setProperty("hibernate.connection.url", url)
					.buildSessionFactory();
		}
		catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			throw new ExceptionInInitializerError("Initial SessionFactory creation failed. "+ex);
		}
	}
	//
	//
	public static void setRun(final String runName, final String modeName){
		session.beginTransaction();
		ModeEntity mode = getModeEntity(modeName);
		if (mode==null){
			mode = new ModeEntity(modeName);
		}
		session.save(mode);
		run = new RunEntity(mode,runName);
		session.save(run);
		session.getTransaction().commit();
	}
	//
	public static void setLoad(final String apiName,final String typeName,final String loadNumberString){
		final BigInteger loadNumber = new BigInteger(loadNumberString);
		session.beginTransaction();
		ApiEntity api = getApiEntity(apiName);
		if (api==null){
			api = new ApiEntity(apiName);
		}
		LoadTypeEntity type = getLoadTypeEntity(typeName);
		if (type==null){
			type = new LoadTypeEntity(typeName);
		}
		LoadEntity load = getLoadEntity(loadNumber);
		if (load == null){
			load = new LoadEntity(run,type,loadNumber,api);
		}
		session.save(api);
		session.save(type);
		session.save(load);
		session.getTransaction().commit();
	}
	//
	public static void setMessage(final Date tstamp, final String className, final String levelName, final String text){
		session.beginTransaction();
		MessageClassEntity classMessage = getMessageClassEntity(className);
		if (classMessage==null){
			classMessage = new MessageClassEntity(className);
		}
		LevelEntity level = getLevelEntity(levelName);
		if (level==null){
			level = new LevelEntity(levelName);
		}
		final MessageEntity message = new MessageEntity(run, classMessage,level,text,tstamp);
		session.save(classMessage);
		session.save(level);
		session.save(message);
		session.getTransaction().commit();
	}
	//
	public static void setThread(final String loadNumberString, final String nodeAddr,final String threadNumberString){
		final BigInteger threadNumber = new BigInteger(threadNumberString);
		session.beginTransaction();
		NodeEntity node = getNodeEntity(nodeAddr);
		if (node == null){
			node = new NodeEntity(nodeAddr);
		}
		LoadEntity load = getLoadEntity(new BigInteger(loadNumberString));
		ThreadEntity threadEntity = getThreadEntity(threadNumber,load);
		if (threadEntity == null){
			threadEntity = new ThreadEntity(load, node, threadNumber);
		}
		session.save(node);
		session.save(load);
		session.save(threadEntity);
		session.getTransaction().commit();
	}
	public static void setTrace(final String identifier, final String ringOffset,final BigInteger size,
								final BigInteger layer, final BigInteger mask, final BigInteger threadNum,
								final BigInteger loadNum, final int status,
								final BigInteger reaStart, final BigInteger reqDur){
		session.beginTransaction();
		final StatusEntity statusEntity = getStatusEntity(status);
		LoadEntity load = getLoadEntity(loadNum);
		ThreadEntity thread = getThreadEntity(threadNum,load);
		DataItemEntity dataItem = getDataItemEntity(identifier, ringOffset, size);
		if (dataItem == null){
			dataItem = new DataItemEntity(identifier, ringOffset, size, layer, mask);
		}else{
			//If DataItem update
			if (dataItem.getLayer()!= layer || dataItem.getMask()!=mask){
				dataItem.setLayer(layer);
				dataItem.setMask(mask);
			}
			//
		}
		final TraceEntity trace = new TraceEntity(dataItem,thread,statusEntity,reaStart,reqDur);
		session.save(statusEntity);
		session.save(thread);
		session.saveOrUpdate(dataItem);
		session.save(trace);
		session.getTransaction().commit();
	}
	//
	public static void setStatus(){
		session.beginTransaction();
		for (Request.Result result:Request.Result.values()){
			StatusEntity statusEntity = getStatusEntity(result.code);
			if ((StatusEntity) session.get(StatusEntity.class,result.code) == null){
				statusEntity = new StatusEntity(result.code, result.description);
			}else{
				if (!statusEntity.getName().equals(result.description)){
					statusEntity.setName(result.description);
				}
			}
			session.saveOrUpdate(statusEntity);
		}
		session.getTransaction().commit();
	}
	private static StatusEntity getStatusEntity(final int codeStatus){
		return (StatusEntity) session.get(StatusEntity.class, codeStatus);
	}
	//
	private static LoadEntity getLoadEntity(final BigInteger loadNumber){
		return (LoadEntity) session.createCriteria(LoadEntity.class)
				.add( Restrictions.eq("num",loadNumber))
				.add(Restrictions.eq("run", run))
				.uniqueResult();
	}
	//
	private static ThreadEntity getThreadEntity(final BigInteger threadNumber, final LoadEntity load){
		return (ThreadEntity) session.createCriteria(ThreadEntity.class)
				.add( Restrictions.eq("load",load))
				.add( Restrictions.eq("num",threadNumber))
				.uniqueResult();
	}
	//
	private static ModeEntity getModeEntity(final String modeName) {
		return (ModeEntity) session.createCriteria(ModeEntity.class)
				.add(Restrictions.eq("name", modeName))
				.uniqueResult();
	}
	//
	private static ApiEntity getApiEntity(final String apiName) {
		return (ApiEntity) session.createCriteria(ApiEntity.class)
				.add( Restrictions.eq("name", apiName) )
				.uniqueResult();
	}
	//
	private static LoadTypeEntity getLoadTypeEntity(final String typeName) {
		return (LoadTypeEntity) session.createCriteria(LoadTypeEntity.class)
				.add( Restrictions.eq("name", typeName))
				.uniqueResult();
	}
	//
	private static MessageClassEntity getMessageClassEntity(final String className) {
		return (MessageClassEntity) session.createCriteria(MessageClassEntity.class)
				.add(Restrictions.eq("name", className))
				.uniqueResult();
	}
	//
	private static LevelEntity getLevelEntity(final String levelName) {
		return (LevelEntity) session.createCriteria(LevelEntity.class)
				.add(Restrictions.eq("name", levelName))
				.uniqueResult();
	}
	//
	private static NodeEntity getNodeEntity(final String nodeAddr) {
		return (NodeEntity) session.createCriteria(NodeEntity.class)
				.add( Restrictions.eq("address",nodeAddr))
				.uniqueResult();
	}
	//
	private static DataItemEntity getDataItemEntity(final String identifier, final String ringOffset, final BigInteger size) {
		return (DataItemEntity) session.createCriteria(DataItemEntity.class)
				.add( Restrictions.eq("identifier", identifier))
				.add(Restrictions.eq("ringOffset", ringOffset))
				.add( Restrictions.eq("size", size))
				.uniqueResult();
	}
}
