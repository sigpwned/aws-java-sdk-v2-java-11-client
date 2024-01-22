package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import static java.util.Objects.requireNonNull;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import org.reactivestreams.FlowAdapters;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

/**
 * Maps an SDK request to a Java 11 HttpRequest.
 */
@SdkInternalApi
final class JavaHttpClientRequestMapper {

  private final Duration responseTimeout;

  public JavaHttpClientRequestMapper(Duration responseTimeout) {
    this.responseTimeout = requireNonNull(responseTimeout);
  }


  /**
   * Creates the Java 11 HttpRequest with HttpRequest.Builder according to the configurations in the
   * AsyncExecuteRequest
   *
   * @return HttpRequest object
   */
  public HttpRequest toJavaHttpClientRequest(AsyncExecuteRequest sdkExecuteRequest) {
    final SdkHttpRequest sdkRequest = sdkExecuteRequest.request();

    final String httpMethod = requestMethod(sdkRequest.method());
    final BodyPublisher httpEntity = bodyPublisher(sdkExecuteRequest);

    final HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().uri(sdkRequest.getUri())
        .method(httpMethod, httpEntity).timeout(getResponseTimeout());

    // Set all non-restricted headers
    sdkRequest.headers().entrySet().stream()
        .flatMap(e -> e.getValue().stream().map(v -> Map.entry(e.getKey(), v)))
        .filter(e -> !isRestrictedHeader(e.getKey()))
        .forEach(e -> httpRequestBuilder.header(e.getKey(), e.getValue()));

    return httpRequestBuilder.build();
  }

  private static String requestMethod(SdkHttpMethod sdkhttpmethod) {
    return sdkhttpmethod.name();
  }

  private static HttpRequest.BodyPublisher bodyPublisher(AsyncExecuteRequest sdkExecuteRequest) {
    final SdkHttpContentPublisher sdkHttpContentPublisher = sdkExecuteRequest.requestContentPublisher();
    final Optional<Long> maybeContentLength = sdkHttpContentPublisher.contentLength();
    if (!sdkExecuteRequest.fullDuplex() && maybeContentLength.orElse(0L) == 0L) {
      // TODO Should this be an AND as written, or an OR for no body in either case?
      return BodyPublishers.noBody();
    }

    final Flow.Publisher<ByteBuffer> flowPublisher = FlowAdapters.toFlowPublisher(
        sdkHttpContentPublisher);

    if (maybeContentLength.isPresent()) {
      // TODO Address the issue of actual content is longer than the content length
      long contentLength = maybeContentLength.get();
      return BodyPublishers.fromPublisher(flowPublisher, contentLength);
    } else {
      return BodyPublishers.fromPublisher(flowPublisher);
    }
  }

  /**
   * In Jdk 11, these headers filtered below are restricted and not allowed to be customized
   */
  private static boolean isRestrictedHeader(String headerName) {
    // TODO Is this all of the restricted headers?
    return headerName.equalsIgnoreCase("Host") || headerName.equalsIgnoreCase("Content-Length")
        || headerName.equalsIgnoreCase("Expect");
  }

  private Duration getResponseTimeout() {
    return responseTimeout;
  }
}