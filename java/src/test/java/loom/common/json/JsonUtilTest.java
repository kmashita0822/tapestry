package loom.common.json;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Value;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class JsonUtilTest implements CommonAssertions {

  @Value
  public static class ExampleClass {
    public String a;
    public int b;
  }

  @Test
  public void test_toJson_Bad() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> JsonUtil.toJson(new Object()));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> JsonUtil.toPrettyJson(new Object()));
  }

  @Test
  public void test_reformatToPrettyJson() {
    assertThat(JsonUtil.reformatToPrettyJson("{\"a\":\"a\",\"b\":1}"))
        .isEqualTo(
            """
            {
              "a" : "a",
              "b" : 1
            }""");
  }

  @Test
  @SuppressWarnings("unused")
  public void toJsonString() {
    var obj =
        new Object() {
          public final String a = "a";
          public final int b = 1;
        };
    assertThat(JsonUtil.toJson(obj)).isEqualTo("{\"a\":\"a\",\"b\":1}");
    assertThat(JsonUtil.toPrettyJson(obj))
        .isEqualTo(
            """
            {
              "a" : "a",
              "b" : 1
            }""");
  }

  @Test
  public void testToSimpleJson() {
    var example = new ExampleClass("hello", 3);

    assertThat(JsonUtil.toSimpleJson(List.of(example)))
        .isEqualTo(List.of(Map.of("a", "hello", "b", 3)));
  }

  @Test
  public void test_treeToSimpleJson() {
    assertThat(JsonUtil.treeToSimpleJson(JsonNodeFactory.instance.nullNode())).isNull();
    assertThat(
            JsonUtil.treeToSimpleJson(
                JsonUtil.parseToJsonNodeTree(
                    """
                    {
                      "a" : "hello",
                      "b" : 3
                    }""")))
        .isEqualTo(Map.of("a", "hello", "b", 3));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> JsonUtil.treeToSimpleJson(JsonNodeFactory.instance.missingNode()));
  }

  @Test
  public void testValidateSimpleJson() {
    JsonUtil.validateSimpleJson(2);

    JsonUtil.validateSimpleJson(2.0);
    JsonUtil.validateSimpleJson(Float.POSITIVE_INFINITY);
    JsonUtil.validateSimpleJson(Float.NEGATIVE_INFINITY);
    JsonUtil.validateSimpleJson(Float.NaN);

    JsonUtil.validateSimpleJson(true);
    JsonUtil.validateSimpleJson(false);
    JsonUtil.validateSimpleJson(null);

    JsonUtil.validateSimpleJson("hello");

    JsonUtil.validateSimpleJson(List.of(1, 2.0, "hello", true, false, Float.POSITIVE_INFINITY));

    JsonUtil.validateSimpleJson(
        Map.of(
            "a",
            1,
            "b",
            2.0,
            "c",
            "hello",
            "d",
            true,
            "e",
            false,
            "f",
            Float.POSITIVE_INFINITY,
            "g",
            List.of(1, 2.0, "hello", true, false, Float.POSITIVE_INFINITY),
            "h",
            Map.of("a", 1, "b", 2.0)));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> JsonUtil.validateSimpleJson(new Object()));

    var cycle = new ArrayList<>();
    //noinspection CollectionAddedToSelf
    cycle.add(cycle);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> JsonUtil.validateSimpleJson(Map.of("foo", cycle)));
    cycle.clear();
  }
}
