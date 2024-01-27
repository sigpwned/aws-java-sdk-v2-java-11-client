package com.sigpwned.software.amazon.awssdk.http.java11.util;

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

  /**
   * <p>
   * Converts a ByteBuffer to a new byte array. The resulting byte array will contain the contents
   * of the ByteBuffer from the current position to the limit. The ByteBuffer's cursor will not be
   * modified.
   * </p>
   *
   * <p>
   * This is not incredibly efficient, since it requires copying the contents of the ByteBuffer, so
   * use with care. It is, however, very useful for testing!
   * </p>
   *
   * @param buffer The ByteBuffer to convert.
   * @return A byte array containing the contents of the ByteBuffer.
   */
  public static byte[] toByteArray(ByteBuffer buffer) {
    byte[] bytes = new byte[buffer.remaining()];
    buffer.duplicate().get(bytes);
    return bytes;
  }
}
