package com.emc.mongoose.util.persist;
//
import com.emc.mongoose.util.logging.ExceptionHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.event.spi.LoadEventListener;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
/**
 * Created by olga on 16.10.14.
 */
public final class HibernateUtil {
	private static final SessionFactory sessionFactory = buildSessionFactory();
	private static final Logger LOG = LogManager.getLogger();
	public static Session session = sessionFactory.openSession();
	private static RunEntity run;

	//
	private static final SessionFactory buildSessionFactory() {
		final String DIR_ROOT = System.getProperty("user.dir");
		final Path path = Paths.get(DIR_ROOT, "conf","hibernate.cfg.xml");
		File hibernateConfFile = new File(path.toString());
		try {
			// Create the SessionFactory from hibernate.cfg.xml
			return new AnnotationConfiguration().configure(hibernateConfFile).buildSessionFactory();
		}
		catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			ExceptionHandler.trace(LOG, Level.WARN, ex,"Initial SessionFactory creation failed.");
			throw new ExceptionInInitializerError("Initial SessionFactory creation failed. "+ex);
		}
	}
	//
	public static final SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	//
	public static final void shutdown() {
		// Close caches and connection pools
		getSessionFactory().close();
	}
	private static final RunEntity getRun(){
		return run;
	}
	//
	public static final void setRun(String runName, String modeName){
		session.beginTransaction();
		ModeEntity mode = (ModeEntity) session.createCriteria(ModeEntity.class).
				add( Restrictions.eq("name", modeName) ).
				uniqueResult();
		if (mode==null){
			mode = new ModeEntity(modeName);
		}
		run = new RunEntity(mode,runName);

		session.merge(run);
		session.merge(mode);
		session.getTransaction().commit();
	}
	//
	public static final void setLoad(String apiName,String typeName,int number){
		BigInteger num = new BigInteger(String.valueOf(number));
		session.beginTransaction();
		ApiEntity api = (ApiEntity) session.createCriteria(ApiEntity.class).
				add( Restrictions.eq("name", apiName) ).uniqueResult();
		if (api==null){
			api = new ApiEntity(apiName);
		}
		session.merge(api);
		LoadTypeEntity type = (LoadTypeEntity) session.createCriteria(LoadTypeEntity.class).
				add( Restrictions.eq("name", typeName)).uniqueResult();
		if (type==null){
			type = new LoadTypeEntity(typeName);
		}
		session.merge(type);
		LoadEntity load = new LoadEntity(getRun(),type,num,api);
		session.merge(load);
		session.getTransaction().commit();
	}

}
