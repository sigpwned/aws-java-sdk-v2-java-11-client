package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import static java.util.Objects.requireNonNull;

import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.ResponseInfo;
import org.reactivestreams.FlowAdapters;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

/**
 * Adapts the call-level protocol from Java 11 Client to SDK
 */
@SdkInternalApi
final class JavaHttpClientResponseAdapter implements BodyHandler<Void> {

  private final SdkAsyncHttpResponseHandler responseHandler;

  public JavaHttpClientResponseAdapter(SdkAsyncHttpResponseHandler responseHandler) {
    this.responseHandler = requireNonNull(responseHandler);
  }

  @Override
  public BodySubscriber<Void> apply(ResponseInfo responseInfo) {
    SdkHttpResponse headers = SdkHttpFullResponse.builder()
        .headers(responseInfo.headers().map())
        .statusCode(responseInfo.statusCode())
        .build(); // get the headers from responseInfo

    getResponseHandler().onHeaders(headers);

    JavaHttpClientBodyProcessor processor = new JavaHttpClientBodyProcessor();

    BodySubscriber<Void> result = HttpResponse.BodySubscribers.fromSubscriber(processor);

    getResponseHandler().onStream(FlowAdapters.toPublisher(processor));

    return result;
  }

  public SdkAsyncHttpResponseHandler getResponseHandler() {
    return responseHandler;
  }
}