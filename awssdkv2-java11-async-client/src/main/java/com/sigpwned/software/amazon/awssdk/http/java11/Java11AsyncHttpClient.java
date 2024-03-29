package com.sigpwned.software.amazon.awssdk.http.java11;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

import com.sigpwned.software.amazon.awssdk.http.java11.internal.JavaHttpClientRequestExecutor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link SdkAsyncHttpClient} that uses a Java HTTP Client.
 *
 * <p>This can be created via {@link @builder()}</p>
 */
@SdkPublicApi
public final class Java11AsyncHttpClient implements SdkAsyncHttpClient {

  private static final Logger log = Logger.loggerFor(Java11AsyncHttpClient.class);

  private static final String CLIENT_NAME = "Java11";

  public static Builder builder() {
    return new DefaultBuilder();
  }

  public static SdkAsyncHttpClient create() {
    return builder().build();
  }

  private final HttpClient javaHttpClient;

  private final AttributeMap serviceDefaultsMap;

  private Java11AsyncHttpClient(DefaultBuilder builder, AttributeMap serviceDefaultsMap) {
    /*this.configuration = new JavaHttpClientConfiguration(serviceDefaultsMap);*/

    Duration connectTimeout = getConnectTimeout(serviceDefaultsMap);
    HttpClient.Version version = getVersion(serviceDefaultsMap);
    SSLParameters sslParameters = getSslParameters(serviceDefaultsMap);
    SSLContext sslContext = getSslContext(serviceDefaultsMap);
    Executor requestExecutor = getRequestExecutor(serviceDefaultsMap);
    ProxySelector proxySelector = getProxySelector(serviceDefaultsMap);

    HttpClient.Builder javaHttpClientBuilder = HttpClient.newBuilder()
        .connectTimeout(connectTimeout).version(version).sslParameters(sslParameters)
        .proxy(proxySelector).sslContext(sslContext);
    if (requestExecutor != null) {
      // According to the docs, it's important not even to call executor() if we want to use the
      // default behavior. It's not enough just to give null.
      javaHttpClientBuilder = javaHttpClientBuilder.executor(requestExecutor);
    }

    this.javaHttpClient = javaHttpClientBuilder.build();

    this.serviceDefaultsMap = serviceDefaultsMap;
  }

  HttpClient getHttpClient() {
    return javaHttpClient;
  }

  /**
   * Returns a {@link CompletableFuture} that will be completed when the response is completed. If
   * the request succeeds, then the future will be completed normally. If the request fails, then
   * the future will be completed exceptionally with an IOException, of a relevant type (e.g.,
   * {@link java.net.http.HttpTimeoutException}, {@link java.net.http.HttpConnectTimeoutException},
   * {@link java.net.ConnectException}) if possible.
   *
   * @param request The request object.
   * @return The future
   * @throws java.util.concurrent.ExecutionException containing an {@link java.io.IOException} if
   *                                                 the request fails
   */
  @Override
  public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
    // TODO What should happen if the future is cancelled? What does happen? Check Netty.

    // Wholly delegate to an internal API
    Duration requestTimeout = getResponseTimeout(serviceDefaultsMap);
    return new JavaHttpClientRequestExecutor(getHttpClient(), requestTimeout).execute(request)
        .thenApply(response -> null);
  }

  @Override
  public void close() {
  }

  @Override
  public String clientName() {
    return CLIENT_NAME;
  }


  /**
   * Builder that allows configuration of the Java NIO HTTP implementation. Use {@link #builder()}
   * to configure and construct a Java Http Client.
   */
  public static interface Builder extends
      SdkAsyncHttpClient.Builder<Java11AsyncHttpClient.Builder> {

    /**
     * The amount of time to wait for a connection before an exeception is thrown.
     *
     * @param connectionTimeout timeout duration.
     * @return This builder for method chaining.
     */
    Builder connectionTimeout(Duration connectionTimeout);


    /**
     * Sets the HTTP protocol to use (i.e. HTTP/1.1 or HTTP/2). Not all services support HTTP/2.
     *
     * @param protocol Protocol to use.
     * @return This builder for method chaining.
     */
    Builder protocol(Protocol protocol);

    /**
     * Sets the SSL related parameters (e.g. Protocols, CipherSuites, ApplicationProtocols etc.) via
     * SSLParameters object.
     *
     * @param sslParameter SSLParameters object.
     * @return This builder for method chaining.
     */
    Builder sslParameters(SSLParameters sslParameter);

    /**
     * Sets the amount of time to wait for a response before timeout.
     *
     * @param responseTimeout timeout duration.
     * @return This builder for method chaining.
     */
    Builder responseTimeout(Duration responseTimeout);

    /**
     * Sets the {@link ExecutorProvider} that will be used by the HTTP client to create the
     * {@link Executor} used to handle requests. If the provider is not set OR if the provider is
     * set and returns null, then the client will use {@link HttpClient}'s default behavior for
     * creating the executor.
     *
     * @param executor a customized executor provider created by user
     * @return This builder for method chaining.
     */
    Builder requestExecutorProvider(ExecutorProvider executor);

    /**
     * Sets the {@link TlsKeyManagersProvider} that will be used by the HTTP client when
     * authenticating with a TLS host.
     *
     * @param tlsTrustManagersProvider the {@link TlsKeyManagersProvider} to use.
     * @return This builder for method chaining.
     */
    Builder tlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider);

    /**
     * Sets the {@link TlsKeyManagersProvider} that will be used by the HTTP client when
     * authenticating with a TLS host.
     *
     * @param tlsKeyManagersProvider the {@link TlsKeyManagersProvider} to use.
     * @return This builder for method chaining.
     */
    Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider);

    /**
     * Sets the {@link ProxyConfiguration} that will be used by the HTTP client.
     *
     * @param proxyConfiguration
     * @return
     */
    Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);
  }

  private static final class DefaultBuilder implements Builder {

    private final AttributeMap.Builder standardOptions = AttributeMap.builder();

    private DefaultBuilder() {
    }

    @Override
    public Builder connectionTimeout(Duration connectionTimeout) {
      Validate.isPositive(connectionTimeout, "connectionTimeout");
      standardOptions.put(Java11SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
      return this;
    }

    /**
     * Setter to set the connection timeout directly.
     *
     * @param connectionTimeout timeout duration.
     */
    public void setConnectionTimeout(Duration connectionTimeout) {
      connectionTimeout(connectionTimeout);
    }

    @Override
    public Builder protocol(Protocol protocol) {
      standardOptions.put(Java11SdkHttpConfigurationOption.PROTOCOL, protocol);
      return this;
    }

    /**
     * Setter to set the protocol directly.
     *
     * @param protocol Protocol to use.
     */
    public void setProtocol(Protocol protocol) {
      protocol(protocol);
    }

    @Override
    public Builder sslParameters(SSLParameters sslParameters) {
      standardOptions.put(Java11SdkHttpConfigurationOption.SSL_PARAMETERS, sslParameters);
      return this;
    }

    public void setSslParameters(SSLParameters sslParameters) {
      sslParameters(sslParameters);
    }

    @Override
    public Builder responseTimeout(Duration responseTimeout) {
      standardOptions.put(Java11SdkHttpConfigurationOption.RESPONSE_TIMEOUT, responseTimeout);
      return this;
    }

    /**
     * Setter to set the timeout of waiting a response.
     *
     * @param responseTimeout timeout duration.
     */
    public void setResponseTimeout(Duration responseTimeout) {
      responseTimeout(responseTimeout);
    }

    /**
     * If customers use this method then we should not close the executor when the client is
     * closed.
     *
     * @param requestExecutorProvider a customized executor created by user
     * @return
     */
    @Override
    public Builder requestExecutorProvider(ExecutorProvider requestExecutorProvider) {
      standardOptions.put(Java11SdkHttpConfigurationOption.REQUEST_EXECUTOR_PROVIDER,
          requestExecutorProvider);
      return this;
    }

    public void setRequestExecutorProvider(ExecutorProvider requestExecutorProvider) {
      requestExecutorProvider(requestExecutorProvider);
    }

    @Override
    public Builder tlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider) {
      standardOptions.put(Java11SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER,
          tlsTrustManagersProvider);
      return this;
    }

    /**
     * Setter to set the TLS trust manager provider directly.
     *
     * @param tlsTrustManagersProvider the {@link TlsTrustManagersProvider} to use.
     */
    public void setTlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider) {
      tlsTrustManagersProvider(tlsTrustManagersProvider);
    }

    @Override
    public Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
      standardOptions.put(Java11SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER,
          tlsKeyManagersProvider);
      return this;
    }

    /**
     * Setter to set the TLS key manager provider directly.
     *
     * @param tlsKeyManagersProvider the {@link TlsKeyManagersProvider} to use.
     */
    public void setTlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
      tlsKeyManagersProvider(tlsKeyManagersProvider);
    }

    @Override
    public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
      standardOptions.put(Java11SdkHttpConfigurationOption.PROXY_CONFIGURATION, proxyConfiguration);
      return this;
    }

    public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
      proxyConfiguration(proxyConfiguration);
    }

    @Override
    public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
      return new Java11AsyncHttpClient(this, standardOptions.build().merge(serviceDefaults)
          .merge(Java11SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
    }
  }

  // REQUEST TIMEOUT ///////////////////////////////////////////////////////////
  private static Duration getResponseTimeout(AttributeMap serviceDefaultsMap) {
    return serviceDefaultsMap.get(Java11SdkHttpConfigurationOption.RESPONSE_TIMEOUT);
  }

  // REQUEST EXECUTOR //////////////////////////////////////////////////////////
  private static Executor getRequestExecutor(AttributeMap serviceDefaultsMap) {
    return serviceDefaultsMap.get(Java11SdkHttpConfigurationOption.REQUEST_EXECUTOR_PROVIDER)
        .executor();
  }

  // PROXY CONFIGURATION ///////////////////////////////////////////////////////
  private static ProxySelector getProxySelector(AttributeMap serviceDefaultsMap) {
    ProxyConfiguration proxyConfiguration = serviceDefaultsMap.get(
        Java11SdkHttpConfigurationOption.PROXY_CONFIGURATION);
    if (proxyConfiguration == null || proxyConfiguration.host() == null) {
      return ProxySelector.getDefault();
    }
    return new DefaultProxySelector(new Proxy(Proxy.Type.HTTP,
        InetSocketAddress.createUnresolved(proxyConfiguration.host(), proxyConfiguration.port())),
        proxyConfiguration.nonProxyHosts());
  }

  private static class DefaultProxySelector extends ProxySelector {

    private final Proxy proxy;
    private final Set<String> nonProxyHosts;

    public DefaultProxySelector(Proxy proxy, Set<String> nonProxyHosts) {
      this.proxy = requireNonNull(proxy);
      this.nonProxyHosts = unmodifiableSet(nonProxyHosts);
    }

    @Override
    public List<Proxy> select(URI uri) {
      return singletonList(nonProxyHosts.contains(uri.getHost()) ? Proxy.NO_PROXY : proxy);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress address, IOException e) {
      log.warn(() -> "Failed to connect to proxy " + address, e);
    }
  }

  // CONNECT TIMEOUT //////////////////////////////////////////////////////////

  private static Duration getConnectTimeout(AttributeMap serviceDefaultsMap) {
    return serviceDefaultsMap.get(Java11SdkHttpConfigurationOption.CONNECTION_TIMEOUT);
  }

  // VERSION //////////////////////////////////////////////////////////////////

  private static HttpClient.Version getVersion(AttributeMap serviceDefaultsMap) {
    HttpClient.Version result;

    Protocol protocol = serviceDefaultsMap.get(Java11SdkHttpConfigurationOption.PROTOCOL);
    switch (protocol) {
      case HTTP1_1:
        result = HttpClient.Version.HTTP_1_1;
        break;
      case HTTP2:
        result = HttpClient.Version.HTTP_2;
        break;
      default:
        throw new RuntimeException("Unrecognized protocol: " + protocol);
    }
    return result;
  }

  // SSL PARAMETERS ///////////////////////////////////////////////////////////

  private static SSLParameters getSslParameters(AttributeMap serviceDefaultsMap) {
    return serviceDefaultsMap.get(Java11SdkHttpConfigurationOption.SSL_PARAMETERS);
  }

  // SSL CONTEXT ///////////////////////////////////////////////////////////////

  /**
   * Create an SSLContext from the provided options.
   *
   * @see <a
   * href="https://github.com/aws/aws-sdk-java-v2/blob/10121d4ed8497a3c5d415475975373d71013517f/http-clients/url-connection-client/src/main/java/software/amazon/awssdk/http/urlconnection/UrlConnectionHttpClient.java#L245">
   * https://github.com/aws/aws-sdk-java-v2/blob/10121d4ed8497a3c5d415475975373d71013517f/http-clients/url-connection-client/src/main/java/software/amazon/awssdk/http/urlconnection/UrlConnectionHttpClient.java#L245</a>
   */
  private SSLContext getSslContext(AttributeMap options) {
    Validate.isTrue(
        options.get(Java11SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER) == null
            || !options.get(Java11SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES),
        "A TlsTrustManagerProvider can't be provided if TrustAllCertificates is also set");

    TrustManager[] trustManagers = null;
    if (options.get(Java11SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER) != null) {
      trustManagers = options.get(Java11SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER)
          .trustManagers();
    }

    if (options.get(Java11SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES)) {
      log.warn(() ->
          "SSL Certificate verification is disabled. This is not a safe setting and should only be "
              + "used for testing.");
      trustManagers = new TrustManager[]{TrustAllTrustManager.INSTANCE};
    }

    TlsKeyManagersProvider provider = options.get(
        Java11SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER);
    KeyManager[] keyManagers = provider.keyManagers();

    SSLContext context;
    try {
      context = SSLContext.getInstance("TLS");
      context.init(keyManagers, trustManagers, null);
      return context;
    } catch (NoSuchAlgorithmException | KeyManagementException ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }

  /**
   * Insecure trust manager to trust all certs. Should only be used for testing.
   *
   * @see <a
   * href="https://github.com/aws/aws-sdk-java-v2/blob/10121d4ed8497a3c5d415475975373d71013517f/http-clients/url-connection-client/src/main/java/software/amazon/awssdk/http/urlconnection/UrlConnectionHttpClient.java#L603">
   * https://github.com/aws/aws-sdk-java-v2/blob/10121d4ed8497a3c5d415475975373d71013517f/http-clients/url-connection-client/src/main/java/software/amazon/awssdk/http/urlconnection/UrlConnectionHttpClient.java#L603</a>
   */
  static class TrustAllTrustManager extends X509ExtendedTrustManager {

    public static final TrustAllTrustManager INSTANCE = new TrustAllTrustManager();

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String s) {
      log.debug(() -> "Accepting a client certificate: " + chain[0].getSubjectDN());
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String s) {
      log.debug(() -> "Accepting a server certificate: " + chain[0].getSubjectDN());
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
        throws CertificateException {
      log.debug(() -> "Accepting a server certificate: " + chain[0].getSubjectDN());
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
        throws CertificateException {
      log.debug(() -> "Accepting a server certificate: " + chain[0].getSubjectDN());
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
        throws CertificateException {
      log.debug(() -> "Accepting a client certificate: " + chain[0].getSubjectDN());
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
        throws CertificateException {
      log.debug(() -> "Accepting a server certificate: " + chain[0].getSubjectDN());
    }
  }
}