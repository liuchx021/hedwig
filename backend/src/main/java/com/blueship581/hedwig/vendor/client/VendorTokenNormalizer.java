package com.blueship581.hedwig.vendor.client;

final class VendorTokenNormalizer {

  private VendorTokenNormalizer() {}

  static String normalize(String rawToken) {
    if (rawToken == null) {
      return null;
    }

    String token = rawToken.trim();
    if (token.isEmpty()) {
      return token;
    }

    token = stripMatchingPair(token, '"');
    token = stripMatchingPair(token, '\'');
    token = stripPrefixIgnoreCase(token, "authorization:");
    token = stripPrefixIgnoreCase(token.trim(), "bearer");
    token = token.trim();

    // Tokens should not contain whitespace; removing it makes pasted multiline values usable.
    return token.replaceAll("\\s+", "");
  }

  private static String stripMatchingPair(String value, char quote) {
    if (value.length() >= 2
        && value.charAt(0) == quote
        && value.charAt(value.length() - 1) == quote) {
      return value.substring(1, value.length() - 1).trim();
    }
    return value;
  }

  private static String stripPrefixIgnoreCase(String value, String prefix) {
    if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
      return value.substring(prefix.length()).trim();
    }
    return value;
  }
}
