package com.sigpwned.software.amazon.awssdk.http.nio.java.util;

import static junit.framework.TestCase.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ByteBuffersTest {

  @Test
  public void concatTest() {
    List<ByteBuffer> list = new ArrayList<>();
    String tempString1 = "Hello";
    String tempString2 = " World!";
    ByteBuffer tempBuffer1 = ByteBuffer.wrap(tempString1.getBytes(StandardCharsets.UTF_8));
    ByteBuffer tempBuffer2 = ByteBuffer.wrap(tempString2.getBytes(StandardCharsets.UTF_8));
    list.add(tempBuffer1);
    list.add(tempBuffer2);
    ByteBuffer buffer = ByteBuffers.concat(list);
    assertEquals(tempString1 + tempString2, new String(buffer.array()));
  }

}
