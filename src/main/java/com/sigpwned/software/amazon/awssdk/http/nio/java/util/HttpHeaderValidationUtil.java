package com.sigpwned.software.amazon.awssdk.http.nio.java.util;

/**
 * Borrowed with love from Netty, which is released under Apache Source Code.
 */
public class HttpHeaderValidationUtil {

  /**
   * Validate a header name.
   *
   * @param value the header value to validate
   * @return {@code -1} if the header value is valid, otherwise the index of the first invalid
   * character.
   */
  public static boolean verifyValidHeaderValueCharSequence(CharSequence value) {
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
    return value.chars().allMatch(HttpHeaderValidationUtil::isValidChar);
  }

  public static boolean isValidChar(int ch) {
    return (ch >= 0x20 && ch <= 0x7E) || ch == '\t';
  }
}
