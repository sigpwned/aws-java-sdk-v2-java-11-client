package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import static java.util.Objects.requireNonNull;

import com.sigpwned.software.amazon.awssdk.http.nio.java.JavaHttpClientNioAsyncHttpClient;
import com.sigpwned.software.amazon.awssdk.http.nio.java.util.MoreHttpHeaders;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;

/**
 * Internal implementation of request execution. The {@link JavaHttpClientNioAsyncHttpClient} will
 * use this class to execute the request. That class is essentially just a user-facing configuration
 * wrapper for this business logic.
 */
@SdkProtectedApi
public class JavaHttpClientRequestExecutor {

  private final HttpClient javaClient;

  private final Duration responseTimeout;

  public JavaHttpClientRequestExecutor(HttpClient javaClient, Duration responseTimeout) {
    this.javaClient = requireNonNull(javaClient);
    this.responseTimeout = responseTimeout;
  }

  public CompletableFuture<HttpResponse<Void>> execute(AsyncExecuteRequest sdkRequest) {
    sdkRequest.request().headers().entrySet().stream()
        .flatMap(e -> e.getValue().stream().map(v -> Map.entry(e.getKey(), v)))
        .forEach(e -> {
          if (!MoreHttpHeaders.isValidHeaderChars(e.getKey())) {
            // We throw unusual exceptions here to pass test
            // SdkAsyncHttpClientH1TestSuite#naughtyHeaderCharactersDoNotGetToServer
            throw new UncheckedIOException(
                new IOException("Invalid HTTP request",
                    new IllegalArgumentException("Request contains invalid header")));
          }
          if (!MoreHttpHeaders.isValidHeaderChars(e.getValue())) {
            // We throw unusual exceptions here to pass test
            // SdkAsyncHttpClientH1TestSuite#naughtyHeaderCharactersDoNotGetToServer
            throw new UncheckedIOException(
                new IOException("Invalid HTTP request",
                    new IllegalArgumentException(
                        "Request header " + e.getKey() + " has invalid value")));
          }
        });

    HttpRequest javaRequest = new JavaHttpClientRequestMapper(
        getResponseTimeout()).toJavaHttpClientRequest(sdkRequest);

    BodyHandler<Void> javaResponseHandler = new JavaHttpClientResponseAdapter(
        sdkRequest.responseHandler());

    return getJavaClient().sendAsync(javaRequest, javaResponseHandler);
  }

  private HttpClient getJavaClient() {
    return javaClient;
  }

  public Duration getResponseTimeout() {
    return responseTimeout;
  }
}
