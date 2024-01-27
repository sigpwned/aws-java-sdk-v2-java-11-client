package com.sigpwned.software.amazon.awssdk.http.java11.internal;

import com.sigpwned.software.amazon.awssdk.http.java11.util.ByteBuffers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

/**
 * A {@link Processor} that converts a {@link List} of {@link ByteBuffer}s to a single
 * {@link ByteBuffer}. Exists solely to correct the impedence mismatch between the SDK's
 * {@link SdkAsyncHttpResponseHandler} and the Java 11 HTTP client's
 * {@link java.net.http.HttpRequest.BodyPublisher} publish and subscribe types.
 */
@SdkInternalApi
class JavaHttpClientBodyProcessor extends SubmissionPublisher<ByteBuffer> implements
    Processor<List<ByteBuffer>, ByteBuffer> {

  private Subscription subscription;

  @Override
  public void onSubscribe(Subscription newSubscription) {
    if (subscription != null) {
      newSubscription.cancel();
    } else {
      subscription = newSubscription;
      subscription.request(1L);
    }
  }

  @Override
  public void onNext(List<ByteBuffer> item) {
    // It's too bad that we have to do a copy here, but because of the semantics of reactive
    // streams, we have to make one onNext() call for each onNext() call we receive.
    submit(ByteBuffers.concat(item));
    if (subscription != null) {
      subscription.request(1L);
    }
  }

  @Override
  public void onError(Throwable throwable) {
    closeExceptionally(throwable);
  }

  @Override
  public void onComplete() {
    close();
  }
}
