package com.sigpwned.software.amazon.awssdk.http.java11;

import java.util.concurrent.Executor;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Provider for the {@link Executor executor} to be used by the SDK when performing HTTP requests by
 * clients that use executors.
 */

@SdkPublicApi
@FunctionalInterface
public interface ExecutorProvider {

  public Executor executor();
}
