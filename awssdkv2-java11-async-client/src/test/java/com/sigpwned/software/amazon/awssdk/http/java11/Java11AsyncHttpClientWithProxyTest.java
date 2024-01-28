package com.sigpwned.software.amazon.awssdk.http.java11;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.*;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.sigpwned.software.amazon.awssdk.http.java11.Java11AsyncHttpClient.TrustAllTrustManager;
import com.sigpwned.software.amazon.awssdk.http.java11.util.ByteBuffers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.TrustManager;
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SimpleSubscriber;

/**
 * Confirm that HTTP proxying works. Note that we're not using HTTPS proxies here, as those are a
 * completely different animal.
 */
public class Java11AsyncHttpClientWithProxyTest {

  @Rule
  public WireMockRule proxyTarget = new WireMockRule(wireMockConfig().port(8080));

  @Rule
  public WireMockRule proxySource = new WireMockRule(wireMockConfig().port(9090));

  /**
   * Confirm that we proxy as expected when a proxy is configured and the target IS NOT in the list
   * of non-proxy hosts
   */
  @Test
  public void proxyRewriteTest() throws Exception {
    // Set up our "target" endpoint
    proxyTarget.stubFor(get("/my/resource").willReturn(
        ok().withHeader("Content-Type", "application/json")
            .withBody("{\"message\":\"Hello world!\"}")));

    // Set up our "proxy" endpoint
    proxySource.stubFor(get("/my/resource").willReturn(
        aResponse().proxiedFrom(format("http://localhost:%d", proxyTarget.port()))));

    // Set up our client. Note our list of non-proxy hosts DOES NOT contain the target host.
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder().proxyConfiguration(
            ProxyConfiguration.builder()
                .endpoint(URI.create(format("http://localhost:%d", proxySource.port())))
                .nonProxyHosts(singleton("example.com")).build())
        .tlsTrustManagersProvider(() -> new TrustManager[]{TrustAllTrustManager.INSTANCE}).build();

    // Set up the request. Note that the host is the TARGET, not the PROXY. Also, the target IS NOT
    // in the list of non-proxy hosts. Therefore, we expect the client to re-write the request to
    // target the proxy instead of the original request target.
    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    final CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpFullRequest.builder().method(SdkHttpMethod.GET)
                .uri(URI.create(format("http://localhost:%d/my/resource", proxyTarget.port()))).build())
        .fullDuplex(false).responseHandler(new SdkAsyncHttpResponseHandler() {
          @Override
          public void onHeaders(SdkHttpResponse hs) {
            responseBuffer.set(hs);
          }

          @Override
          public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new SimpleSubscriber(byteBuffer -> {
              try {
                bodyBuffer.write(ByteBuffers.toByteArray(byteBuffer));
              } catch (IOException e) {
                // We're using a ByteArrayOutputStream, so this should never happen
                throw new UncheckedIOException(e);
              }
            }));
          }

          @Override
          public void onError(Throwable error) {
            error.printStackTrace(System.err);
          }
        }).build());

    future.get();

    final SdkHttpResponse response = responseBuffer.get();
    final String body = bodyBuffer.toString(StandardCharsets.UTF_8);

    // If the response looks right and the proxy has a request, then we're in good shape.
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(body).isEqualTo("{\"message\":\"Hello world!\"}");
    assertThat(proxySource.countRequestsMatching(RequestPattern.everything()).getCount()).isEqualTo(
        1);
  }


  /**
   * Confirm that we DO NOT proxy as expected when a proxy is configured and the target IS in the
   * list of non-proxy hosts
   */
  @Test
  public void proxySkipRewriteTest() throws Exception {
    // Set up our "target" endpoint
    proxyTarget.stubFor(get("/my/resource").willReturn(
        ok().withHeader("Content-Type", "application/json")
            .withBody("{\"message\":\"Hello world!\"}")));

    // Set up our "proxy" endpoint
    proxySource.stubFor(get("/my/resource").willReturn(
        aResponse().proxiedFrom(format("http://localhost:%d", proxyTarget.port()))));

    // Set up our client. Note our list of non-proxy hosts DOES contain the target host.
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder().proxyConfiguration(
            ProxyConfiguration.builder()
                .endpoint(URI.create(format("http://localhost:%d", proxySource.port())))
                .nonProxyHosts(singleton("localhost")).build())
        .tlsTrustManagersProvider(() -> new TrustManager[]{TrustAllTrustManager.INSTANCE}).build();

    // Set up the request. Note that the host is the TARGET, not the PROXY. Also, the target IS
    // in the list of non-proxy hosts. Therefore, we expect the client NOT TO re-write the request
    // to target the proxy instead of the original request target.
    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    final CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpFullRequest.builder().method(SdkHttpMethod.GET)
                .uri(URI.create(format("http://localhost:%d/my/resource", proxyTarget.port()))).build())
        .fullDuplex(false).responseHandler(new SdkAsyncHttpResponseHandler() {
          @Override
          public void onHeaders(SdkHttpResponse hs) {
            responseBuffer.set(hs);
          }

          @Override
          public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new SimpleSubscriber(byteBuffer -> {
              try {
                bodyBuffer.write(ByteBuffers.toByteArray(byteBuffer));
              } catch (IOException e) {
                // We're using a ByteArrayOutputStream, so this should never happen
                throw new UncheckedIOException(e);
              }
            }));
          }

          @Override
          public void onError(Throwable error) {
            error.printStackTrace(System.err);
          }
        }).build());

    future.get();

    final SdkHttpResponse response = responseBuffer.get();
    final String body = bodyBuffer.toString(StandardCharsets.UTF_8);

    // If the response looks right and the proxy DOES NOT have a request, then we're in good shape.
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(body).isEqualTo("{\"message\":\"Hello world!\"}");
    assertThat(proxySource.countRequestsMatching(RequestPattern.everything()).getCount()).isEqualTo(
        0);
  }
}