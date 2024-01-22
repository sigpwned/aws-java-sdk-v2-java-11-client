# aws-java-sdk-v2-java-nio-client

An implementation of the [AWS Java SDK v2 HTTP Client SPI](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/http-configuration.html) using [the Java 11 HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html).

It was pulled from the [@aws/aws-java-sdk-v2](https://github.com/aws/aws-sdk-java-v2) [java-11-http-client](https://github.com/aws/aws-sdk-java-v2/tree/java-11-http-client) branch.

It is very much a work in progress. Help is welcome and appreciated!

## Background

AWS Java SDK v2 has an [HTTP client service provider interface](https://central.sonatype.com/artifact/software.amazon.awssdk/http-client-spi), which it leverages to provide [a variety of managed HTTP clients](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/http-configuration.html) (e.g., [software.amazon.awssdk:url-connection-client](https://central.sonatype.com/artifact/software.amazon.awssdk/url-connection-client)). It would be usefult to have an additional AWS HTTP Client backed by the Java 11 HttpClient for a variety of reasons:

* Reduced runtime size
* Reduced cold start time
* Simplified dependency management
* Virtual Thread readiness

So far, Amazon itself seems [reluctant to provide such an implementation](https://github.com/aws/aws-sdk-java-v2/issues/1447#issuecomment-1902675971). And that's OK! Amazon has its own values, priorities, and resource constraints.

But they've also given us the tools to build such a client ourselves. This project is that implementation.

## Help Wanted

So you want to help, huh? Outstanding!

The most important work happening right now is determining the list of tests required for the asynchronous client. Please check out #2 for instructions on how to contribute.

Naturally, all helpers will be added to the contributors.
