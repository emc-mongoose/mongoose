package com.emc.mongoose.storage.mock;

import com.emc.mongoose.common.net.NetUtil;
import com.emc.mongoose.storage.mock.impl.distribution.MDns;
import com.emc.mongoose.storage.mock.impl.distribution.NodeListener;
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
		try (final JmDNS jmDns = JmDNS.create(NetUtil.getHostAddr())) {
			try(final NodeListener listener = new NodeListener(jmDns, MDns.Type.HTTP)) {
				System.out.println("JmDNS address: " + jmDns.getInetAddress());
				listener.open();
				TimeUnit.SECONDS.sleep(10);
//				assertThat(listener.isNagainaDetected(), is(true));
			}
		}
	}
}