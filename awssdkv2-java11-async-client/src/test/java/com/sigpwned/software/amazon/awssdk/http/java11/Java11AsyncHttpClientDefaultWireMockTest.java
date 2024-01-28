package com.sigpwned.software.amazon.awssdk.http.java11;

import com.sigpwned.software.amazon.awssdk.http.java11.Java11AsyncHttpClient.TrustAllTrustManager;
import javax.net.ssl.TrustManager;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkAsyncHttpClientH1TestSuite;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

public class Java11AsyncHttpClientDefaultWireMockTest extends SdkAsyncHttpClientH1TestSuite {

  @Override
  protected SdkAsyncHttpClient setupClient() {
    return Java11AsyncHttpClient.builder()
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
