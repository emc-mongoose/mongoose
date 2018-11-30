package com.emc.mongoose.controls;

import com.emc.mongoose.control.ConfigServlet;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** @author veronika K. on 02.11.18 */
public class ConfigServletTest {

  private static final int PORT = 9999;
  private static final String HOST = "http://localhost:" + PORT;
  private static final Map schema = new HashMap<>();

  static {
    schema.put("key", Object.class);
  }

  private static final Config config = new BasicConfig("-", schema);
  private static final String configStr = "{\n\t\"key\" : \"value\"\n}";
  private static final String schemaStr = "{\n  \"key\" : \"any\"\n}";

  static {
    config.val("key", "value");
  }

  private static final Server server = new Server(PORT);

  @Before
  public void setUp() throws Exception {
    final ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new ConfigServlet(config)), "/config/*");
    server.start();
  }

  @Test
  public void test() throws Exception {
    final String config = resultFromServer(HOST + "/config");
    final String schema = resultFromServer(HOST + "/config/schema");
    Assert.assertEquals(config, configStr);
    Assert.assertEquals(schema, schemaStr);
  }

  private String resultFromServer(final String urlPath) throws Exception {
    final String result;
    final URL url = new URL(urlPath);
    final URLConnection conn = url.openConnection();
    try (final BufferedReader br =
        new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
      result = br.lines().collect(Collectors.joining("\n"));
    }
    return result;
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }
}
