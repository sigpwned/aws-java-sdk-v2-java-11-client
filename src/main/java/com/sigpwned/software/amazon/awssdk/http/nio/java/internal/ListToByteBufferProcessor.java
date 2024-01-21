package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.PublishProcessor;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A processor connects the publisher (HttpResponse.BodyHandler) and subscriber (SdkAsyncHttpResponseHandler).
 * Since jdk 11 doesn't support creating HttpResponse.BodyHandler with ByteBuffer, but supports that with
 * List&lt;ByteBuffer&gt;, while SDK expects a ByteBuffer-typed publisher, so this class is to convert List
 * object into ByteBuffer, then the flow is like publisher - processor - subscriber.
 */
@SdkInternalApi
public final class ListToByteBufferProcessor implements Subscriber<List<ByteBuffer>> {

  private PublishProcessor<List<ByteBuffer>> processor;
  private Flowable<ByteBuffer> publisherToSdk;

  private final CompletableFuture<Void> terminated = new CompletableFuture<>();

  public ListToByteBufferProcessor() {
    this.processor = PublishProcessor.create();
    this.publisherToSdk = processor.map(ListToByteBufferProcessor::convertListToByteBuffer);
  }

  public static ByteBuffer convertListToByteBuffer(List<ByteBuffer> list) {
    int bodyPartSize = list.stream().mapToInt(Buffer::remaining).sum();
    ByteBuffer processedByteBuffer = ByteBuffer.allocate(bodyPartSize);
    list.forEach(processedByteBuffer::put);
    processedByteBuffer.flip();
    return processedByteBuffer;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    processor.onSubscribe(subscription);
  }

  @Override
  public void onNext(List<ByteBuffer> byteBufferList) {
    processor.onNext(byteBufferList);

  }

  @Override
  public void onError(Throwable throwable) {
    terminated.completeExceptionally(throwable);
    processor.onError(throwable);
  }

  @Override
  public void onComplete() {
    terminated.complete(null);
    processor.onComplete();
  }

  Flowable<ByteBuffer> getPublisherToSdk() {
    return publisherToSdk;
  }

  CompletableFuture<Void> getTerminated() {
    return terminated;
  }

}