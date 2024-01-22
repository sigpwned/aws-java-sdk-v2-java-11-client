package com.sigpwned.software.amazon.awssdk.http.nio.java;

import com.sigpwned.software.amazon.awssdk.http.ExecutorProvider;
import java.time.Duration;
import javax.net.ssl.SSLParameters;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
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
   * Option to disable SSL cert validation and SSL host name verification. By default, this option
   * is off. Only enable this option for testing purposes.
   *
   * @see SdkHttpConfigurationOption#TRUST_ALL_CERTIFICATES
   */
  public static final SdkHttpConfigurationOption<Boolean> TRUST_ALL_CERTIFICATES = SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

  /**
   * The {@link TlsTrustManagersProvider} that will be used by the HTTP client when authenticating
   * with a TLS host.
   *
   * @see SdkHttpConfigurationOption#TLS_TRUST_MANAGERS_PROVIDER
   */
  public static final SdkHttpConfigurationOption<TlsTrustManagersProvider> TLS_TRUST_MANAGERS_PROVIDER = SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER;

  /**
   * The {@link TlsKeyManagersProvider} that will be used by the HTTP client when authenticating
   * with a TLS host.
   *
   * @see SdkHttpConfigurationOption#TLS_KEY_MANAGERS_PROVIDER
   */
  public static final SdkHttpConfigurationOption<TlsKeyManagersProvider> TLS_KEY_MANAGERS_PROVIDER = SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER;

  /**
   * HTTP protocol to use.
   */
  public static final SdkHttpConfigurationOption<Protocol> PROTOCOL = SdkHttpConfigurationOption.PROTOCOL;

  /**
   * Timeout for establishing a connection to a remote service.
   */
  public static final SdkHttpConfigurationOption<Duration> CONNECTION_TIMEOUT = SdkHttpConfigurationOption.CONNECTION_TIMEOUT;

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

  /**
   * The {@link ExecutorProvider} that will be used by the HTTP client when executing requests. If
   * the provider returns null, the client will use the default executor.
   */
  public static final JavaHttpClientHttpConfigurationOption<ExecutorProvider> REQUEST_EXECUTOR_PROVIDER = new JavaHttpClientHttpConfigurationOption<>(
      "RequestExecutorProvider", ExecutorProvider.class);

  private static final SSLParameters DEFAULT_SSL_PARAMETERS = new SSLParameters();
  private static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(30);
  private static final ExecutorProvider DEFAULT_REQUEST_EXECUTOR_PROVIDER = () -> null;

  public static final AttributeMap GLOBAL_HTTP_DEFAULTS = AttributeMap.builder()
      .put(SSL_PARAMETERS, DEFAULT_SSL_PARAMETERS).put(RESPONSE_TIMEOUT, DEFAULT_RESPONSE_TIMEOUT)
      .put(REQUEST_EXECUTOR_PROVIDER, DEFAULT_REQUEST_EXECUTOR_PROVIDER).build()
      .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS);

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
