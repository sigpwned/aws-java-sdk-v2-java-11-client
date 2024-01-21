package com.sigpwned.software.amazon.awssdk.http.nio.java;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;

/**
 * Service binding for the Java HttpClient default implementation. Allows SDK to pick this up automatically from the classpath.
 */
@SdkPublicApi
public class JavaSdkAsyncHttpService implements SdkAsyncHttpService {

  @Override
  public SdkAsyncHttpClient.Builder createAsyncHttpClientFactory() {
    return JavaHttpClientNioAsyncHttpClient.builder();
  }

}