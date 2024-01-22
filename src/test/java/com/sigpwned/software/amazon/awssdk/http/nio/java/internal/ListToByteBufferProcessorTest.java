package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.processors.ReplayProcessor;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import org.junit.Test;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Subscription;

public class ListToByteBufferProcessorTest {

  private PublishProcessor<List<ByteBuffer>> publishProcessor = PublishProcessor.create();
  private ErrorPublisher errorPublisher = new ErrorPublisher();
  private CompletePublisher completePublisher = new CompletePublisher();

  private final int byteBufferSize1 = 20;
  private final int byteBufferSize2 = 20;

  private FlowableSubscriber<ByteBuffer> simSdkSubscriber = new FlowableSubscriber<ByteBuffer>() {
    private Subscription subscription;

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
      subscription.request(1);
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
      assertEquals(byteBuffer.capacity(), byteBufferSize1 + byteBufferSize2);
      subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
      throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
    }
  };

  @Test
  public void publisherTest() {
    List<ByteBuffer> list1 = new ArrayList<>();
    List<ByteBuffer> list2 = new ArrayList<>();
    String tempString1 = "Hello";
    String tempString2 = " World";
    String tempString3 = "!";
    ByteBuffer tempBuffer1 = ByteBuffer.wrap(tempString1.getBytes(StandardCharsets.UTF_8));
    ByteBuffer tempBuffer2 = ByteBuffer.wrap(tempString2.getBytes(StandardCharsets.UTF_8));
    ByteBuffer tempBuffer3 = ByteBuffer.wrap(tempString3.getBytes(StandardCharsets.UTF_8));
    list1.add(tempBuffer1);
    list1.add(tempBuffer2);
    list2.add(tempBuffer3);

    Flowable<List<ByteBuffer>> publisher = Flowable.just(list1, list2);
    JavaHttpClientBodyProcessor processor = new JavaHttpClientBodyProcessor();
    ReplayProcessor<ByteBuffer> subscriber = ReplayProcessor.create();

    processor.subscribe(FlowAdapters.toFlowSubscriber(subscriber));
    publisher.subscribe(FlowAdapters.toSubscriber(processor));

    List<ByteBuffer> received = Flowable.fromPublisher(subscriber).toList().blockingGet();

    assertThat(new String(received.get(0).array(), StandardCharsets.UTF_8)).isEqualTo(
        "Hello World");
    assertThat(new String(received.get(1).array(), StandardCharsets.UTF_8)).isEqualTo("!");
  }

  @Test
  public void onErrorTest() {
    JavaHttpClientBodyProcessor bodyProcessor = new JavaHttpClientBodyProcessor();
    errorPublisher.subscribe(bodyProcessor);
    bodyProcessor.subscribe(FlowAdapters.toFlowSubscriber(simSdkSubscriber));
    bodyProcessor.onComplete();
  }

  @Test
  public void onCompleteTest() {
    JavaHttpClientBodyProcessor bodyProcessor = new JavaHttpClientBodyProcessor();
    bodyProcessor.subscribe(FlowAdapters.toFlowSubscriber(simSdkSubscriber));
    completePublisher.subscribe(bodyProcessor);
    bodyProcessor.onComplete();
  }

  @Test
  public void ProcessorTest() {
    List<ByteBuffer> testList1 = new ArrayList<>();
    ByteBuffer buf1 = ByteBuffer.allocate(byteBufferSize1);
    ByteBuffer buf2 = ByteBuffer.allocate(byteBufferSize2);

    testList1.add(buf1);
    testList1.add(buf2);

    JavaHttpClientBodyProcessor bodyProcessor = new JavaHttpClientBodyProcessor();

    publishProcessor.subscribe(FlowAdapters.toSubscriber(bodyProcessor));
    bodyProcessor.subscribe(FlowAdapters.toFlowSubscriber(simSdkSubscriber));
    publishProcessor.onNext(testList1);
    publishProcessor.onComplete();
  }

  public static class ErrorPublisher implements Flow.Publisher<List<ByteBuffer>> {

    @Override
    public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
      subscriber.onSubscribe(new Flow.Subscription() {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
      });

      subscriber.onError(
          new RuntimeException("onError: invoked successfully, something went wrong!"));
    }

  }

  public static class CompletePublisher implements Flow.Publisher<List<ByteBuffer>> {

    @Override
    public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
      subscriber.onSubscribe(new Flow.Subscription() {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
      });

      subscriber.onComplete();
    }
  }
}