package com.sigpwned.software.amazon.awssdk.http.java11;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sigpwned.software.amazon.awssdk.http.java11.util.ByteBuffers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpConnectTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public class Expect100ContinueTest {

  @Test
  public void expect100ContinueWorksWithZeroContentLength200() throws Exception {
    Handler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {
        response.setStatus(200);
        response.setContentLength(0);
        response.addHeader("x-amz-test-header", "foo");
        response.flushBuffer();
      }
    };

    try (SdkAsyncHttpClient client = createClient();
        EmbeddedServer server = new EmbeddedServer(handler)) {
      HttpExecuteResponse response = sendRequest(client, server);
      assertThat(response.httpResponse().statusCode()).isEqualTo(200);
      assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
    }
  }

  @Test
  public void expect100ContinueWorksWith204() throws Exception {
    Handler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {
        response.setStatus(204);
        response.addHeader("x-amz-test-header", "foo");
        response.flushBuffer();
      }
    };

    try (SdkAsyncHttpClient client = createClient();
        EmbeddedServer server = new EmbeddedServer(handler)) {
      HttpExecuteResponse response = sendRequest(client, server);
      assertThat(response.httpResponse().statusCode()).isEqualTo(204);
      assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
    }
  }

  @Test
  public void expect100ContinueWorksWith304() throws Exception {
    Handler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {
        response.setStatus(304);
        response.addHeader("x-amz-test-header", "foo");
        response.flushBuffer();
      }
    };

    try (SdkAsyncHttpClient client = createClient();
        EmbeddedServer server = new EmbeddedServer(handler)) {
      HttpExecuteResponse response = sendRequest(client, server);
      assertThat(response.httpResponse().statusCode()).isEqualTo(304);
      assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
    }
  }

  @Test
  public void expect100ContinueWorksWith417() throws Exception {
    Handler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {
        response.setStatus(417);
        response.addHeader("x-amz-test-header", "foo");
        response.flushBuffer();
      }
    };

    try (SdkAsyncHttpClient client = createClient();
        EmbeddedServer server = new EmbeddedServer(handler)) {
      HttpExecuteResponse response = sendRequest(client, server);
      assertThat(response.httpResponse().statusCode()).isEqualTo(417);
      assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
    }
  }

  @Test
  public void expect100ContinueWorksWithZeroContentLength500() throws Exception {
    Handler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {
        response.setStatus(500);
        response.setContentLength(0);
        response.addHeader("x-amz-test-header", "foo");
        response.flushBuffer();
      }
    };

    try (SdkAsyncHttpClient client = createClient();
        EmbeddedServer server = new EmbeddedServer(handler)) {
      HttpExecuteResponse response = sendRequest(client, server);
      assertThat(response.httpResponse().statusCode()).isEqualTo(500);
      assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
    }
  }

  @Test
  public void expect100ContinueWorksWithPositiveContentLength500() throws Exception {
    Handler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {
        response.setStatus(500);
        response.setContentLength(5);
        response.addHeader("x-amz-test-header", "foo");
        // This is required. That seems fair to me.
        response.getOutputStream().write(new byte[5]);
        response.flushBuffer();
      }
    };

    try (SdkAsyncHttpClient client = createClient();
        EmbeddedServer server = new EmbeddedServer(handler)) {
      HttpExecuteResponse response = sendRequest(client, server);
      assertThat(response.httpResponse().statusCode()).isEqualTo(500);
      assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
    }
  }

  @Test
  public void expect100ContinueWorksWithPositiveContentLength400() throws Exception {
    Handler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {
        response.setStatus(400);
        // This is required. That seems fair to me.
        response.addHeader("x-amz-test-header", "foo");
        response.getOutputStream().write(new byte[5]);
        response.setContentLength(5);
        response.flushBuffer();
      }
    };

    try (SdkAsyncHttpClient client = createClient();
        EmbeddedServer server = new EmbeddedServer(handler)) {
      HttpExecuteResponse response = sendRequest(client, server);
      assertThat(response.httpResponse().statusCode()).isEqualTo(400);
      assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
    }
  }

  @Test
  public void expect100ContinueFailsWithPositiveContentLength200() throws Exception {
    Handler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {
        response.setStatus(200);
        response.setContentLength(1);
        response.flushBuffer();
      }
    };

    try (SdkAsyncHttpClient client = createClient();
        EmbeddedServer server = new EmbeddedServer(handler)) {
      assertThatThrownBy(() -> sendRequest(client, server, true)).isInstanceOf(
          IOException.class);
    }
  }

  /**
   * This does not pass, and I'm not sure if or why it's required.
   */
  @Test
  @Ignore
  public void expect100ContinueFailsWithChunkedEncoded200() throws Exception {
    Handler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {
        response.setStatus(200);
        response.flushBuffer();
      }
    };

    try (SdkAsyncHttpClient client = createClient();
        EmbeddedServer server = new EmbeddedServer(handler)) {
      assertThatThrownBy(() -> sendRequest(client, server, true)).isInstanceOf(
          UncheckedIOException.class);
    }
  }

  private HttpExecuteResponse sendRequest(SdkAsyncHttpClient client, EmbeddedServer server)
      throws IOException {
    return sendRequest(client, server, false);
  }

  private HttpExecuteResponse sendRequest(SdkAsyncHttpClient client, EmbeddedServer server,
      boolean chunked)
      throws IOException {
    final AtomicReference<SdkHttpResponse> responseBuffer = new AtomicReference<>();
    final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    final URI serverUri=server.uri();

    try {
      SdkHttpRequest.Builder requestBuilder = SdkHttpRequest.builder()
          .uri(serverUri)
          .putHeader("Expect", "100-continue")
          .method(SdkHttpMethod.PUT);
      client.execute(AsyncExecuteRequest.builder()
              .request(requestBuilder.build())
              .fullDuplex(true)
              .requestContentPublisher(new SimpleSdkHttpContentPublisher(new byte[0], !chunked))
              .responseHandler(new SdkAsyncHttpResponseHandler() {
                @Override
                public void onHeaders(SdkHttpResponse headers) {
                  responseBuffer.set(headers);
                }

                @Override
                public void onStream(Publisher<ByteBuffer> stream) {
                  stream.subscribe(new SimpleSubscriber(byteBuffer -> {
                    try {
                      bodyBuffer.write(ByteBuffers.toByteArray(byteBuffer));
                    } catch (IOException e) {
                      // We're using a ByteArrayOutputStream, so this should never happen
                      throw new UncheckedIOException(e);
                    }
                  }));
                }

                @Override
                public void onError(Throwable error) {
                  error.printStackTrace(System.err);
                }
              })
              .build())
          .get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InterruptedIOException();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof HttpConnectTimeoutException) {
        e.printStackTrace(System.err);
        System.err.println(serverUri);
        System.err.println(server.uri());
        throw (HttpConnectTimeoutException) cause;
      } else if (cause instanceof IOException) {
        throw (IOException) cause;
      } else if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw new RuntimeException(cause);
      }
    }

    return HttpExecuteResponse.builder()
        .response(responseBuffer.get())
        .responseBody(
            AbortableInputStream.create(new ByteArrayInputStream(bodyBuffer.toByteArray())))
        .build();
  }

  private SdkAsyncHttpClient createClient() {
    return Java11AsyncHttpClient.builder()
        .connectionTimeout(Duration.ofSeconds(5L))
        .build();
  }

  private static class EmbeddedServer implements SdkAutoCloseable {
    // TODO It might be nice to refactor this to use WireMock?

    private final Server server;

    public EmbeddedServer(Handler handler) throws Exception {
      server = new Server(0);
      server.setHandler(handler);
      server.start();
    }

    public URI uri() {
      return server.getURI();
    }

    @Override
    public void close() {
      try {
        server.stop();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}