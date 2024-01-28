package com.sigpwned.software.amazon.awssdk.http.java11;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

/**
 * A simple implementation of a {@link SdkHttpContentPublisher} that publishes a single
 * {@link ByteBuffer} and completes.
 *
 * @see software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher
 */
public class SimpleSdkHttpContentPublisher implements SdkHttpContentPublisher {

  private final byte[] content;

  private final boolean hasContentLength;

  public SimpleSdkHttpContentPublisher(byte[] content) {
    this(content, true);
  }

  public SimpleSdkHttpContentPublisher(byte[] content, boolean hasContentLength) {
    this.content = requireNonNull(content);
    this.hasContentLength = hasContentLength;
  }

  @Override
  public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
    subscriber.onSubscribe(new SimpleSubscription(subscriber, content));
  }

  @Override
  public Optional<Long> contentLength() {
    return hasContentLength ? Optional.of((long) content.length) : Optional.empty();
  }
}
