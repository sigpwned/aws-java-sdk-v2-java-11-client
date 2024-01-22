package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.junit.Test;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

public class JavaHttpRequestFactoryTest {

  @Test
  public void returnFullDuplexPublisherWithoutContentLengthTest() {
    SdkHttpContentPublisher publisher = new SdkHttpContentPublisher() {
      @Override
      public Optional<Long> contentLength() {
        return Optional.empty();
      }

      @Override
      public void subscribe(Subscriber<? super ByteBuffer> subscriber) {

      }
    };
    AsyncExecuteRequest asyncExecuteRequest = AsyncExecuteRequest.builder()
        .requestContentPublisher(publisher)
        .fullDuplex(true)
        .build();

    HttpRequest.BodyPublisher bodyPublisher = JavaHttpRequestFactory.createBodyPublisher(
        asyncExecuteRequest);

    // Check whether the BodyPublisher is the same as the one generated with zero content length
    assertThat(bodyPublisher.equals(
        HttpRequest.BodyPublishers.fromPublisher(FlowAdapters.toFlowPublisher(publisher))));

  }

  @Test
  public void returnFullDuplexPublisherWithContentLengthTest() {
    SdkHttpContentPublisher publisher = new SdkHttpContentPublisher() {
      @Override
      public Optional<Long> contentLength() {
        return Optional.of(100L);
      }

      @Override
      public void subscribe(Subscriber<? super ByteBuffer> subscriber) {

      }
    };
    AsyncExecuteRequest asyncExecuteRequest = AsyncExecuteRequest.builder()
        .requestContentPublisher(publisher)
        .fullDuplex(true)
        .build();

    HttpRequest.BodyPublisher bodyPublisher = JavaHttpRequestFactory.createBodyPublisher(
        asyncExecuteRequest);

    // Check whether the BodyPublisher is the same as the one generated with non-zero content length
    assertThat(bodyPublisher.equals(
        HttpRequest.BodyPublishers.fromPublisher(FlowAdapters.toFlowPublisher(publisher))));
  }

  @Test
  public void returnNonFullDuplexPublisherWithoutContentLengthTest() {
    SdkHttpContentPublisher publisher = new SdkHttpContentPublisher() {
      @Override
      public Optional<Long> contentLength() {
        return Optional.empty();
      }

      @Override
      public void subscribe(Subscriber<? super ByteBuffer> subscriber) {

      }
    };
    AsyncExecuteRequest asyncExecuteRequest = AsyncExecuteRequest.builder()
        .requestContentPublisher(publisher)
        .fullDuplex(false)
        .build();

    HttpRequest.BodyPublisher bodyPublisher = JavaHttpRequestFactory.createBodyPublisher(
        asyncExecuteRequest);

    // Check whether the BodyPublisher is the same as the one generated with zero content length
    assertThat(bodyPublisher.equals(HttpRequest.BodyPublishers.noBody()));

  }

  @Test
  public void returnNonFullDuplexPublisherWithContentLengthTest() {
    SdkHttpContentPublisher publisher = new SdkHttpContentPublisher() {
      @Override
      public Optional<Long> contentLength() {
        return Optional.of(100L);
      }

      @Override
      public void subscribe(Subscriber<? super ByteBuffer> subscriber) {

      }
    };
    AsyncExecuteRequest asyncExecuteRequest = AsyncExecuteRequest.builder()
        .requestContentPublisher(publisher)
        .fullDuplex(false)
        .build();

    HttpRequest.BodyPublisher bodyPublisher = JavaHttpRequestFactory.createBodyPublisher(
        asyncExecuteRequest);

    // Check whether the BodyPublisher is the same as the one generated with non-zero content length
    assertThat(bodyPublisher.equals(
        HttpRequest.BodyPublishers.fromPublisher(FlowAdapters.toFlowPublisher(publisher))));
  }

}