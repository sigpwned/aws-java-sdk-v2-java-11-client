package com.sigpwned.software.amazon.awssdk.http.java11;

import static software.amazon.awssdk.utils.StringUtils.isEmpty;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ProxyConfigProvider;
import software.amazon.awssdk.utils.ProxyEnvironmentSetting;
import software.amazon.awssdk.utils.ProxySystemSetting;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Proxy configuration for {@link Java11AsyncHttpClient}. This class is used to configure an HTTP
 * proxy to be used by the {@link Java11AsyncHttpClient}.
 *
 * @see Java11AsyncHttpClient.Builder#proxyConfiguration(ProxyConfiguration)
 */
@SdkPublicApi
public final class ProxyConfiguration implements
    ToCopyableBuilder<ProxyConfiguration.Builder, ProxyConfiguration> {

  private final URI endpoint;
  private final Set<String> nonProxyHosts;
  private final String host;
  private final int port;
  private final String scheme;
  private final boolean useSystemPropertyValues;
  private final boolean useEnvironmentVariablesValues;

  /**
   * Initialize this configuration. Private to require use of {@link #builder()}.
   */
  private ProxyConfiguration(DefaultClientProxyConfigurationBuilder builder) {
    this.endpoint = builder.endpoint;
    this.scheme = resolveScheme(builder);
    ProxyConfigProvider proxyConfigProvider =
        ProxyConfigProvider.fromSystemEnvironmentSettings(
            builder.useSystemPropertyValues,
            builder.useEnvironmentVariablesValues,
            scheme);

    this.nonProxyHosts = resolveNonProxyHosts(builder, proxyConfigProvider);
    this.useSystemPropertyValues = builder.useSystemPropertyValues;

    if (builder.endpoint != null) {
      this.host = builder.endpoint.getHost();
      this.port = builder.endpoint.getPort();
    } else {
      this.host = proxyConfigProvider != null ? proxyConfigProvider.host() : null;
      this.port = proxyConfigProvider != null ? proxyConfigProvider.port() : 0;
    }
    this.useEnvironmentVariablesValues = builder.useEnvironmentVariablesValues;
  }

  private String resolveScheme(DefaultClientProxyConfigurationBuilder builder) {
    if (endpoint != null) {
      return endpoint.getScheme();
    } else {
      return builder.scheme;
    }
  }

  private static Set<String> resolveNonProxyHosts(DefaultClientProxyConfigurationBuilder builder,
      ProxyConfigProvider proxyConfigProvider) {
    return builder.nonProxyHosts != null || proxyConfigProvider == null ? builder.nonProxyHosts :
        proxyConfigProvider.nonProxyHosts();
  }

  /**
   * Returns the proxy host name either from the configured endpoint or from the "http.proxyHost"
   * system property if {@link Builder#useSystemPropertyValues(Boolean)} is set to true.
   */
  public String host() {
    return host;
  }

  /**
   * Returns the proxy port either from the configured endpoint or from the "http.proxyPort" system
   * property if {@link Builder#useSystemPropertyValues(Boolean)} is set to true.
   * <p>
   * If no value is found in neither of the above options, the default value of 0 is returned.
   */
  public int port() {
    return port;
  }

  /**
   * Returns the {@link URI#getScheme()} from the configured endpoint. Otherwise, return null.
   */
  public String scheme() {
    return scheme;
  }

  /**
   * The hosts that the client is allowed to access without going through the proxy.
   * <p>
   * If the value is not set on the object, the value represent by "http.nonProxyHosts" system
   * property is returned. If system property is also not set, an unmodifiable empty set is
   * returned.
   *
   * @see Builder#nonProxyHosts(Set)
   */
  public Set<String> nonProxyHosts() {
    return Collections.unmodifiableSet(
        nonProxyHosts != null ? nonProxyHosts : Collections.emptySet());
  }

  @Override
  public Builder toBuilder() {
    return builder()
        .endpoint(endpoint)
        .nonProxyHosts(nonProxyHosts)
        .useSystemPropertyValues(useSystemPropertyValues)
        .scheme(scheme)
        .useEnvironmentVariablesValues(useEnvironmentVariablesValues);
  }

  /**
   * Create a {@link Builder}, used to create a {@link ProxyConfiguration}.
   */
  public static Builder builder() {
    return new DefaultClientProxyConfigurationBuilder();
  }

  @Override
  public String toString() {
    return ToString.builder("ProxyConfiguration")
        .add("endpoint", endpoint)
        .add("nonProxyHosts", nonProxyHosts)
        .build();
  }

  public String resolveScheme() {
    return endpoint != null ? endpoint.getScheme() : scheme;
  }

  /**
   * A builder for {@link ProxyConfiguration}.
   *
   * <p>All implementations of this interface are mutable and not thread safe.</p>
   */
  public interface Builder extends CopyableBuilder<Builder, ProxyConfiguration> {

    /**
     * Configure the endpoint of the proxy server that the SDK should connect through. Currently,
     * the endpoint is limited to a host and port. Any other URI components will result in an
     * exception being raised.
     */
    Builder endpoint(URI endpoint);

    /**
     * Configure the hosts that the client is allowed to access without going through the proxy.
     */
    Builder nonProxyHosts(Set<String> nonProxyHosts);

    /**
     * Add a host that the client is allowed to access without going through the proxy.
     *
     * @see ProxyConfiguration#nonProxyHosts()
     */
    Builder addNonProxyHost(String nonProxyHost);

    /**
     * Option whether to use system property values from {@link ProxySystemSetting} if any of the
     * config options are missing.
     * <p>
     * This value is set to "true" by default which means SDK will automatically use system property
     * values for options that are not provided during building the {@link ProxyConfiguration}
     * object. To disable this behavior, set this value to "false".It is important to note that when
     * this property is set to "true," all proxy settings will exclusively originate from system
     * properties, and no partial settings will be obtained from EnvironmentVariableValues
     */
    Builder useSystemPropertyValues(Boolean useSystemPropertyValues);

    /**
     * Option whether to use environment variable values from {@link ProxyEnvironmentSetting} if any
     * of the config options are missing. This value is set to "true" by default, which means SDK
     * will automatically use environment variable values for options that are not provided during
     * building the {@link ProxyConfiguration} object. To disable this behavior, set this value to
     * "false".It is important to note that when this property is set to "true," all proxy settings
     * will exclusively originate from environment variableValues, and no partial settings will be
     * obtained from SystemPropertyValues.
     *
     * @param useEnvironmentVariablesValues The option whether to use environment variable values
     * @return This object for method chaining.
     */
    Builder useEnvironmentVariablesValues(Boolean useEnvironmentVariablesValues);

    /**
     * The HTTP scheme to use for connecting to the proxy. Valid values are {@code http} and
     * {@code https}.
     * <p>
     * The client defaults to {@code http} if none is given.
     *
     * @param scheme The proxy scheme.
     * @return This object for method chaining.
     */
    Builder scheme(String scheme);
  }

  /**
   * An SDK-internal implementation of {@link Builder}.
   */
  private static final class DefaultClientProxyConfigurationBuilder implements Builder {

    private URI endpoint;
    private String scheme = "http";
    private Set<String> nonProxyHosts;
    private Boolean useSystemPropertyValues = Boolean.TRUE;
    private Boolean useEnvironmentVariablesValues = Boolean.TRUE;

    @Override
    public Builder endpoint(URI endpoint) {
      if (endpoint != null) {
        Validate.isTrue(isEmpty(endpoint.getUserInfo()),
            "Proxy endpoint user info is not supported.");
        Validate.isTrue(isEmpty(endpoint.getPath()), "Proxy endpoint path is not supported.");
        Validate.isTrue(isEmpty(endpoint.getQuery()), "Proxy endpoint query is not supported.");
        Validate.isTrue(isEmpty(endpoint.getFragment()),
            "Proxy endpoint fragment is not supported.");
      }

      this.endpoint = endpoint;
      return this;
    }

    public void setEndpoint(URI endpoint) {
      endpoint(endpoint);
    }

    @Override
    public Builder nonProxyHosts(Set<String> nonProxyHosts) {
      this.nonProxyHosts = nonProxyHosts != null ? new HashSet<>(nonProxyHosts) : null;
      return this;
    }

    @Override
    public Builder addNonProxyHost(String nonProxyHost) {
      if (this.nonProxyHosts == null) {
        this.nonProxyHosts = new HashSet<>();
      }
      this.nonProxyHosts.add(nonProxyHost);
      return this;
    }

    public void setNonProxyHosts(Set<String> nonProxyHosts) {
      nonProxyHosts(nonProxyHosts);
    }

    @Override
    public Builder useSystemPropertyValues(Boolean useSystemPropertyValues) {
      this.useSystemPropertyValues = useSystemPropertyValues;
      return this;
    }

    public void setUseSystemPropertyValues(Boolean useSystemPropertyValues) {
      useSystemPropertyValues(useSystemPropertyValues);
    }

    @Override
    public Builder useEnvironmentVariablesValues(Boolean useEnvironmentVariablesValues) {
      this.useEnvironmentVariablesValues = useEnvironmentVariablesValues;
      return this;
    }

    @Override
    public Builder scheme(String scheme) {
      this.scheme = scheme;
      return this;
    }

    public void setScheme(String scheme) {
      scheme(scheme);
    }

    public void setUseEnvironmentVariablesValues(Boolean useEnvironmentVariablesValues) {
      useEnvironmentVariablesValues(useEnvironmentVariablesValues);
    }

    @Override
    public ProxyConfiguration build() {
      return new ProxyConfiguration(this);
    }
  }
}