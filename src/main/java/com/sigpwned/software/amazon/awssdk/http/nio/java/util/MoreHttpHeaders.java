package com.sigpwned.software.amazon.awssdk.http.nio.java.util;

/**
 * Inspired by Netty
 */
public final class MoreHttpHeaders {

  private MoreHttpHeaders() {
  }

  /**
   * Validate a header name.
   *
   * @param value the header value to validate
   * @return {@code -1} if the header value is valid, otherwise the index of the first invalid
   * character.
   * @see <a
   * href="https://github.com/netty/netty/blob/323f78ae7c6fcda0c5c62c20afa77a940fb2ee26/codec-http/src/main/java/io/netty/handler/codec/http/HttpHeaderValidationUtil.java#L143">
   * https://github.com/netty/netty/blob/323f78ae7c6fcda0c5c62c20afa77a940fb2ee26/codec-http/src/main/java/io/netty/handler/codec/http/HttpHeaderValidationUtil.java#L143</a>
   */
  public static boolean isValidHeaderChars(CharSequence value) {
    // Validate value to field-content rule.
    //  field-content  = field-vchar [ 1*( SP / HTAB ) field-vchar ]
    //  field-vchar    = VCHAR / obs-text
    //  VCHAR          = %x21-7E ; visible (printing) characters
    //  obs-text       = %x80-FF
    //  SP             = %x20
    //  HTAB           = %x09 ; horizontal tab
    //  See: https://datatracker.ietf.org/doc/html/rfc7230#section-3.2
    //  And: https://datatracker.ietf.org/doc/html/rfc5234#appendix-B.1
    // AWS also expects headers not to include newline or carriage returns.
    return value.chars().allMatch(MoreHttpHeaders::isValidHeaderChar);
  }

  public static boolean isValidHeaderChar(int ch) {
    return (ch >= 0x20 && ch <= 0x7E) || ch == '\t';
  }
}
