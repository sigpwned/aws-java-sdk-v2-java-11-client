package com.sigpwned.software.amazon.awssdk.http.nio.java;

import com.sigpwned.software.amazon.awssdk.http.nio.java.JavaHttpClientNioAsyncHttpClient.TrustAllTrustManager;
import javax.net.ssl.TrustManager;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkAsyncHttpClientH1TestSuite;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

public class JavaHttpClientDefaultWireMockTest extends SdkAsyncHttpClientH1TestSuite {

  @Override
  protected SdkAsyncHttpClient setupClient() {
    return JavaHttpClientNioAsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{TrustAllTrustManager.INSTANCE}).build();
  }

  @Test
  public void test() {
  }
}
