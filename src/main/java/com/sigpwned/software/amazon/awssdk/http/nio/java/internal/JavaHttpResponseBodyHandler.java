package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import java.net.http.HttpResponse;
import org.reactivestreams.FlowAdapters;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

/**
 * An implementation of {@link HttpResponse.BodyHandler}, connecting the ListToByteBufferProcessor and
 * SdkAsyncHttpResponseHandler.
 */
@SdkProtectedApi
final class JavaHttpResponseBodyHandler implements HttpResponse.BodyHandler<Void> {
  private SdkAsyncHttpResponseHandler responseHandler;
  private ListToByteBufferProcessor listToByteBufferProcessor;

  JavaHttpResponseBodyHandler(SdkAsyncHttpResponseHandler responseHandler,
      ListToByteBufferProcessor listToByteBufferProcessor) {
    this.responseHandler = responseHandler;
    this.listToByteBufferProcessor = listToByteBufferProcessor;
  }

  @Override
  public HttpResponse.BodySubscriber<Void> apply(HttpResponse.ResponseInfo responseInfo) {
    SdkHttpResponse headers = SdkHttpFullResponse.builder()
        .headers(responseInfo.headers().map())
        .statusCode(responseInfo.statusCode())
        .build(); // get the headers from responseInfo
    responseHandler.onHeaders(headers);
    responseHandler.onStream(listToByteBufferProcessor.getPublisherToSdk());
    return HttpResponse.BodySubscribers.fromSubscriber(FlowAdapters.toFlowSubscriber(listToByteBufferProcessor));
  }

}