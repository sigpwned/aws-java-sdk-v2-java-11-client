package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import com.sigpwned.software.amazon.awssdk.http.nio.java.util.ByteBuffers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
class JavaHttpClientBodyProcessor implements Processor<List<ByteBuffer>, ByteBuffer> {

  private Subscriber<? super ByteBuffer> subscriber;
  private Subscription subscription;

  @Override
  public void subscribe(Subscriber<? super ByteBuffer> newSubscriber) {
    if (this.subscriber != null) {
      subscriber.onError(new IllegalStateException("already subscribed"));
    } else {
      subscriber = newSubscriber;
      subscriber.onSubscribe(new Subscription() {
        @Override
        public void request(long n) {
          if (subscription != null) {
            subscription.request(n);
          }
        }

        @Override
        public void cancel() {
          if (subscription != null) {
            subscription.cancel();
          }
        }
      });
    }
  }

  @Override
  public void onSubscribe(Subscription newSubscription) {
    if (subscription != null) {
      newSubscription.cancel();
    } else {
      subscription = newSubscription;
    }
  }

  @Override
  public void onNext(List<ByteBuffer> item) {
    if (subscriber != null) {
      subscriber.onNext(ByteBuffers.concat(item));
    }
  }

  @Override
  public void onError(Throwable throwable) {
    if (subscriber != null) {
      subscriber.onError(throwable);
    }
  }

  @Override
  public void onComplete() {
    if (subscriber != null) {
      subscriber.onComplete();
    }
  }
}
