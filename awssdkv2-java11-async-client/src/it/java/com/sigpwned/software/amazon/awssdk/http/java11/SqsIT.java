package com.sigpwned.software.amazon.awssdk.http.java11;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;

public class SqsIT {
  @ClassRule
  public static LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
          .withServices(Service.SQS);

  public SqsAsyncClient client;

  @Before
  public void setupSqsIT() {
    client = SqsAsyncClient.builder().endpointOverride(localstack.getEndpoint())
        .httpClientBuilder(Java11AsyncHttpClient.builder())
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
        .region(Region.of(localstack.getRegion())).build();
  }

  @Test
  public void smokeTest() throws Exception {
    ListQueuesResponse response = client.listQueues().get();
    assertThat(response.queueUrls()).isEmpty();
  }
}
