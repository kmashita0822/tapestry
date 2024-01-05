package loom.graph.constraints;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import loom.graph.CommonEnvironments;
import loom.graph.LoomGraph;
import loom.graph.nodes.*;
import loom.polyhedral.IndexProjectionFunction;
import loom.testing.BaseTestClass;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unused")
public class IPFSignatureAgreementConstraintTest extends BaseTestClass {
  @Test
  public void test_valid_short() {
    var env = CommonEnvironments.expressionEnvironment();
    env.assertConstraint(IPFSignatureAgreementConstraint.class);

    var graph = env.newGraph();

    var tensorA =
        TensorNode.withBody(
                b ->
                    b.dtype("int32")
                        .range(ZRange.fromStartWithShape(new ZPoint(-10, 4), new ZPoint(3, 4))))
            .addTo(graph);

    var tensorB = TensorNode.withBody(b -> b.dtype("int32").shape(4, 5)).addTo(graph);

    var ipfSignature =
        IPFSignature.builder()
            .input(
                "x",
                IndexProjectionFunction.builder()
                    .affineMap(
                        ZAffineMap.fromMatrix(new int[][] {{1, 0}, {0, 0}})
                            .translate(tensorA.getRange().getStart()))
                    .shape(ZPoint.of(1, tensorA.getShape().get(1)))
                    .build())
            .input(
                "y",
                IndexProjectionFunction.builder()
                    .affineMap(
                        ZAffineMap.fromMatrix(new int[][] {{0, 0}, {0, 1}})
                            .translate(tensorB.getRange().getStart()))
                    .shape(ZPoint.of(tensorB.getShape().get(0), 1))
                    .build())
            .output(
                "z",
                IndexProjectionFunction.builder()
                    .affineMap(ZAffineMap.fromMatrix(new int[][] {{1, 0}, {0, 1}}))
                    .build())
            .build();

    var op =
        apply(
            graph,
            "matmul",
            ipfSignature,
            inputs -> {
              var x = inputs.get("x").getFirst().getRange().getShape().get(0);
              var y = inputs.get("y").getFirst().getRange().getShape().get(1);
              return ZRange.fromShape(x, y);
            },
            Map.of(
                "x",
                List.of(TensorSelection.from(tensorA)),
                "y",
                List.of(TensorSelection.from(tensorB))),
            Map.of("z", List.of("int32")),
            null);

    assertThat(graph.nodeScan().nodeClass(IPFSignatureNode.class).asStream().count()).isEqualTo(1);
    assertThat(graph.nodeScan().nodeClass(IPFIndexNode.class).asStream().count()).isEqualTo(2);
    assertThat(graph.nodeScan().nodeClass(OperationSignatureNode.class).asStream().count())
        .isEqualTo(1);
    assertThat(graph.nodeScan().nodeClass(ApplicationNode.class).asStream().count()).isEqualTo(1);

    assertThat(
            graph
                .nodeScan()
                .nodeClass(TensorNode.class)
                .asStream()
                .filter(n -> Objects.equals(n.getLabel(), "matmul/z[0]"))
                .count())
        .isEqualTo(1);

    graph.validate();
  }

  @Test
  public void test_valid_long() {
    var env = CommonEnvironments.expressionEnvironment();
    env.assertConstraint(IPFSignatureAgreementConstraint.class);

    var graph = env.newGraph();

    var tensorA = TensorNode.withBody(b -> b.dtype("int32").shape(3, 4)).addTo(graph);

    var tensorB = TensorNode.withBody(b -> b.dtype("int32").shape(4, 5)).addTo(graph);

    var tensorC = TensorNode.withBody(b -> b.dtype("int32").shape(3, 5)).addTo(graph);

    var sigIndex =
        IPFIndexNode.withBody(b -> b.range(new ZRange(ZPoint.of(0, 0), ZPoint.of(3, 5))))
            .addTo(graph);

    var ipfSignature =
        IPFSignatureNode.withBody(
                b ->
                    b.input(
                            "x",
                            List.of(
                                IndexProjectionFunction.builder()
                                    .affineMap(ZAffineMap.fromMatrix(new int[][] {{1, 0}, {0, 0}}))
                                    .shape(ZPoint.of(1, tensorA.getShape().get(1)))
                                    .build()))
                        .input(
                            "y",
                            List.of(
                                IndexProjectionFunction.builder()
                                    .affineMap(ZAffineMap.fromMatrix(new int[][] {{0, 0}, {0, 1}}))
                                    .shape(ZPoint.of(tensorB.getShape().get(0), 1))
                                    .build()))
                        .output(
                            "z",
                            List.of(
                                IndexProjectionFunction.builder()
                                    .affineMap(ZAffineMap.fromMatrix(new int[][] {{1, 0}, {0, 1}}))
                                    .build())))
            .addTo(graph);

    var sig =
        OperationSignatureNode.withBody(
                b ->
                    b.name("matmul")
                        .signatureId(ipfSignature.getId())
                        .indexId(sigIndex.getId())
                        .input("x", List.of(TensorSelection.from(tensorA)))
                        .input("y", List.of(TensorSelection.from(tensorB)))
                        .output("z", List.of(TensorSelection.from(tensorC))))
            .addTo(graph);

    var app1Index =
        IPFIndexNode.withBody(b -> b.range(new ZRange(ZPoint.of(0, 0), ZPoint.of(3, 2))))
            .addTo(graph);

    var app1 =
        ApplicationNode.withBody(
                b ->
                    b.operationId(sig.getId())
                        .indexId(app1Index.getId())
                        .input("x", List.of(TensorSelection.from(tensorA)))
                        .input("y", List.of(TensorSelection.from(tensorB, ZRange.fromShape(4, 2))))
                        .output(
                            "z", List.of(TensorSelection.from(tensorC, ZRange.fromShape(3, 2)))))
            .addTo(graph);

    var appIndex2 =
        IPFIndexNode.withBody(b -> b.range(new ZRange(ZPoint.of(0, 2), ZPoint.of(3, 5))))
            .addTo(graph);

    var app2 =
        ApplicationNode.withBody(
                b ->
                    b.operationId(sig.getId())
                        .indexId(appIndex2.getId())
                        .input("x", List.of(TensorSelection.from(tensorA)))
                        .input(
                            "y",
                            List.of(
                                TensorSelection.from(
                                    tensorB, new ZRange(ZPoint.of(0, 2), ZPoint.of(4, 5)))))
                        .output(
                            "z",
                            List.of(
                                TensorSelection.from(
                                    tensorC, new ZRange(ZPoint.of(0, 2), ZPoint.of(3, 5))))))
            .addTo(graph);

    graph.validate();
  }

  public static OperationSignatureNode apply(
      LoomGraph graph,
      String kernelName,
      IPFSignature ipfSignature,
      Function<Map<String, List<TensorSelection>>, ZRange> indexBuilder,
      Map<String, List<TensorSelection>> inputs,
      Map<String, List<String>> outputTypes,
      @Nullable Map<String, Object> params) {

    /*
     * NOTE: there is some weirdness here.
     *
     * If the input tensors have a range that does not start from (0, ...);
     * then we probably want to adjust the IPFSignature of the generated
     * OperationSignatureNode to reflect that.
     *
     * That's messy; and the alternative is to say that the TensorSelections
     * are always relative to the range of the input tensors; not absolute.
     * Which is a larger change.
     */

    var index = indexBuilder.apply(inputs);

    var ipfSignatureNode = IPFSignatureNode.builder().body(ipfSignature).addTo(graph);

    var ipfIndexNode = IPFIndexNode.withBody(b -> b.range(index)).addTo(graph);

    var opSigNode =
        OperationSignatureNode.withBody(
                b -> {
                  b.name(kernelName);
                  b.signatureId(ipfSignatureNode.getId());
                  b.indexId(ipfIndexNode.getId());

                  for (var entry : inputs.entrySet()) {
                    var name = entry.getKey();
                    var selections = entry.getValue();
                    var projections = ipfSignature.getInputs().get(name);
                    assert projections != null && projections.size() == selections.size();

                    for (int idx = 0; idx < selections.size(); ++idx) {
                      var s = selections.get(idx);
                      var p = projections.get(idx);
                      assert s.getRange().equals(p.apply(index));
                    }

                    b.input(name, selections);
                  }

                  for (var entry : outputTypes.entrySet()) {
                    var name = entry.getKey();
                    var types = entry.getValue();
                    var projections = ipfSignature.getOutputs().get(name);
                    assert projections != null && projections.size() == types.size();

                    List<TensorSelection> selections = new ArrayList<>();
                    for (int idx = 0; idx < projections.size(); ++idx) {
                      var p = projections.get(idx);
                      var t = types.get(idx);

                      var tensor =
                          TensorNode.withBody(tb -> tb.dtype(t).range(p.apply(index)))
                              .label("%s/%s[%d]".formatted(kernelName, name, idx))
                              .addTo(graph);

                      selections.add(TensorSelection.from(tensor));
                    }
                    b.output(name, selections);
                  }
                })
            .addTo(graph);

    createIpfShards(opSigNode, index);

    return opSigNode;
  }

  @CanIgnoreReturnValue
  public static List<ApplicationNode> createIpfShards(
      OperationSignatureNode sig, ZRange... shardIndexes) {
    return createIpfShards(sig, Arrays.asList(shardIndexes));
  }

  public static List<ApplicationNode> createIpfShards(
      OperationSignatureNode sig, Collection<ZRange> shardIndexes) {
    return shardIndexes.stream().map(shardIndex -> createIpfShard(sig, shardIndex)).toList();
  }

  public static ApplicationNode createIpfShard(OperationSignatureNode sig, ZRange shardIndex) {
    var graph = sig.assertGraph();
    var sigIndex = graph.assertNode(sig.getIndexId(), IPFIndexNode.TYPE, IPFIndexNode.class);
    var ipfSig =
        graph.assertNode(sig.getSignatureId(), IPFSignatureNode.TYPE, IPFSignatureNode.class);

    var appIndex = IPFIndexNode.withBody(b -> b.range(shardIndex)).addTo(graph);

    assert sigIndex.getRange().contains(shardIndex);

    return ApplicationNode.withBody(
            b -> {
              b.operationId(sig.getId());
              b.indexId(appIndex.getId());

              for (var entry : ipfSig.getInputs().entrySet()) {
                var name = entry.getKey();
                var projections = entry.getValue();
                var baseSelections = sig.getInputs().get(name);
                assert baseSelections != null && projections.size() == baseSelections.size();

                List<TensorSelection> selections = new ArrayList<>();
                for (int idx = 0; idx < projections.size(); ++idx) {
                  var p = projections.get(idx);
                  var s = baseSelections.get(idx);
                  selections.add(new TensorSelection(s.getTensorId(), p.apply(shardIndex)));
                }
                b.input(name, selections);
              }

              for (var entry : ipfSig.getOutputs().entrySet()) {
                var name = entry.getKey();
                var projections = entry.getValue();
                var baseSelections = sig.getOutputs().get(name);
                assert baseSelections != null && projections.size() == baseSelections.size();

                List<TensorSelection> selections = new ArrayList<>();
                for (int idx = 0; idx < projections.size(); ++idx) {
                  var p = projections.get(idx);
                  var s = baseSelections.get(idx);
                  selections.add(new TensorSelection(s.getTensorId(), p.apply(shardIndex)));
                }
                b.output(name, selections);
              }
            })
        .addTo(graph);
  }
}
