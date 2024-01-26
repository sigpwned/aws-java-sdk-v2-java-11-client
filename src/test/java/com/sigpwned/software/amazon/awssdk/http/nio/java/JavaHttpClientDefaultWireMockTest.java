package com.sigpwned.software.amazon.awssdk.http.nio.java;

import com.sigpwned.software.amazon.awssdk.http.nio.java.JavaHttpClientNioAsyncHttpClient.TrustAllTrustManager;
import java.net.http.HttpClient;
import javax.net.ssl.TrustManager;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkAsyncHttpClientH1TestSuite;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

public class JavaHttpClientDefaultWireMockTest extends SdkAsyncHttpClientH1TestSuite {

  @Override
  protected SdkAsyncHttpClient setupClient() {
    return JavaHttpClientNioAsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{TrustAllTrustManager.INSTANCE}).build();
  }

  @Override
  @Ignore
  public void connectionReceiveServerErrorStatusShouldNotReuseConnection() {
    // TODO We should not reuse connections that received a 5xx from the server.
    // We don't support closing connections on error for now.
  }

  @Test
  public void test() {
  }
}
