package com.sigpwned.software.amazon.awssdk.http.nio.java;

import javax.net.ssl.SSLParameters;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.http.SdkAsyncHttpClientH1TestSuite;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

public class JavaHttpClientDefaultWireMockTest extends SdkAsyncHttpClientH1TestSuite {


  @Override
  protected SdkAsyncHttpClient setupClient() {
    return JavaHttpClientNioAsyncHttpClient.builder()
        .build();
  }

  @Before
  public void setupJavaHttpClientDefaultWireMockTest() {
    // https://stackoverflow.com/questions/19540289/how-to-fix-the-java-security-cert-certificateexception-no-subject-alternative
    // https://stackoverflow.com/questions/52859195/using-httpbuilder-api-in-java-11-where-do-you-specify-the-hostnameverifier
    System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    System.setProperty("jdk.internal.httpclient.debug", "true");
  }

  @After
  public void teardownJavaHttpClientDefaultWireMockTest() {
    System.clearProperty("jdk.internal.httpclient.debug");
  }
}
