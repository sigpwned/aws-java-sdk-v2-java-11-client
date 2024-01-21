package com.sigpwned.software.amazon.awssdk.http.nio.java;

import java.time.Duration;
import javax.net.ssl.SSLParameters;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * These should actually be in the SDK, but we're intentionally keeping this library separate, so we
 * keep them here. As a result, there's some pokery-jiggery to get the defaults loaded.
 *
 * @see SdkHttpConfigurationOption
 */
@SdkProtectedApi
public final class JavaHttpClientHttpConfigurationOption<T> extends AttributeMap.Key<T> {

  /**
   * SSLParameters of the SSLSocket, could be set in HttpClient.
   */
  public static final JavaHttpClientHttpConfigurationOption<SSLParameters> SSL_PARAMETERS = new JavaHttpClientHttpConfigurationOption<>(
      "SslParameters", SSLParameters.class);

  /**
   * Timeout for waiting for a response.
   */
  public static final JavaHttpClientHttpConfigurationOption<Duration> RESPONSE_TIMEOUT = new JavaHttpClientHttpConfigurationOption<>(
      "ResponseTimeout", Duration.class);

  private static final SSLParameters DEFAULT_SSL_PARAMETERS = new SSLParameters();
  private static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(30);

  public static final AttributeMap GLOBAL_HTTP_DEFAULTS = AttributeMap.builder()
      .put(SSL_PARAMETERS, DEFAULT_SSL_PARAMETERS).put(RESPONSE_TIMEOUT, DEFAULT_RESPONSE_TIMEOUT)
      .build().merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS);

  private final String name;

  private JavaHttpClientHttpConfigurationOption(String name, Class<T> klazz) {
    super(klazz);
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return name();
  }
}
