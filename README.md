# aws-java-sdk-v2-java-11-client [![tests](https://github.com/sigpwned/aws-java-sdk-v2-java-11-client/actions/workflows/integration.yml/badge.svg)](https://github.com/sigpwned/aws-java-sdk-v2-java-11-client/actions/workflows/integration.yml)

An implementation of the [AWS Java SDK v2 HTTP Client SPI](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/http-configuration.html) using [the Java 11 HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html).

It was pulled from the [@aws/aws-java-sdk-v2](https://github.com/aws/aws-sdk-java-v2) [java-11-http-client](https://github.com/aws/aws-sdk-java-v2/tree/java-11-http-client) branch.

It is very much a work in progress. Help, feedback, and PRs are all welcome and (greatly) appreciated!

## Background

AWS Java SDK v2 has an [HTTP client service provider interface](https://central.sonatype.com/artifact/software.amazon.awssdk/http-client-spi), which it leverages to provide [a variety of managed HTTP clients](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/http-configuration.html) (e.g., [software.amazon.awssdk:url-connection-client](https://central.sonatype.com/artifact/software.amazon.awssdk/url-connection-client)). It would be usefult to have an additional AWS HTTP Client backed by the Java 11 HttpClient for a variety of reasons:

* Reduced runtime size
* Reduced cold start time
* Simplified dependency management
* Virtual Thread readiness

So far, Amazon itself seems [reluctant to provide such an implementation](https://github.com/aws/aws-sdk-java-v2/issues/1447#issuecomment-1902675971). And that's OK! Amazon has its own values, priorities, and resource constraints.

But they've also given us the tools to build such a client ourselves. This project is that implementation.

## Implementations

More information about the "official" Amazon implementations is available [here](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/http-configuration.html).

Integration tests against live or mock services remain a work-in-progress.

### Asynchronous

The `awssdkv2-java11-async-client` is an asynchronous implementation of the `SdkAsyncHttpClient` asynchronous HTTP client SPI from the AWS Java SDK v2. You can compare it to the two asynchronous clients Amazon currently supports for that library:

* Netty [GitHub](https://github.com/aws/aws-sdk-java-v2/tree/master/http-clients/netty-nio-client) [Developer Guide](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/http-configuration-netty.html) [JavaDocs](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/http/nio/netty/NettyNioAsyncHttpClient.html)
* AWS CRT [GitHub](https://github.com/aws/aws-sdk-java-v2/tree/master/http-clients/aws-crt-client) [Developer Guide](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/http-configuration-crt.html) [JavaDocs](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/http/crt/AwsCrtAsyncHttpClient.html)

This implementation can be used like any other implementation, for example for use in the SQS client:

```java
    SqsAsyncClient client = SqsAsyncClient.builder()
        .httpClient(Java11AsyncHttpClient.create())
        .region(Region.US_EAST_1)
        .build();
```

There are a few caveats about its usage as compared to other clients due to differences in the underlying `HttpClient` implementation:

* There is no support for read and write timeouts, per [JDK-8258397](https://bugs.openjdk.org/browse/JDK-8258397). Rather, the client can be configured to use a request timeout, which limits how long the client waits until HTTP response headers are received, but that's it.
* Proxy authentication is not supported out of the box. Per [JDK-8229962](https://bugs.openjdk.org/browse/JDK-8229962), configuring proxy authentication on `HttpClient` requires providing some process-level configuration parameters up front, so proxy authentication is [up to the user](https://stackoverflow.com/a/60170227/2103602), at least for now.
* There is a difference in behavior for canceled HTTP requests between Java 11 and Java 15, and Java 16 and later. Per [JDK-8245462](https://bugs.openjdk.org/browse/JDK-8245462), before Java 16, calling `CompletableFuture#cancel(boolean)` on the result of `execute()` does nothing, whereas starting with Java 16 it now attempts to cancel the request in-flight.
* The client does not close connections after receiving 5XX responses from the server, which is technically part of the requirements for other HTTP clients.
* There may be some slight differences in how `Expect: 100-continue`  is handled versus other clients.

## Prior Art

There are some other implementations using this same approach that were very useful in creating this implementation:

* [@gabfssilva/aws-spi-java-11](https://github.com/gabfssilva/aws-spi-java-11)
* [@rmcsoft/j11_aws_http_client](https://github.com/rmcsoft/j11_aws_http_client)

They generally appear to be abandoned, which is why I created this project.

## Help Wanted

So you want to help, huh? Outstanding!

The most important work happening right now is determining the list of tests required for the asynchronous client. Please check out [#2](https://github.com/sigpwned/aws-java-sdk-v2-java-nio-client/issues/2) for instructions on how to contribute.

Naturally, all helpers will be added to the [contributors](https://github.com/sigpwned/aws-java-sdk-v2-java-nio-client/blob/main/CONTRIBUTORS.md).
