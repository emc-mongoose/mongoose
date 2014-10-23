package com.emc.mongoose.util.persist;
//
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.criterion.Restrictions;
//
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
/**
 * Created by olga on 16.10.14.
 */
public final class PersistDAO {
	private static final SessionFactory sessionFactory = buildSessionFactory();
	//private static final Logger LOG = LogManager.getLogger();
	public static final Session session = sessionFactory.openSession();
	private static RunEntity run;

	//
	private static SessionFactory buildSessionFactory() {
		final String DIR_ROOT = System.getProperty("user.dir");
		final Path path = Paths.get(DIR_ROOT, "conf","hibernate.cfg.xml");
		File hibernateConfFile = new File(path.toString());
		try {
			// Create the SessionFactory from hibernate.cfg.xml
			return new AnnotationConfiguration().configure(hibernateConfFile).buildSessionFactory();
		}
		catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			//ExceptionHandler.trace(LOG, Level.WARN, ex,"Initial SessionFactory creation failed.");
			throw new ExceptionInInitializerError("Initial SessionFactory creation failed. "+ex);
		}
	}
	//
	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	//
	public static void shutdown() {
		// Close caches and connection pools
		getSessionFactory().close();
	}
	private static RunEntity getRun(){
		return run;
	}
	//
	public static void setRun(final String runName, final String modeName){
		session.beginTransaction();
		ModeEntity mode = (ModeEntity) session.createCriteria(ModeEntity.class).
				add( Restrictions.eq("name", modeName) ).
				uniqueResult();
		if (mode==null){
			mode = new ModeEntity(modeName);
		}
		session.save(mode);
		run = new RunEntity(mode,runName);
		session.save(run);
		session.getTransaction().commit();
	}
	//
	public static void setLoad(final String apiName,final String typeName,final int number){
		BigInteger num = new BigInteger(String.valueOf(number));
		session.beginTransaction();
		ApiEntity api = (ApiEntity) session.createCriteria(ApiEntity.class).
				add( Restrictions.eq("name", apiName) ).uniqueResult();
		if (api==null){
			api = new ApiEntity(apiName);
		}
		LoadTypeEntity type = (LoadTypeEntity) session.createCriteria(LoadTypeEntity.class).
				add( Restrictions.eq("name", typeName)).uniqueResult();
		if (type==null){
			type = new LoadTypeEntity(typeName);
		}
		final LoadEntity load = new LoadEntity(run,type,num,api);
		session.save(api);
		session.save(type);
		session.save(load);
		session.getTransaction().commit();
	}

}
