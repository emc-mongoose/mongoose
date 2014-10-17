package com.emc.mongoose.util.persist;
//
import com.emc.mongoose.util.logging.ExceptionHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
/**
 * Created by olga on 16.10.14.
 */
public class HibernateUtil {
	private static final SessionFactory sessionFactory = buildSessionFactory();
	private static final Logger LOG = LogManager.getLogger();
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
}
