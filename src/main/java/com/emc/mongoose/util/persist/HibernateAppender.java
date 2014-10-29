package com.emc.mongoose.util.persist;
//
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
								ERR = "err";
	//
	private HibernateAppender(
			final String name, final Filter filter, final Layout<? extends Serializable> layout,
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
		}
		return new HibernateAppender(name, filter, DEFAULT_LAYOUT, runid, runmode);
	}
	//
	private static void initDataBase(final String username,
									 final String password, final String url){
			final SessionFactory sessionFactory = buildSessionFactory(username, password, url);
			session = sessionFactory.openSession();
	}
	//
	@Override
	public final void append(final LogEvent event) {
		final Date date = new Date();
		final String marker = event.getMarker().toString();
		switch (marker){
			case PERF_AVG:
				System.out.println(marker);
				final String[] list = event.getThreadName().split("-");
				if (list.length>1){
					setLoad(list[1],list[2],list[0]);
				}
				break;
			case MSG:
			case ERR:
				setMessage(date,event.getLoggerName(),event.getLevel().toString(),event.getMessage().toString());
				break;
			case PERF_TRACE:
				final String[] threadInfo = event.getThreadName().split("-");
				final String nodeAddr = threadInfo[3].split("\\s*[#|<|>]\\s*")[1];
				final String threadNum  = threadInfo[3].split("\\s*[#|<|>]\\s*")[3];
				setThread(threadInfo[0],nodeAddr,threadNum);
				Map<String,String> map = event.getContextMap();
				Set<String> keys = map.keySet();
				Iterator<String> iterKey = keys.iterator();
				while (iterKey.hasNext()){
					String elem = iterKey.next();
					System.out.println(elem);
				}

		}
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
			//ExceptionHandler.trace(LOG, Level.WARN, ex,"Initial SessionFactory creation failed.");
			throw new ExceptionInInitializerError("Initial SessionFactory creation failed. "+ex);
		}
	}
	//
	//
	public static void setRun(final String runName, final String modeName){
		session.beginTransaction();
		ModeEntity mode = (ModeEntity) session.createCriteria(ModeEntity.class)
				.add( Restrictions.eq("name", modeName) )
				.uniqueResult();
		if (mode==null){
			mode = new ModeEntity(modeName);
		}
		session.save(mode);
		run = new RunEntity(mode,runName);
		session.save(run);
		session.getTransaction().commit();
	}
	//
	public static void setLoad(final String apiName,final String typeName,final String number){
		final BigInteger num = new BigInteger(number);
		session.beginTransaction();
		ApiEntity api = (ApiEntity) session.createCriteria(ApiEntity.class)
				.add( Restrictions.eq("name", apiName) ).uniqueResult();
		if (api==null){
			api = new ApiEntity(apiName);
		}
		LoadTypeEntity type = (LoadTypeEntity) session.createCriteria(LoadTypeEntity.class)
				.add( Restrictions.eq("name", typeName)).uniqueResult();
		if (type==null){
			type = new LoadTypeEntity(typeName);
		}
		LoadEntity load = (LoadEntity) session.createCriteria(LoadEntity.class)
				.add(Restrictions.eq("run", run))
				.add( Restrictions.eq("num", num))
				.uniqueResult();
		if (load == null){
			load = new LoadEntity(run,type,num,api);
		}
		session.save(api);
		session.save(type);
		session.save(load);
		session.getTransaction().commit();
	}
	//
	public static void setMessage(final Date tstamp, final String className, final String levelName, final String text){
		session.beginTransaction();
		MessageClassEntity classMessage = (MessageClassEntity) session.createCriteria(MessageClassEntity.class)
				.add(Restrictions.eq("name", className)).uniqueResult();
		if (classMessage==null){
			classMessage = new MessageClassEntity(className);
		}
		LevelEntity level = (LevelEntity) session.createCriteria(LevelEntity.class)
				.add(Restrictions.eq("name", levelName)).uniqueResult();
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
	public static void setThread(final String loadNumber, final String nodeAddr,final String num){
		final BigInteger number = new BigInteger(num);
		session.beginTransaction();
		NodeEntity node = (NodeEntity) session.createCriteria(NodeEntity.class)
				.add( Restrictions.eq("address",nodeAddr))
				.uniqueResult();
		if (node == null){
			node = new NodeEntity(nodeAddr);
		}
		LoadEntity load = (LoadEntity) session.createCriteria(LoadEntity.class)
				.add( Restrictions.eq("num",new BigInteger(loadNumber)))
				.add(Restrictions.eq("run", run))
				.uniqueResult();
		ThreadEntity threadEntity = (ThreadEntity) session.createCriteria(ThreadEntity.class)
				.add( Restrictions.eq("num",number))
				.add(Restrictions.eq("load", load))
				.uniqueResult();
		if (threadEntity == null){
			threadEntity = new ThreadEntity(load, node, number);
		}

		session.save(node);
		session.save(load);
		session.save(threadEntity);
		session.getTransaction().commit();
	}
	public static void setTrace(){

	}
}
