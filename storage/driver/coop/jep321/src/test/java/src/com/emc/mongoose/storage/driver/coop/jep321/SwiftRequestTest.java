package src.com.emc.mongoose.storage.driver.coop.jep321;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;

public class SwiftRequestTest {

  @Test
  public void testGetAuthTokenSyncRequest() throws Exception {
    final var client = HttpClient.newBuilder().build();
    final var uri = new URI("http://127.0.0.1:8080/auth/v1.0");
    final var request =
        HttpRequest.newBuilder()
            .GET()
            .uri(uri)
            .header("X-Auth-User", "test:tester")
            .header("X-Auth-Key", "yuiwegheebae")
            .version(Version.HTTP_1_1)
            .build();
    final var response = client.send(request, BodyHandlers.discarding());
    final var token =
        response.headers().firstValue("X-Auth-Token").orElseThrow(AssertionError::new);
    assertNotNull(token);
  }

  @Test
  public void testGetAuthTokenAsyncRequest() throws Exception {
    final var client = HttpClient.newBuilder().build();
    final var uri = new URI("http://127.0.0.1:8080/auth/v1.0");
    final var request =
        HttpRequest.newBuilder()
            .GET()
            .uri(uri)
            .header("X-Auth-User", "test:tester")
            .header("X-Auth-Key", "yuiwegheebae")
            .version(Version.HTTP_1_1)
            .build();
    final CountDownLatch responseGotLatch = new CountDownLatch(1);
    client
        .sendAsync(request, BodyHandlers.discarding())
        .handle(
            (response, thrown) -> {
              assertNull(thrown);
              final var token =
                  response.headers().firstValue("X-Auth-Token").orElseThrow(AssertionError::new);
              assertNotNull(token);
              responseGotLatch.countDown();
              return true;
            });
    responseGotLatch.await();
  }
}
