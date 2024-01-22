package com.sigpwned.software.amazon.awssdk.http.nio.java.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public final class ByteBuffers {

  private ByteBuffers() {
  }

  /**
   * Concatenates a list of ByteBuffers into a single ByteBuffer. The resulting ByteBuffer's cursor
   * will be positioned at the beginning of the buffer. The list of ByteBuffers will not be
   * modified, but each element's cursor may be positioned at the end of the buffer.
   *
   * @param buffers The list of ByteBuffers to concatenate.
   * @return A ByteBuffer containing the contents of all ByteBuffers in the list.
   * @see #concat(List)
   */
  public static ByteBuffer concat(final ByteBuffer... buffers) {
    return concat(Arrays.asList(buffers));
  }

  /**
   * Concatenates a list of ByteBuffers into a single ByteBuffer. The resulting ByteBuffer's cursor
   * will be positioned at the beginning of the buffer. The list of ByteBuffers will not be
   * modified, but each element's cursor may be positioned at the end of the buffer.
   *
   * @param bs The list of ByteBuffers to concatenate.
   * @return A ByteBuffer containing the contents of all ByteBuffers in the list.
   */
  public static ByteBuffer concat(List<ByteBuffer> bs) {
    if (bs.isEmpty()) {
      return ByteBuffer.allocate(0);
    }

    if (bs.size() == 1) {
      return bs.get(0);
    }

    final int totalRemaining = bs.stream().mapToInt(Buffer::remaining).sum();
    final ByteBuffer result = ByteBuffer.allocate(totalRemaining);

    bs.forEach(result::put);

    result.flip();

    return result;
  }
}
