package com.emc.mongoose.storage.mock;

import com.emc.mongoose.storage.mock.distribution.MDns;
import com.emc.mongoose.storage.mock.distribution.NagainaListener;
import org.junit.Ignore;
import org.junit.Test;

import javax.jmdns.JmDNS;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 Created on 23.08.16.
 */
public class NagainaMainTest {

	@Ignore
	@Test
	public void shouldFindNagaina()
	throws Exception {
		try(final NagainaListener listener = new NagainaListener(JmDNS.create(), MDns.Type.HTTP)) {
			listener.open();
			TimeUnit.SECONDS.sleep(1);
			assertThat(listener.isNagainaDetected(), is(true));
		}
	}
}