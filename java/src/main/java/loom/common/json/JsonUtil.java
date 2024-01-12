package loom.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import lombok.NoArgsConstructor;
import lombok.Value;
import loom.common.collections.IteratorUtils;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class JsonUtil {
  @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  public static class Tree {

    /**
     * Check if all elements in an array are numeric.
     *
     * @param array The array to check.
     * @return True if all elements are numeric, false otherwise.
     */
    public static boolean isAllNumeric(ArrayNode array) {
      return allOf(array, JsonNode::isNumber);
    }

    /**
     * Check if all elements in an array match the Predicate.
     *
     * @param array The array to check.
     * @return True if all elements are boolean, false otherwise.
     */
    public static boolean allOf(ArrayNode array, Predicate<JsonNode> predicate) {
      for (var it = array.elements(); it.hasNext(); ) {
        var node = it.next();
        if (!predicate.test(node)) {
          return false;
        }
      }
      return true;
    }

    /**
     * Check if any elements in an array match the Predicate.
     *
     * @param array The array to check.
     * @return True if any elements are numeric, false otherwise.
     */
    public static boolean anyOf(ArrayNode array, Predicate<JsonNode> predicate) {
      for (var it = array.elements(); it.hasNext(); ) {
        var node = it.next();
        if (predicate.test(node)) {
          return true;
        }
      }
      return false;
    }

    /**
     * Adapt an ArrayNode to a {@code Stream<JsonNode>}.
     *
     * @param array The array to adapt.
     * @return The Stream.
     */
    public static Stream<JsonNode> stream(ArrayNode array) {
      return StreamSupport.stream(array.spliterator(), false);
    }

    /**
     * Adapt an ObjectNode to a {@code Stream<Map.Entry<String, JsonNode>>}.
     *
     * @param object The object to adapt.
     * @return The Stream.
     */
    public static Stream<Map.Entry<String, JsonNode>> stream(ObjectNode object) {
      return IteratorUtils.iteratorToStream(object.fields());
    }
  }

  private static final ObjectMapper COMMON_MAPPER =
      new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  // Prevent Construction.

  /**
   * Get a Jackson ObjectMapper with default settings.
   *
   * @return the ObjectMapper.
   */
  static ObjectMapper getMapper() {
    return COMMON_MAPPER;
  }

  /**
   * Serialize an object to JSON via Jackson defaults.
   *
   * @param obj the object to serialize.
   * @return the JSON string.
   * @throws IllegalArgumentException if the object cannot be serialized.
   */
  public static String toJson(Object obj) {
    try {
      return getMapper().writer().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Serialize an object to pretty JSON via Jackson defaults.
   *
   * @param obj the object to serialize.
   * @return the pretty JSON string.
   * @throws IllegalArgumentException if the object cannot be serialized.
   */
  public static String toPrettyJson(Object obj) {
    try {
      return getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String reformatToPrettyJson(String json) {
    return toPrettyJson(parseToJsonNodeTree(json));
  }

  /**
   * Parse a JSON string to a Jackson JsonNode tree.
   *
   * @param json the JSON string.
   * @return the JsonNode tree.
   */
  public static JsonNode parseToJsonNodeTree(String json) {
    try {
      return getMapper().readTree(json);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Convert an object to a Jackson JsonNode tree.
   *
   * @param obj the object to convert.
   * @return the JsonNode tree.
   */
  public static JsonNode valueToJsonNodeTree(Object obj) {
    return getMapper().valueToTree(obj);
  }

  /**
   * De-serialize a JSON string to an object of the specified class.
   *
   * @param json the JSON string.
   * @param clazz the class of the object to de-serialize.
   * @param <T> the type of the object to de-serialize.
   * @return the de-serialized object.
   * @throws IllegalArgumentException if the JSON string cannot be de-serialized to the specified
   *     class.
   */
  public static <T> T fromJson(String json, Class<T> clazz) {
    try {
      return readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * De-serialize a JSON string to an object of the specified class.
   *
   * @param json the JSON string.
   * @param cls the class of the object to de-serialize.
   * @return the de-serialized object.
   * @param <T> the type of the object to de-serialize.
   * @throws JsonProcessingException if the JSON string cannot be de-serialized to the specified.
   */
  public static <T> T readValue(String json, Class<T> cls) throws JsonProcessingException {
    return getMapper().readValue(json, cls);
  }

  /**
   * Convert an object to an object of the specified class.
   *
   * @param tree the object to convert.
   * @param clazz the class of the object to convert to.
   * @return the converted object.
   * @param <T> the type of the object to convert to.
   * @throws IllegalArgumentException if the object cannot be converted to the specified class.
   */
  public static <T> T convertValue(Object tree, Class<T> clazz) {
    return getMapper().convertValue(tree, clazz);
  }

  /**
   * Convert an Object to a simple JSON object.
   *
   * @param obj the object to convert.
   * @return the simple JSON value tree.
   */
  public static Object toSimpleJson(Object obj) {
    return treeToSimpleJson(valueToJsonNodeTree(obj));
  }

  /**
   * Convert a Jackson JsonNode tree a simple JSON value tree.
   *
   * <p>Simple JSON value trees are composed of the following types:
   *
   * <ul>
   *   <li>{@code null}
   *   <li>{@code String}
   *   <li>{@code Number}
   *   <li>{@code Boolean}
   *   <li>{@code List<Simple>}
   *   <li>{@code Map<String, Simple>}
   * </ul>
   *
   * @param node the node to convert.
   * @return the simple JSON value tree.
   */
  public static Object treeToSimpleJson(JsonNode node) {
    if (node.isNull()) {
      return null;
    } else if (node instanceof BooleanNode) {
      return node.booleanValue();
    } else if (node instanceof NumericNode n) {
      return n.numberValue();
    } else if (node.isTextual()) {
      return node.textValue();
    } else if (node instanceof ArrayNode arr) {
      var result = new ArrayList<>();
      arr.elements().forEachRemaining(item -> result.add(treeToSimpleJson(item)));
      return result;
    } else if (node instanceof ObjectNode obj) {
      var result = new TreeMap<>();
      obj.fields()
          .forEachRemaining(
              field -> result.put(field.getKey(), treeToSimpleJson(field.getValue())));
      return result;
    } else {
      throw new IllegalArgumentException("Unexpected node type: " + node.getClass());
    }
  }

  /** Traversal context for the validateSimpleJson method. */
  @Value
  protected static class SelectionPath {
    @Nullable SelectionPath parent;
    @Nullable Object selector;
    @Nullable Object target;

    public SelectionPath(@Nullable Object target) {
      this.parent = null;
      this.selector = null;
      this.target = target;
    }

    public SelectionPath(
        @Nullable SelectionPath parent, @Nullable String selector, @Nullable Object target) {
      this.parent = parent;
      this.selector = selector;
      this.target = target;
    }

    public SelectionPath(
        @Nullable SelectionPath parent, @Nullable Integer selector, @Nullable Object target) {
      this.parent = parent;
      this.selector = selector;
      this.target = target;
    }

    /**
     * Selector path from the root to this context location.
     *
     * @return a string representing the path.
     */
    @Override
    public String toString() {
      if (selector == null) {
        return "";
      }
      var prefix = (parent == null) ? "" : parent.toString();

      if (selector instanceof String s) {
        if (!prefix.isEmpty()) {
          return "%s.%s".formatted(prefix, s);
        } else {
          return s;
        }
      } else {
        var n = (Integer) selector;
        if (!prefix.isEmpty()) {
          return "%s[%d]".formatted(prefix, n);
        } else {
          throw new IllegalArgumentException("Unexpected value: " + selector);
        }
      }
    }

    /**
     * Is there a cycle in the traversal path?
     *
     * @return true if there is a cycle.
     */
    public boolean isCycle() {
      var h = parent;
      while (h != null) {
        if (h.target == target) {
          return true;
        }
        h = h.parent;
      }
      return false;
    }
  }

  /**
   * Validate that a JSON object is a simple JSON value tree.
   *
   * @param tree the object to validate.
   * @throws IllegalArgumentException if the object is not a simple JSON value tree.
   */
  public static void validateSimpleJson(Object tree) {
    var scheduled = new ArrayDeque<SelectionPath>();
    scheduled.add(new SelectionPath(tree));

    while (!scheduled.isEmpty()) {
      var item = scheduled.pop();

      if (item.isCycle()) {
        throw new IllegalArgumentException("Cycle detected at " + item);
      }

      final var target = item.getTarget();

      if (target == null
          || target instanceof String
          || target instanceof Number
          || target instanceof Boolean) {
        // Valid scalar values.
        continue;
      }

      if (target instanceof Map<?, ?> map) {
        map.forEach(
            (k, v) -> {
              if (k instanceof String s) {
                // Valid if all children are valid.
                scheduled.add(new SelectionPath(item, s, v));
              } else {
                throw new IllegalArgumentException("Unexpected key: " + k + " at " + item);
              }
            });
        continue;
      }

      if (target instanceof List<?> list) {
        for (int i = 0; i < list.size(); i++) {
          // Valid if all children are valid.
          scheduled.add(new SelectionPath(item, i, list.get(i)));
        }
        continue;
      }

      throw new IllegalArgumentException(
          "Unexpected value type (%s) at %s".formatted(target.getClass().getSimpleName(), item));
    }
  }
}
