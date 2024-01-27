package com.sigpwned.software.amazon.awssdk.http.java11;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.options;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.*;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sigpwned.software.amazon.awssdk.http.java11.Java11AsyncHttpClient.TrustAllTrustManager;
import com.sigpwned.software.amazon.awssdk.http.java11.util.ByteBuffers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.TrustManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.http.async.SimpleSubscriber;

/**
 * <p>
 * Ensure that the JavaHttpClientNioAsyncHttpClient generally works as documented and expected.
 * </p>
 *
 * <p>
 * Specifically, ensure that:
 * </p>
 *
 * <ul>
 *   <li>All HTTP methods are supported</li>
 *   <li>HTTP and HTTPS are supported</li>
 *   <li>
 *     Various configuration parameters are honored, to the extent that the client allows and that
 *     the testing framework(s) AWS has standardized on allow
 *   </li>
 *   <li>
 *     Request bodies work with and without content-length headers
 *   </li>
 * </ul>
 *
 * <p>
 * This suite does not test the underlying Java HTTP Client. That is the responsibility of the Java
 * HTTP Client team. This test only tests the integration between the Java HTTP Client and the AWS
 * SDK for Java.
 * </p>
 *
 * <p>
 * Also, this suite does not test the various and sundry AWS-specific requirements for their HTTP
 * clients. Instead, there is a separate test suite that extends an AWS-provided test suite that
 * handles those cases.
 * </p>
 */
public class JavaHttpClientFunctionalTest {

  /**
   * Not currently used, but perhaps useful in the future.
   */
  public static final RecordingNetworkTrafficListener wiremockTrafficListener = new RecordingNetworkTrafficListener();

  @Rule
  public WireMockRule mockServer = new WireMockRule(
      wireMockConfig().dynamicPort().dynamicHttpsPort()
          .networkTrafficListener(wiremockTrafficListener));

  @Before
  public void setupJavaHttpClientNioAsyncHttpClientWireMockTest() {
    // System.setProperty("jdk.httpclient.HttpClient.log", "all");
    wiremockTrafficListener.reset();
  }

  @After
  public void cleanupJavaHttpClientNioAsyncHttpClientWireMockTest() {
    // System.clearProperty("jdk.httpclient.HttpClient.log");
  }

  /**
   * We should be able to perform the simplest of GET requests
   */
  @Test(timeout = 5000)
  public void smokeTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    stubFor(get("/my/resource").willReturn(ok().withHeader("Content-Type", "application/json")
        .withBody("{\"message\":\"Hello world!\"}")));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
                .method(SdkHttpMethod.GET).build()).fullDuplex(false)
        .responseHandler(new SdkAsyncHttpResponseHandler() {
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

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String body = bodyBuffer.toString(StandardCharsets.UTF_8);

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(body).isEqualTo("{\"message\":\"Hello world!\"}");
  }

  /**
   * Test the connection timeout
   */
  @Ignore
  @Test(timeout = 5000)
  public void connectTimeoutTest() throws Exception {
    // Evidently, this isn't possible with Wiremock anymore.
    // https://stackoverflow.com/a/60192310
  }

  /**
   * Test the read timeout
   */
  @Ignore
  @Test(timeout = 5000)
  public void readTimeoutTest() throws Exception {
    // The HttpClient does not support socket timeout operations.
    // https://stackoverflow.com/a/73204764
    // https://bugs.openjdk.org/browse/JDK-8258397
  }

  /**
   * Test the write timeout
   */
  @Ignore
  @Test(timeout = 5000)
  public void writeTimeoutTest() throws Exception {
    // The HttpClient does not support socket timeout operations.
    // https://stackoverflow.com/a/73204764
    // https://bugs.openjdk.org/browse/JDK-8258397
  }

  /**
   * We should support HTTP
   */
  @Test(timeout = 5000)
  public void httpTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    stubFor(get("/my/resource").willReturn(ok().withHeader("Content-Type", "application/json")
        .withBody("{\"message\":\"Hello world!\"}")));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("http")
                .uri(URI.create(format("http://localhost:%d/my/resource", mockServer.port())))
                .method(SdkHttpMethod.GET).build()).fullDuplex(false)
        .responseHandler(new SdkAsyncHttpResponseHandler() {
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

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String body = bodyBuffer.toString(StandardCharsets.UTF_8);

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(body).isEqualTo("{\"message\":\"Hello world!\"}");
  }

  /**
   * We should support HTTPS
   */
  @Test(timeout = 5000)
  public void httpsTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    stubFor(get("/my/resource").willReturn(ok().withHeader("Content-Type", "application/json")
        .withBody("{\"message\":\"Hello world!\"}")));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https")
                .uri(URI.create(format("https://localhost:%d/my/resource", mockServer.httpsPort())))
                .method(SdkHttpMethod.GET).build()).fullDuplex(false)
        .responseHandler(new SdkAsyncHttpResponseHandler() {
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

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String body = bodyBuffer.toString(StandardCharsets.UTF_8);

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(body).isEqualTo("{\"message\":\"Hello world!\"}");
  }

  /**
   * We should be able to perform the simplest of GET requests
   */
  @Test(timeout = 5000)
  public void connectionRefusedTest() throws Exception {
    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    // We picked a random port. Guaranteed random, by dice roll. https://xkcd.com/221/
    // Seriously, though, if port 1 is actually up and listening, then this test breaks.
    final AtomicInteger calls = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create("http://localhost:1/my/resource"))
                .method(SdkHttpMethod.GET).build()).fullDuplex(false)
        .responseHandler(new SdkAsyncHttpResponseHandler() {
          @Override
          public void onHeaders(SdkHttpResponse hs) {
            calls.incrementAndGet();
          }

          @Override
          public void onStream(Publisher<ByteBuffer> stream) {
            calls.incrementAndGet();
          }

          @Override
          public void onError(Throwable error) {
            calls.incrementAndGet();
          }
        }).build());

    // Wait for the response to complete
    assertThatThrownBy(future::get).isExactlyInstanceOf(ExecutionException.class)
        .hasCauseExactlyInstanceOf(ConnectException.class);
    assertThat(calls.get()).isEqualTo(0);
  }

  /**
   * We should be able to perform the simplest of GET requests
   */
  @Test(timeout = 5000)
  public void requestTimeoutTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    stubFor(get("/my/resource").willReturn(ok().withHeader("Content-Type", "application/json")
        .withBody("{\"message\":\"Hello world!\"}").withFixedDelay(5000)));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()})
        .responseTimeout(Duration.ofMillis(1000L)).build();

    final AtomicInteger calls = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
                .method(SdkHttpMethod.GET).build()).fullDuplex(false)
        .responseHandler(new SdkAsyncHttpResponseHandler() {
          @Override
          public void onHeaders(SdkHttpResponse hs) {
            calls.incrementAndGet();
          }

          @Override
          public void onStream(Publisher<ByteBuffer> stream) {
            calls.incrementAndGet();
          }

          @Override
          public void onError(Throwable error) {
            calls.incrementAndGet();
          }
        }).build());

    // Wait for the response to complete
    assertThatThrownBy(future::get).isExactlyInstanceOf(ExecutionException.class)
        .hasCauseExactlyInstanceOf(HttpTimeoutException.class);
    assertThat(calls.get()).isEqualTo(0);
  }

  /**
   * We should be able to perform GET requests
   */
  @Test(timeout = 5000)
  public void methodHeadTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    stubFor(head(urlPathEqualTo("/my/resource")).willReturn(
        ok().withHeader("Content-Type", "application/json")));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    final AtomicInteger errorsBuffer = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
        SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
            .method(SdkHttpMethod.HEAD).build()).responseHandler(new SdkAsyncHttpResponseHandler() {
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
        errorsBuffer.incrementAndGet();
      }
    }).build());

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String body = bodyBuffer.toString(StandardCharsets.UTF_8);
    final int errors = errorsBuffer.get();

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(body).isEqualTo("");
    assertThat(errors).isEqualTo(0);
  }

  /**
   * We should be able to perform OPTIONS requests
   */
  @Test(timeout = 5000)
  public void methodOptionsTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    stubFor(options(urlPathEqualTo("/my/resource")).willReturn(ok()));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    final AtomicInteger errorsBuffer = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
                .method(SdkHttpMethod.OPTIONS).build())
        .responseHandler(new SdkAsyncHttpResponseHandler() {
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
            errorsBuffer.incrementAndGet();
          }
        }).build());

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String body = bodyBuffer.toString(StandardCharsets.UTF_8);
    final int errors = errorsBuffer.get();

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(body).isEqualTo("");
    assertThat(errors).isEqualTo(0);
  }

  /**
   * We should be able to perform GET requests
   */
  @Test(timeout = 5000)
  public void methodGetTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    stubFor(get("/my/resource").willReturn(ok().withHeader("Content-Type", "application/json")
        .withBody("{\"message\":\"Hello world!\"}")));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    final AtomicInteger errorsBuffer = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
                .method(SdkHttpMethod.GET).build()).fullDuplex(false)
        .responseHandler(new SdkAsyncHttpResponseHandler() {
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
            errorsBuffer.incrementAndGet();
          }
        }).build());

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String body = bodyBuffer.toString(StandardCharsets.UTF_8);
    final int errors = errorsBuffer.get();

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(body).isEqualTo("{\"message\":\"Hello world!\"}");
    assertThat(errors).isEqualTo(0);
  }

  /**
   * We should be able to perform DELETE requests
   */
  @Test(timeout = 5000)
  public void methodDeleteTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    stubFor(delete("/my/resource").willReturn(noContent()));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    final AtomicInteger errorsBuffer = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
                .method(SdkHttpMethod.DELETE).build()).fullDuplex(false)
        .responseHandler(new SdkAsyncHttpResponseHandler() {
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
            errorsBuffer.incrementAndGet();
          }
        }).build());

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String body = bodyBuffer.toString(StandardCharsets.UTF_8);
    final int errors = errorsBuffer.get();

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(204);
    assertThat(body).isEqualTo("");
    assertThat(errors).isEqualTo(0);
  }

  /**
   * We should be able to perform POST requests
   */
  @Test(timeout = 5000)
  public void methodPostTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    final String requestBody = "example";
    stubFor(post("/my/resource").withRequestBody(equalTo(requestBody)).willReturn(
        ok().withHeader("Content-Type", "application/json")
            .withBody("{\"message\":\"Hello world!\"}")));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream responseBodyBuffer = new ByteArrayOutputStream();
    final AtomicInteger errorsBuffer = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
                .method(SdkHttpMethod.POST).build()).fullDuplex(true)
        .requestContentPublisher(new SimpleSdkHttpContentPublisher(requestBodyBytes))
        .responseHandler(new SdkAsyncHttpResponseHandler() {
          @Override
          public void onHeaders(SdkHttpResponse hs) {
            responseBuffer.set(hs);
          }

          @Override
          public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new SimpleSubscriber(byteBuffer -> {
              try {
                responseBodyBuffer.write(ByteBuffers.toByteArray(byteBuffer));
              } catch (IOException e) {
                // We're using a ByteArrayOutputStream, so this should never happen
                throw new UncheckedIOException(e);
              }
            }));
          }

          @Override
          public void onError(Throwable error) {
            error.printStackTrace(System.err);
            errorsBuffer.incrementAndGet();
          }
        }).build());

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String responseBody = responseBodyBuffer.toString(StandardCharsets.UTF_8);
    final int errors = errorsBuffer.get();

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(responseBody).isEqualTo("{\"message\":\"Hello world!\"}");
    assertThat(errors).isEqualTo(0);
  }

  /**
   * We should be able to perform PUT requests
   */
  @Test(timeout = 5000)
  public void methodPutLengthTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    final String requestBody = "example";
    stubFor(put("/my/resource").withRequestBody(equalTo(requestBody)).willReturn(
        ok().withHeader("Content-Type", "application/json")
            .withBody("{\"message\":\"Hello world!\"}")));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream responseBodyBuffer = new ByteArrayOutputStream();
    final AtomicInteger errorsBuffer = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
                .method(SdkHttpMethod.PUT).build()).fullDuplex(true)
        .requestContentPublisher(new SimpleSdkHttpContentPublisher(requestBodyBytes))
        .responseHandler(new SdkAsyncHttpResponseHandler() {
          @Override
          public void onHeaders(SdkHttpResponse hs) {
            responseBuffer.set(hs);
          }

          @Override
          public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new SimpleSubscriber(byteBuffer -> {
              try {
                responseBodyBuffer.write(ByteBuffers.toByteArray(byteBuffer));
              } catch (IOException e) {
                // We're using a ByteArrayOutputStream, so this should never happen
                throw new UncheckedIOException(e);
              }
            }));
          }

          @Override
          public void onError(Throwable error) {
            error.printStackTrace(System.err);
            errorsBuffer.incrementAndGet();
          }
        }).build());

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String responseBody = responseBodyBuffer.toString(StandardCharsets.UTF_8);
    final int errors = errorsBuffer.get();

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(responseBody).isEqualTo("{\"message\":\"Hello world!\"}");
    assertThat(errors).isEqualTo(0);
  }


  /**
   * We should be able to perform PATCH requests
   */
  @Test(timeout = 5000)
  public void methodPatchLengthTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    final String requestBody = "example";
    stubFor(patch(urlPathEqualTo("/my/resource")).withRequestBody(equalTo(requestBody)).willReturn(
        ok().withHeader("Content-Type", "application/json")
            .withBody("{\"message\":\"Hello world!\"}")));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream responseBodyBuffer = new ByteArrayOutputStream();
    final AtomicInteger errorsBuffer = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
                .method(SdkHttpMethod.PATCH).build()).fullDuplex(true)
        .requestContentPublisher(new SimpleSdkHttpContentPublisher(requestBodyBytes))
        .responseHandler(new SdkAsyncHttpResponseHandler() {
          @Override
          public void onHeaders(SdkHttpResponse hs) {
            responseBuffer.set(hs);
          }

          @Override
          public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new SimpleSubscriber(byteBuffer -> {
              try {
                responseBodyBuffer.write(ByteBuffers.toByteArray(byteBuffer));
              } catch (IOException e) {
                // We're using a ByteArrayOutputStream, so this should never happen
                throw new UncheckedIOException(e);
              }
            }));
          }

          @Override
          public void onError(Throwable error) {
            error.printStackTrace(System.err);
            errorsBuffer.incrementAndGet();
          }
        }).build());

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String responseBody = responseBodyBuffer.toString(StandardCharsets.UTF_8);
    final int errors = errorsBuffer.get();

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(responseBody).isEqualTo("{\"message\":\"Hello world!\"}");
    assertThat(errors).isEqualTo(0);
  }

  /**
   * We should be able to perform an entity-bearing request WITH a Content-Length header
   */
  @Test(timeout = 5000)
  public void requestBodyWithContentLengthTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    final String requestBody = "example";
    stubFor(post("/my/resource").withRequestBody(equalTo(requestBody)).willReturn(
        ok().withHeader("Content-Type", "application/json")
            .withBody("{\"message\":\"Hello world!\"}")));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream responseBodyBuffer = new ByteArrayOutputStream();
    final AtomicInteger errorsBuffer = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
                .method(SdkHttpMethod.POST).build()).fullDuplex(true)
        .requestContentPublisher(new SimpleSdkHttpContentPublisher(requestBodyBytes, true))
        .responseHandler(new SdkAsyncHttpResponseHandler() {
          @Override
          public void onHeaders(SdkHttpResponse hs) {
            responseBuffer.set(hs);
          }

          @Override
          public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new SimpleSubscriber(byteBuffer -> {
              try {
                responseBodyBuffer.write(ByteBuffers.toByteArray(byteBuffer));
              } catch (IOException e) {
                // We're using a ByteArrayOutputStream, so this should never happen
                throw new UncheckedIOException(e);
              }
            }));
          }

          @Override
          public void onError(Throwable error) {
            error.printStackTrace(System.err);
            errorsBuffer.incrementAndGet();
          }
        }).build());

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String responseBody = responseBodyBuffer.toString(StandardCharsets.UTF_8);
    final int errors = errorsBuffer.get();

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(responseBody).isEqualTo("{\"message\":\"Hello world!\"}");
    assertThat(errors).isEqualTo(0);
  }


  /**
   * We should be able to perform an entity-bearing request WITHOUT a Content-Length header
   */
  @Test(timeout = 5000)
  public void requestBodyWithoutContentLengthTest() throws Exception {
    // Setup the WireMock mapping stub for the test
    final String requestBody = "example";
    stubFor(post("/my/resource").withRequestBody(equalTo(requestBody)).willReturn(
        ok().withHeader("Content-Type", "application/json")
            .withBody("{\"message\":\"Hello world!\"}")));

    mockServer.start();

    // Set up request (with HTTP Client embedded in Java 11+)
    SdkAsyncHttpClient client = Java11AsyncHttpClient.builder()
        .tlsTrustManagersProvider(() -> new TrustManager[]{new TrustAllTrustManager()}).build();

    final byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream responseBodyBuffer = new ByteArrayOutputStream();
    final AtomicInteger errorsBuffer = new AtomicInteger(0);
    CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder().request(
            SdkHttpRequest.builder().protocol("https").uri(URI.create(mockServer.url("/my/resource")))
                .method(SdkHttpMethod.POST).build()).fullDuplex(true)
        .requestContentPublisher(new SimpleSdkHttpContentPublisher(requestBodyBytes, false))
        .responseHandler(new SdkAsyncHttpResponseHandler() {
          @Override
          public void onHeaders(SdkHttpResponse hs) {
            responseBuffer.set(hs);
          }

          @Override
          public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new SimpleSubscriber(byteBuffer -> {
              try {
                responseBodyBuffer.write(ByteBuffers.toByteArray(byteBuffer));
              } catch (IOException e) {
                // We're using a ByteArrayOutputStream, so this should never happen
                throw new UncheckedIOException(e);
              }
            }));
          }

          @Override
          public void onError(Throwable error) {
            error.printStackTrace(System.err);
            errorsBuffer.incrementAndGet();
          }
        }).build());

    // Wait for the response to complete
    future.get();

    // Collect our body as a string
    final SdkHttpResponse response = responseBuffer.get();
    final String responseBody = responseBodyBuffer.toString(StandardCharsets.UTF_8);
    final int errors = errorsBuffer.get();

    // Make sure everything looks right
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(responseBody).isEqualTo("{\"message\":\"Hello world!\"}");
    assertThat(errors).isEqualTo(0);
  }


  /**
   * A simple implementation of a {@link SdkHttpContentPublisher} that publishes a single
   * {@link ByteBuffer} and completes.
   *
   * @see software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher
   */
  private static class SimpleSdkHttpContentPublisher implements SdkHttpContentPublisher {

    private final byte[] content;

    private final boolean hasContentLength;

    public SimpleSdkHttpContentPublisher(byte[] content) {
      this(content, true);
    }

    public SimpleSdkHttpContentPublisher(byte[] content, boolean hasContentLength) {
      this.content = requireNonNull(content);
      this.hasContentLength = hasContentLength;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
      subscriber.onSubscribe(new SimpleSubscription(subscriber, content));
    }

    @Override
    public Optional<Long> contentLength() {
      return hasContentLength ? Optional.of((long) content.length) : Optional.empty();
    }
  }

  /**
   * A simple implementation of a {@link Subscription} that publishes a single {@link ByteBuffer}
   * and completes. This is used to publish the content of a {@link SdkHttpContentPublisher}. Ripped
   * from {@link software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher}.
   */
  private static class SimpleSubscription implements Subscription {

    private final Subscriber<? super ByteBuffer> subscriber;
    private final byte[] content;
    private boolean running;

    private SimpleSubscription(Subscriber<? super ByteBuffer> subscriber, byte[] content) {
      this.subscriber = requireNonNull(subscriber);
      this.content = requireNonNull(content);
      this.running = true;
    }

    public void request(long n) {
      if (running) {
        running = false;
        if (n <= 0L) {
          subscriber.onError(new IllegalArgumentException("Demand must be positive"));
        } else {
          subscriber.onNext(ByteBuffer.wrap(content));
          subscriber.onComplete();
        }
      }
    }

    public void cancel() {
      this.running = false;
    }
  }
}
