package com.sigpwned.software.amazon.awssdk.http.nio.java;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SimpleSubscriber;

public final class RecordingResponseHandler implements SdkAsyncHttpResponseHandler {

  List<SdkHttpResponse> responses = new ArrayList<>();
  private StringBuilder bodyParts = new StringBuilder();
  CompletableFuture<Void> completeFuture = new CompletableFuture<>();

  @Override
  public void onHeaders(SdkHttpResponse response) {
    responses.add(response);
  }

  @Override
  public void onStream(Publisher<ByteBuffer> publisher) {
    publisher.subscribe(new SimpleSubscriber(byteBuffer -> {
      byte[] b = new byte[byteBuffer.remaining()];
      byteBuffer.duplicate().get(b);
      bodyParts.append(new String(b, StandardCharsets.UTF_8));
    }) {

      @Override
      public void onError(Throwable t) {
        completeFuture.completeExceptionally(t);
      }

      @Override
      public void onComplete() {
        completeFuture.complete(null);
      }
    });
  }

  @Override
  public void onError(Throwable error) {
    completeFuture.completeExceptionally(error);
  }

  public String fullResponseAsString() {
    return bodyParts.toString();
  }
}