package com.sigpwned.software.amazon.awssdk.http.java11;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Tests for {@link ProxyConfiguration}.
 *
 * @see <a
 * href="https://github.com/aws/aws-sdk-java-v2/blob/3748964d2793d732eae6dfead9691327fdd14569/http-clients/url-connection-client/src/test/java/software/amazon/awssdk/http/urlconnection/ProxyConfigurationTest.java">
 * https://github.com/aws/aws-sdk-java-v2/blob/3748964d2793d732eae6dfead9691327fdd14569/http-clients/url-connection-client/src/test/java/software/amazon/awssdk/http/urlconnection/ProxyConfigurationTest.java</a>
 */
public class ProxyConfigurationTest {

  @AfterAll
  public static void cleanup() {
    clearProxyProperties();
  }

  private static void clearProxyProperties() {
    System.clearProperty("http.proxyHost");
    System.clearProperty("http.proxyPort");
    System.clearProperty("http.nonProxyHosts");
    System.clearProperty("http.proxyUser");
    System.clearProperty("http.proxyPassword");
  }

  @BeforeEach
  public void setup() {
    clearProxyProperties();
  }

  @Test
  public void testEndpointValues_SystemPropertyEnabled() {
    String host = "foo.com";
    int port = 7777;
    System.setProperty("http.proxyHost", host);
    System.setProperty("http.proxyPort", Integer.toString(port));

    ProxyConfiguration config = ProxyConfiguration.builder().useSystemPropertyValues(true).build();

    assertThat(config.host()).isEqualTo(host);
    assertThat(config.port()).isEqualTo(port);
    assertThat(config.scheme()).isEqualTo("http");
  }

  @Test
  public void testEndpointValues_SystemPropertyDisabled() {
    ProxyConfiguration config = ProxyConfiguration.builder()
        .endpoint(URI.create("http://localhost:1234"))
        .useSystemPropertyValues(Boolean.FALSE)
        .build();

    assertThat(config.host()).isEqualTo("localhost");
    assertThat(config.port()).isEqualTo(1234);
    assertThat(config.scheme()).isEqualTo("http");
  }

  @Test
  public void testProxyConfigurationWithSystemPropertyDisabled() throws Exception {
    Set<String> nonProxyHosts = new HashSet<>();
    nonProxyHosts.add("foo.com");

    // system property should not be used
    System.setProperty("http.proxyHost", "foo.com");
    System.setProperty("http.proxyPort", "5555");
    System.setProperty("http.nonProxyHosts", "bar.com");

    ProxyConfiguration config = ProxyConfiguration.builder()
        .endpoint(URI.create("http://localhost:1234"))
        .nonProxyHosts(nonProxyHosts)
        .useSystemPropertyValues(Boolean.FALSE)
        .build();

    assertThat(config.host()).isEqualTo("localhost");
    assertThat(config.port()).isEqualTo(1234);
    assertThat(config.nonProxyHosts()).isEqualTo(nonProxyHosts);
  }

  @Test
  public void testProxyConfigurationWithSystemPropertyEnabled() throws Exception {
    Set<String> nonProxyHosts = new HashSet<>();
    nonProxyHosts.add("foo.com");

    // system property should not be used
    System.setProperty("http.proxyHost", "foo.com");
    System.setProperty("http.proxyPort", "5555");
    System.setProperty("http.nonProxyHosts", "bar.com");

    ProxyConfiguration config = ProxyConfiguration.builder()
        .nonProxyHosts(nonProxyHosts)
        .build();

    assertThat(config.nonProxyHosts()).isEqualTo(nonProxyHosts);
    assertThat(config.host()).isEqualTo("foo.com");
  }

  @Test
  public void testProxyConfigurationWithoutNonProxyHosts_toBuilder_shouldNotThrowNPE() {
    ProxyConfiguration proxyConfiguration =
        ProxyConfiguration.builder()
            .endpoint(URI.create("http://localhost:4321"))
            .build();

    assertThat(proxyConfiguration.toBuilder()).isNotNull();
  }
}