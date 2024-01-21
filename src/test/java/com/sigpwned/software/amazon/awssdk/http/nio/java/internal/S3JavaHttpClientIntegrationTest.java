package com.sigpwned.software.amazon.awssdk.http.nio.java.internal;

import static junit.framework.TestCase.assertEquals;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import com.sigpwned.software.amazon.awssdk.http.nio.java.JavaHttpClientNioAsyncHttpClient;
import java.util.concurrent.CompletableFuture;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Ignore("Copied from source, but integration tests not enabled yet.")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class S3JavaHttpClientIntegrationTest {

  private static final String BUCKET = "zhouquas3testbucketjava";
  private static final String KEY = "smalltestkey";
  private static final String LARGEKEY = "largetestkey";
  private static final String VALUE = "Hello World!";
  private static String LARGESTRING = randomAlphabetic(25000);

  private S3AsyncClient s3Async;
  private S3Client s3Client;

  private void createS3AsyncClient(SdkAsyncHttpClient myClient) {
    this.s3Async = S3AsyncClient.builder()
        .httpClient(myClient)
        .overrideConfiguration(b -> b.retryPolicy(RetryPolicy.none()))
        .serviceConfiguration(S3Configuration.builder().checksumValidationEnabled(false).build())
        .region(Region.US_WEST_2)
        .build();
  }

  private void createS3Client() {
    this.s3Client = S3Client.create();
  }

  private void createAsyncClientWithJavaHttpClientHTTP1() {
    SdkAsyncHttpClient javaHttpCient = JavaHttpClientNioAsyncHttpClient.builder().protocol(Protocol.HTTP1_1).build();
    createS3AsyncClient(javaHttpCient);
  }

  private void createAsyncClientWithJavaHttpClientHTTP2() {
    SdkAsyncHttpClient javaHttpCient = JavaHttpClientNioAsyncHttpClient.builder().protocol(Protocol.HTTP2).build();
    createS3AsyncClient(javaHttpCient);
  }

  /* To run the tests in order, here alphabets are added at the beginning of the names of tests. */
  @Test
  public void As3createBucketWithJavaHttpClientHTTP1Test() {
    createAsyncClientWithJavaHttpClientHTTP1();
    createBucketTest(s3Async);
  }

  @Test
  public void Bs3createBucketWithJavaHttpClientHTTP2Test() {
    createAsyncClientWithJavaHttpClientHTTP2();
    createBucketTest(s3Async);
  }

  @Test
  public void Cs3PutSmallObjectWithJavaHttpClientHTTP1Test() {
    createAsyncClientWithJavaHttpClientHTTP1();
    putObjectTest(s3Async, KEY, VALUE);
  }

  @Test
  public void Ds3PutSmallObjectWithJavaHttpClientHTTP2Test() {
    createAsyncClientWithJavaHttpClientHTTP2();
    putObjectTest(s3Async, KEY, VALUE);
  }

  @Test
  public void Es3PutLargeObjectWithJavaHttpClientHTTP1Test() {
    createAsyncClientWithJavaHttpClientHTTP1();
    putObjectTest(s3Async, LARGEKEY, LARGESTRING);
  }

  @Test
  public void Fs3PutLargeObjectWithJavaHttpClientHTTP2Test() {
    createAsyncClientWithJavaHttpClientHTTP2();
    putObjectTest(s3Async, LARGEKEY, LARGESTRING);
  }

  @Test
  public void Gs3GetObjectWithJavaHttpClientHTTP1Test() {
    createAsyncClientWithJavaHttpClientHTTP1();
    getObjectTest(s3Async);
  }

  @Test
  public void Hs3GetObjectWithJavaHttpClientHTTP2Test() {
    createAsyncClientWithJavaHttpClientHTTP2();
    getObjectTest(s3Async);
  }

  @Test
  public void Is3GetObjectWithSyncHttpClient() {
    createS3Client();
    ResponseBytes responseBytes = s3Client.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).build(),
        ResponseTransformer.toBytes());
    assertEquals(responseBytes.asUtf8String(), VALUE);
  }

  @Test
  public void Js3DeleteObjectWithJavaHttpClientHTTP1Test() {
    createAsyncClientWithJavaHttpClientHTTP1();
    deleteObjectTest(s3Async);
  }


  private static void createBucketTest(S3AsyncClient s3Async) {
    try (s3Async) {
      s3Async.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build()).join();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void putObjectTest(S3AsyncClient s3Async, String key, String value) {
    try (s3Async) {
      CompletableFuture<PutObjectResponse> future = s3Async.putObject(
          PutObjectRequest.builder()
              .bucket(BUCKET)
              .key(key)
              .build(),
          AsyncRequestBody.fromString(value)
      );
      future.join();
    }
  }

  private static void getObjectTest(S3AsyncClient s3Async) {
    ResponseBytes responseBytes = s3Async.getObject(
        GetObjectRequest.builder()
            .bucket(BUCKET)
            .key(KEY)
            .build(),
        AsyncResponseTransformer.toBytes()).join();
    assertEquals(responseBytes.asUtf8String(), VALUE);
  }

  private static void deleteObjectTest(S3AsyncClient s3Async) {
    try (s3Async) {
      CompletableFuture<DeleteObjectResponse> future = s3Async.deleteObject(
          DeleteObjectRequest.builder()
              .bucket(BUCKET)
              .key(KEY)
              .build());
      future.join();
    }
  }
}
