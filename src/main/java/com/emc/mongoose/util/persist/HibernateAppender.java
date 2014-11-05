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
import java.util.logging.Level;
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
	public static Session SESSION = null;
	private static RunEntity run;
	private static Boolean ENABLED_FLAG;
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
			final @PluginAttribute("enabled") Boolean enabled,
			final @PluginAttribute("runid") String runid,
			final @PluginAttribute("runmode") String runmode,
			final @PluginAttribute("username") String username,
			final @PluginAttribute("password") String password,
			final @PluginAttribute("addr") String addr,
			final @PluginAttribute("port") String port,
			final @PluginAttribute("namedatabase") String namedatabase
	) {
		java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.OFF);
		HibernateAppender newAppender = null;
		ENABLED_FLAG = enabled;
		if(name == null) {
			throw new IllegalArgumentException("No name provided for HibernateAppender");
		}
		final String url = "jdbc:postgresql://"+addr+":"+port+"/"+namedatabase;
		try {
			if(ENABLED_FLAG) {
				initDataBase(username, password, url);
				setRun(runid, runmode);
				setStatus();
			}
			newAppender = new HibernateAppender(name, filter, DEFAULT_LAYOUT, runid, runmode);
		} catch (final Exception e) {
			throw new IllegalStateException("Open DB session failed", e);
		}
		return newAppender;
	}
	//Init Database with username,password and url
	private static void initDataBase(final String username,
									 final String password, final String url){
			final SessionFactory sessionFactory = buildSessionFactory(username, password, url);
			if(sessionFactory != null) {
				SESSION = sessionFactory.openSession();
			}
	}
	//Append method
	@Override
	public final void append(final LogEvent event) {
		if (ENABLED_FLAG){
			final Date date = new Date();
			final String marker = event.getMarker().toString();
			switch (marker) {
				case MSG:
				case ERR:
					setMessage(date, event.getLoggerName(), event.getLevel().toString(), event.getMessage().getFormattedMessage());
					break;
				case PERF_TRACE:
					//System.out.println(event.getContextMap().get(KEY_API) + " | " + event.getContextMap().get(KEY_LOAD_TYPE) + " | " + event.getContextMap().get(KEY_LOAD_NUM) + " | " + event.getContextMap().get(KEY_NODE_ADDR));
					setLoad(event.getContextMap().get(KEY_API),
							event.getContextMap().get(KEY_LOAD_TYPE),
							event.getContextMap().get(KEY_LOAD_NUM));
					setThread(event.getContextMap().get(KEY_LOAD_NUM),
							event.getContextMap().get(KEY_NODE_ADDR),
							event.getContextMap().get(KEY_THREAD_NUM));
					final String[] message = event.getMessage().getFormattedMessage().split("\\s*[,|/]\\s*");
					setTrace(message[0], message[1], getValueFromMessage(message, 2), getValueFromMessage(message, 3),
							getValueFromMessage(message, 4), new BigInteger(event.getContextMap().get(KEY_THREAD_NUM)),
							new BigInteger(event.getContextMap().get(KEY_LOAD_NUM)), Integer.valueOf(message[5]),
							getValueFromMessage(message, 6), getValueFromMessage(message, 7));
					break;
			}
		}
	}
	//
	private static BigInteger getValueFromMessage( final String[] message, final int index){
		return new BigInteger(message[index], 16);
	}
	//
	private static SessionFactory buildSessionFactory(
			final String username, final String password,
			final String url) {
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
		catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			throw new ExceptionInInitializerError("Initial SessionFactory creation failed. "+ex);
		}
		return newSessionFactory;
	}
	//
	//
	private static void setRun(final String runName, final String modeName){
		SESSION.beginTransaction();
		ModeEntity mode = getModeEntity(modeName);
		if (mode==null){
			mode = new ModeEntity(modeName);
		}
		SESSION.save(mode);
		run = new RunEntity(mode, runName);
		SESSION.save(run);
		SESSION.getTransaction().commit();
	}
	//
	private static void setLoad(final String apiName, final String typeName, final String loadNumberString){
		final BigInteger loadNumber = new BigInteger(loadNumberString);
		SESSION.beginTransaction();
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
			load = new LoadEntity(run, type, loadNumber, api);
		}
		SESSION.save(api);
		SESSION.save(type);
		SESSION.save(load);
		SESSION.getTransaction().commit();
	}
	//
	private static void setMessage(final Date tstamp, final String className, final String levelName, final String text){
		SESSION.beginTransaction();
		MessageClassEntity classMessage = getMessageClassEntity(className);
		if (classMessage==null){
			classMessage = new MessageClassEntity(className);
		}
		LevelEntity level = getLevelEntity(levelName);
		if (level==null){
			level = new LevelEntity(levelName);
		}
		final MessageEntity message = new MessageEntity(run, classMessage, level, text, tstamp);
		SESSION.save(classMessage);
		SESSION.save(level);
		SESSION.save(message);
		SESSION.getTransaction().commit();
	}
	//
	private static void setThread(final String loadNumberString, final String nodeAddr, final String threadNumberString){
		final BigInteger threadNumber = new BigInteger(threadNumberString);
		SESSION.beginTransaction();
		NodeEntity node = getNodeEntity(nodeAddr);
		if (node == null){
			node = new NodeEntity(nodeAddr);
		}
		LoadEntity load = getLoadEntity(new BigInteger(loadNumberString));
		ThreadEntity threadEntity = getThreadEntity(threadNumber, load);
		if (threadEntity == null){
			threadEntity = new ThreadEntity(load, node, threadNumber);
		}
		SESSION.save(node);
		SESSION.save(load);
		SESSION.save(threadEntity);
		SESSION.getTransaction().commit();
	}
	private static void setTrace(final String identifier, final String ringOffset, final BigInteger size,
								final BigInteger layer, final BigInteger mask, final BigInteger threadNum,
								final BigInteger loadNum, final int status,
								final BigInteger reaStart, final BigInteger reqDur){
		SESSION.beginTransaction();
		final StatusEntity statusEntity = getStatusEntity(status);
		LoadEntity load = getLoadEntity(loadNum);
		ThreadEntity thread = getThreadEntity(threadNum, load);
		DataItemEntity dataItem = getDataItemEntity(identifier, ringOffset, size);
		if (dataItem == null){
			dataItem = new DataItemEntity(identifier, ringOffset, size, layer, mask);
		}else{
			//If DataItem update
			if (!dataItem.getLayer().equals(layer) || !dataItem.getMask().equals(mask)){
				dataItem.setLayer(layer);
				dataItem.setMask(mask);
			}
			//
		}
		final TraceEntity trace = new TraceEntity(dataItem, thread, statusEntity, reaStart, reqDur);
		SESSION.save(statusEntity);
		SESSION.save(thread);
		SESSION.saveOrUpdate(dataItem);
		SESSION.save(trace);
		SESSION.getTransaction().commit();
	}
	//
	private static void setStatus(){
		SESSION.beginTransaction();
		for (final Request.Result result:Request.Result.values()){
			StatusEntity statusEntity = getStatusEntity(result.code);
			if (SESSION.get(StatusEntity.class, result.code) == null){
				statusEntity = new StatusEntity(result.code, result.description);
			}else{
				if (!statusEntity.getName().equals(result.description)){
					statusEntity.setName(result.description);
				}
			}
			SESSION.saveOrUpdate(statusEntity);
		}
		SESSION.getTransaction().commit();
	}
	private static StatusEntity getStatusEntity(final int codeStatus){
		return (StatusEntity) SESSION.get(StatusEntity.class, codeStatus);
	}
	//
	private static LoadEntity getLoadEntity(final BigInteger loadNumber){
		return (LoadEntity) SESSION.createCriteria(LoadEntity.class)
				.add( Restrictions.eq("num", loadNumber))
				.add(Restrictions.eq("run", run))
				.uniqueResult();
	}
	//
	private static ThreadEntity getThreadEntity(final BigInteger threadNumber, final LoadEntity load){
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
	private static DataItemEntity getDataItemEntity(final String identifier, final String ringOffset, final BigInteger size) {
		return (DataItemEntity) SESSION.createCriteria(DataItemEntity.class)
				.add( Restrictions.eq("identifier", identifier))
				.add(Restrictions.eq("ringOffset", ringOffset))
				.add( Restrictions.eq("size", size))
				.uniqueResult();
	}
}
