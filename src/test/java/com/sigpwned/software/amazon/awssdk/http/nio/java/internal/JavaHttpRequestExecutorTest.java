package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.*;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sigpwned.software.amazon.awssdk.http.nio.java.RecordingResponseHandler;
import io.reactivex.Flowable;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

public class JavaHttpRequestExecutorTest {

  private final RecordingNetworkTrafficListener wiremockTrafficListener = new RecordingNetworkTrafficListener();

  @Test
  public void methodGetTest() {
    RequestCheck(SdkHttpMethod.GET, "");
  }

  @Test
  public void methodPutTest() {
    String body = randomAlphabetic(10);
    RequestCheck(SdkHttpMethod.PUT, body);
  }

  @Test
  public void methodPostTest() {
    String body = randomAlphabetic(32);
    RequestCheck(SdkHttpMethod.POST, body);
  }

  @Test
  public void methodDeleteTest() {
    RequestCheck(SdkHttpMethod.DELETE, "");
  }

  @Test
  public void methodHeadTest() {
    RequestCheck(SdkHttpMethod.HEAD, "");
  }

  @Test
  public void methodPatchTest() {
    String body = randomAlphabetic(10);
    RequestCheck(SdkHttpMethod.PATCH, body);
  }

  @Test
  public void methodOptionTest() {
    RequestCheck(SdkHttpMethod.OPTIONS, "");
  }

  private void RequestCheck(SdkHttpMethod method, String body) {
    // Given
    URI uri = URI.create("http://localhost:" + 8080);
    Map<String, List<String>> headers = new HashMap<>();
    headers.put("foo", Collections.singletonList("bar"));
    SdkHttpFullRequest request = createRequest(uri, "", body, method, emptyMap(), headers);
    RecordingResponseHandler recorder = new RecordingResponseHandler();

    // Mock the HttpClient to capture the request flows into this client
    MyHttpClient mockJavaHttpClient = mock(MyHttpClient.class);

    JavaHttpClientRequestExecutor javaHttpRequestExecutor = new JavaHttpClientRequestExecutor(
        mockJavaHttpClient, Duration.ofSeconds(30));
    CompletableFuture<HttpResponse<Void>> responseFuture = new CompletableFuture<>();
    when(mockJavaHttpClient.sendAsync(any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class))).thenReturn(responseFuture);
    javaHttpRequestExecutor.execute(
        AsyncExecuteRequest.builder().request(request).requestContentPublisher(createProvider(body))
            .responseHandler(recorder).build());

    // Then, check whether the request passed to the executor has been generated correctly
    Mockito.verify(mockJavaHttpClient).sendAsync(argThat(
        (HttpRequest httpRequest) -> httpRequest.uri().toString().equals(uri.toString())
            && httpRequest.method().equals(method.toString()) && httpRequest.headers().map()
            .toString().equals(headers.toString())), any(HttpResponse.BodyHandler.class));
  }

  @Rule
  public WireMockRule mockServer = new WireMockRule(
      wireMockConfig().dynamicPort().dynamicHttpsPort()
          .networkTrafficListener(wiremockTrafficListener), false);

  @Test
  public void handlerReceiveSuccessfullyTest() {
    String body = randomAlphabetic(32);
    successfullyExecuteTest(SdkHttpMethod.POST, body);
  }

  @Test
  public void handlerReceiveFailedTest() {
    String body = randomAlphabetic(32);
    unsuccessfullyExecuteTest(SdkHttpMethod.POST, body);
  }

  private void successfullyExecuteTest(SdkHttpMethod method, String body)
      throws CompletionException {
    // Check the CompletableFuture when the request is executed successfully
    stubFor(WireMock.any(urlPathEqualTo("/")).willReturn(
        aResponse().withBody(body).withHeader("Some-Header", "With Value").withFixedDelay(2)));

    URI uri = URI.create("http://localhost:" + mockServer.port());
    Map<String, List<String>> headers = new HashMap<>();
    headers.put("foo", Collections.singletonList("bar"));
    SdkHttpRequest request = createRequest(uri, "/", body, method, emptyMap(), headers);
    RecordingResponseHandler recorder = new RecordingResponseHandler();

    HttpClient javaHttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(1)).build();
    JavaHttpClientRequestExecutor javaHttpRequestExecutor = new JavaHttpClientRequestExecutor(
        javaHttpClient, Duration.ofSeconds(1));

    javaHttpRequestExecutor.execute(
        AsyncExecuteRequest.builder().request(request).requestContentPublisher(createProvider(body))
            .responseHandler(recorder).build()).join();

    assertThat(wiremockTrafficListener.response.toString().endsWith(body));
  }


  private void unsuccessfullyExecuteTest(SdkHttpMethod method, String body)
      throws CompletionException {
    // Check the CompletableFuture when the request is executed unsuccessfully
    try {
      stubFor(WireMock.any(urlPathEqualTo("/"))
          .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

      URI uri = URI.create("http://localhost:" + mockServer.port());
      Map<String, List<String>> headers = new HashMap<>();
      headers.put("foo", Collections.singletonList("bar"));
      SdkHttpRequest request = createRequest(uri, "/", body, method, emptyMap(), headers);
      RecordingResponseHandler recorder = new RecordingResponseHandler();

      HttpClient javaHttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
          .connectTimeout(Duration.ofSeconds(20)).build();

      JavaHttpClientRequestExecutor javaHttpRequestExecutor = new JavaHttpClientRequestExecutor(
          javaHttpClient, Duration.ofSeconds(30));

      javaHttpRequestExecutor.execute(AsyncExecuteRequest.builder().request(request)
          .requestContentPublisher(createProvider(body)).responseHandler(recorder).build()).join();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private SdkHttpFullRequest createRequest(URI uri) {
    return createRequest(uri, "/", "", SdkHttpMethod.GET, emptyMap(), emptyMap());
  }

  private SdkHttpFullRequest createRequest(URI uri, String resourcePath, String body,
      SdkHttpMethod method, Map<String, String> params, Map<String, List<String>> headers) {
    String contentLength = body == null ? null : String.valueOf(body.getBytes(UTF_8).length);
    return SdkHttpFullRequest.builder().uri(uri).method(method).encodedPath(resourcePath)
        .applyMutation(b -> params.forEach(b::putRawQueryParameter)).applyMutation(b -> {
          b.putHeader("Host", uri.getHost());
          if (contentLength != null) {
            b.putHeader("Content-Length", contentLength);
          }
        }).applyMutation(b -> headers.forEach(b::putHeader)).build();
  }


  private SdkHttpContentPublisher createProvider(String body) {

    return new SdkHttpContentPublisher() {
      Flowable<ByteBuffer> flowable = Flowable.just(ByteBuffer.wrap(body.getBytes()));

      @Override
      public Optional<Long> contentLength() {
        return Optional.of((long) body.length());
      }

      @Override
      public void subscribe(Subscriber<? super ByteBuffer> s) {
        flowable.subscribeWith(s);
      }
    };
  }


  private static class RecordingNetworkTrafficListener implements WiremockNetworkTrafficListener {

    private final StringBuilder requests = new StringBuilder();
    private final StringBuilder response = new StringBuilder();

    @Override
    public void opened(Socket socket) {

    }

    @Override
    public void incoming(Socket socket, ByteBuffer byteBuffer) {
      requests.append(StandardCharsets.UTF_8.decode(byteBuffer));
    }

    @Override
    public void outgoing(Socket socket, ByteBuffer byteBuffer) {
      response.append(UTF_8.decode(byteBuffer));
    }

    @Override
    public void closed(Socket socket) {

    }

    public void reset() {
      requests.setLength(0);
    }
  }

  // The constructor of HttpClient is protected, so to mock it here an auxiliary class is added
  private static abstract class MyHttpClient extends HttpClient {

    public MyHttpClient() {
      super();
    }
  }
}