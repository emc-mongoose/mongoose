package com.emc.mongoose.util.logging;


import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.DirectoryLoader;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;


import java.nio.file.Paths;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

/**
 * Created by olga on 09.10.14.
 */
public class ConnectionFactory {
	private static interface Singleton {
		final ConnectionFactory INSTANCE = new ConnectionFactory();
	}

	private final DataSource dataSource;

	private ConnectionFactory() {
		final Configuration dbConf = new BaseConfiguration();
		DirectoryLoader.loadPropsFromDir(Paths.get(Main.DIR_ROOT, Main.DIR_CONF, Main.DIR_PROPERTIES, "db"), dbConf);
		final String	USER = dbConf.getString("db.username"),
						PASSWORD = dbConf.getString("db.password"),
						ADDR = dbConf.getString("db.addr"),
						PORT = dbConf.getString("db.port"),
						NAMEDB = dbConf.getString("db.name"),
						URL = "jdbc:postgresql://"+ADDR+":"+PORT+"/"+NAMEDB;
		Properties properties = new Properties();
		properties.setProperty("user", USER);
		properties.setProperty("password", PASSWORD);
		GenericObjectPool<PoolableConnection> pool = new GenericObjectPool<PoolableConnection>();
		DriverManagerConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
				URL, properties
		);
		new PoolableConnectionFactory(
				connectionFactory, pool, null, "SELECT 1", 3, false, false, Connection.TRANSACTION_READ_COMMITTED
		);
		this.dataSource = new PoolingDataSource(pool);
	}

	public final static Connection getDatabaseConnection()
	throws SQLException {
		return Singleton.INSTANCE.dataSource.getConnection();

	}



}
