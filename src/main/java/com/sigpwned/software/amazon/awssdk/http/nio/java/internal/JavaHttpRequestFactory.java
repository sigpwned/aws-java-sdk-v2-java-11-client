package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import com.sigpwned.software.amazon.awssdk.http.nio.java.JavaHttpClientHttpConfigurationOption;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Flow;
import org.reactivestreams.FlowAdapters;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.AttributeMap;

@SdkInternalApi
final class JavaHttpRequestFactory {

  private Duration responseTimeout;

  JavaHttpRequestFactory(AttributeMap serviceDefaultsMap) {
    responseTimeout = serviceDefaultsMap.get(
        JavaHttpClientHttpConfigurationOption.RESPONSE_TIMEOUT);
  }

  private String getRequestMethod(SdkHttpMethod sdkhttpmethod) {
    return sdkhttpmethod.name();
  }


  static HttpRequest.BodyPublisher createBodyPublisher(HttpExecuteRequest httpExecuteRequest) {
    // TODO: Address the issue of actual content is longer than the content length
    if(httpExecuteRequest.contentStreamProvider().isEmpty()){
      return BodyPublishers.noBody();
    }
    ContentStreamProvider contentStreamProvider = httpExecuteRequest.contentStreamProvider().get();
    return BodyPublishers.ofInputStream(contentStreamProvider::newStream);
  }

  /**
   * Creates the Java 11 HttpRequest with HttpRequest.Builder according to the configurations in the
   * AsyncExecuteRequest
   *
   * @return HttpRequest object
   */
  HttpRequest createJavaHttpRequest(HttpExecuteRequest httpExecuteRequest) {
    SdkHttpRequest request = httpExecuteRequest.httpRequest();
    HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();
    httpRequestBuilder.uri(request.getUri());
    httpRequestBuilder.method(getRequestMethod(request.method()),
        createBodyPublisher(httpExecuteRequest));
    //TODO: Check the restricted types of headers

    // In Jdk 11, these headers filtered below are restricted and not allowed to be customized
    request.headers().forEach((name, values) -> {
      if (!name.equals("Host") && !name.equals("Content-Length") && !name.equals("Expect")) {
        httpRequestBuilder.setHeader(name, String.join(",", values));
      }
    });
    httpRequestBuilder.timeout(responseTimeout);

    return httpRequestBuilder.build();
  }


  static HttpRequest.BodyPublisher createBodyPublisher(AsyncExecuteRequest asyncExecuteRequest) {
    // TODO: Address the issue of actual content is longer than the content length
    SdkHttpContentPublisher sdkHttpContentPublisher = asyncExecuteRequest.requestContentPublisher();
    Optional<Long> contentlength = sdkHttpContentPublisher.contentLength();
    Flow.Publisher<ByteBuffer> flowPublisher = FlowAdapters.toFlowPublisher(
        sdkHttpContentPublisher);
    if (!asyncExecuteRequest.fullDuplex() && (contentlength.isEmpty()
        || contentlength.get() == 0)) {
      HttpRequest.BodyPublisher bodyPublisher = BodyPublishers.noBody();
      return bodyPublisher;
    } else {
      return contentlength.map(aLong -> BodyPublishers.fromPublisher(flowPublisher,
          aLong)).orElseGet(() -> BodyPublishers.fromPublisher(flowPublisher));
    }
  }

  /**
   * Creates the Java 11 HttpRequest with HttpRequest.Builder according to the configurations in the
   * AsyncExecuteRequest
   *
   * @return HttpRequest object
   */
  HttpRequest createJavaHttpRequest(AsyncExecuteRequest asyncExecuteRequest) {
    SdkHttpRequest request = asyncExecuteRequest.request();
    HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();
    httpRequestBuilder.uri(request.getUri());
    httpRequestBuilder.method(getRequestMethod(request.method()),
        createBodyPublisher(asyncExecuteRequest));
    //TODO: Check the restricted types of headers

    // In Jdk 11, these headers filtered below are restricted and not allowed to be customized
    request.headers().forEach((name, values) -> {
      if (!name.equals("Host") && !name.equals("Content-Length") && !name.equals("Expect")) {
        httpRequestBuilder.setHeader(name, String.join(",", values));
      }
    });
    httpRequestBuilder.timeout(responseTimeout);

    return httpRequestBuilder.build();
  }
}