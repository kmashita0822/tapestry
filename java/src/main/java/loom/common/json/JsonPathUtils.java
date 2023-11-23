package loom.common.json;

import com.google.common.base.Splitter;

public class JsonPathUtils {
  /** No instantiation. */
  private JsonPathUtils() {}

  /**
   * Convert a JSON pointer to a JSON path.
   *
   * @param jsonPointer the JSON pointer.
   * @return the JSON path.
   */
  public static String jsonPointerToJsonPath(String jsonPointer) {
    if (jsonPointer == null || jsonPointer.isEmpty()) {
      return "$";
    }

    // JSON Pointer starts with a '/', but JSON Path starts with a '$'
    StringBuilder jsonPath = new StringBuilder("$");

    // Split the JSON Pointer into parts
    for (String part : Splitter.on('/').split(jsonPointer)) {
      if (!part.isEmpty()) {
        try {
          var idx = Integer.parseInt(part);
          jsonPath.append("[").append(idx).append("]");
        } catch (NumberFormatException e) {
          jsonPath.append(".").append(part);
        }
      }
    }

    return jsonPath.toString();
  }

  /**
   * Concatenate JSON paths.
   *
   * @param parts the parts to concatenate.
   * @return the concatenated JSON path.
   */
  public static String concatJsonPath(String... parts) {
    var sb = new StringBuilder();
    sb.append("$");
    for (var part : parts) {
      if (part == null || part.isEmpty()) {
        continue;
      }
      part = part.replaceFirst("^\\$", "");
      sb.append(part);
    }
    return sb.toString();
  }
}
