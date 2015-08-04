package com.emc.mongoose.storage.adapter.sdk;
//

import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.filepool.fplibrary.FPLibraryException;
import com.filepool.fplibrary.FPPool;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

//
//
//
//

/**
 Created by kurila on 02.10.14.
 */
public class PoolImpl<T extends DataObject>
implements Pool<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String VERSIONING_ENTITY_CONTENT =
		"<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
		"<Status>Enabled</Status></VersioningConfiguration>";
	private final static String VERSIONING_URL_PART = "/?versioning";
	//
	private final ObjectRequestConfigImpl<T> reqConf;
	private String name;
	private boolean versioningEnabled;
	//
	public PoolImpl(
			final ObjectRequestConfigImpl<T> reqConf, final String name, final boolean versioningEnabled
	) {
		this.reqConf = reqConf;
		//
		if(name == null || name.length() == 0) {
			final Date dt = Calendar.getInstance(LogUtil.TZ_UTC, LogUtil.LOCALE_DEFAULT).getTime();
			this.name = "mongoose-" + LogUtil.FMT_DT.format(dt);
		} else {
			this.name = name;
		}
		this.versioningEnabled = versioningEnabled;
	}
	//
	@Override
	public final String getName() {
		return toString();
	}
	//
	@Override
	public final String toString() {
		return name;
	}
	//

	//

	//
	@Override
	public final boolean exists(final String addr)
	throws IllegalStateException {
		boolean flagExists = false;
		//

		//
		return flagExists;
	}
	//
	//
	@Override
	public final void create(final String addr)
	throws IllegalStateException {
		//

		try {

			FPPool pool = new FPPool(addr);
			System.out.println("Pool open @ "+addr);

		}catch (FPLibraryException e){

		}catch (IllegalArgumentException e){

		}

	}
	//
	@Override
	public final void delete(final String addr)
	throws IllegalStateException {
		//

	}
}
