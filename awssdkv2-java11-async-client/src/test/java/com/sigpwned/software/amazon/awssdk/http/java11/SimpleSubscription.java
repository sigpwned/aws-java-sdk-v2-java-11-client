package com.sigpwned.software.amazon.awssdk.http.java11;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

/**
 * A simple implementation of a {@link Subscription} that publishes a single {@link ByteBuffer} and
 * completes. This is used to publish the content of a {@link SdkHttpContentPublisher}. Ripped from
 * {@link software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher}.
 */
public class SimpleSubscription implements Subscription {

  private final Subscriber<? super ByteBuffer> subscriber;
  private final byte[] content;
  private boolean running;

  SimpleSubscription(Subscriber<? super ByteBuffer> subscriber, byte[] content) {
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
