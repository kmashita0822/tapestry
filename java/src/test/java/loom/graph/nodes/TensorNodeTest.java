package loom.graph.nodes;

import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.junit.Test;

public class TensorNodeTest extends BaseTestClass {

  @Test
  public void test_scalar_body() {
    var body = TensorNode.Body.builder().dtype("int32").shape(ZPoint.of()).build();
    assertJsonEquals(
        body,
        """
            {
                "dtype": "int32",
                "range": {"start": [], "end": []}
            }
            """);

    assertThat(body.getNDim()).isEqualTo(0);
    assertThat(body.getRange()).isEqualTo(ZRange.fromShape(0));
    assertThat(body.getSize()).isEqualTo(1);
    assertThat(body.getRange()).isEqualTo(new ZRange(ZPoint.of(), ZPoint.of()));
  }

  @Test
  public void test_body() {
    var body =
        TensorNode.Body.builder()
            .dtype("int32")
            .range(new ZRange(ZPoint.of(-1, -1), ZPoint.of(2, 3)))
            .build();

    assertJsonEquals(
        body,
        """
          {
            "dtype": "int32",
            "range": {"start":[-1, -1], "end":[2, 3]}
          }
          """);

    assertThat(body.getNDim()).isEqualTo(2);
    assertThat(body.getSize()).isEqualTo(12);
    assertThat(body.getRange()).isEqualTo(new ZRange(ZPoint.of(-1, -1), ZPoint.of(2, 3)));

    assertThat(body).hasToString("TensorNode.Body(dtype=int32, range=zr[-1:2, -1:3])");
  }
}
