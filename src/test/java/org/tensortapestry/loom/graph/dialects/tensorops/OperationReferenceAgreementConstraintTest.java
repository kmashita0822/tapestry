package org.tensortapestry.loom.graph.dialects.tensorops;

import static org.tensortapestry.loom.graph.LoomConstants.Errors.NODE_REFERENCE_ERROR;
import static org.tensortapestry.loom.graph.LoomConstants.Errors.NODE_VALIDATION_ERROR;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.LoomConstants;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.tensortapestry.loom.validation.ListValidationIssueCollector;
import org.tensortapestry.loom.validation.ValidationIssue;
import org.tensortapestry.loom.zspace.ZPoint;
import org.tensortapestry.loom.zspace.ZRange;

public class OperationReferenceAgreementConstraintTest extends BaseTestClass {

  public LoomGraph createGraph() {
    var env = CommonEnvironments.expressionEnvironment();
    env.assertConstraint(OperationReferenceAgreementConstraint.class);
    return env.newGraph();
  }

  @Test
  public void test_Empty() {
    var graph = createGraph();
    graph.validate();
  }

  @Test
  public void test_valid() {
    var graph = createGraph();

    var tensorA = TensorNode
      .builder(graph)
      .configure(b -> b.dtype("int32").shape(new ZPoint(2, 3)))
      .label("A")
      .build();

    var sourceOp = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("source");
        b.output(
          "output",
          List.of(
            new TensorSelection(
              tensorA.getId(),
              tensorA.viewBodyAs(TensorNode.Body.class).getRange()
            )
          )
        );
      })
      .build();

    ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sourceOp.getId());
        b.output(
          "output",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorA.getId())
              .range(new ZRange(ZPoint.of(0, 0), ZPoint.of(1, 3)))
              .build()
          )
        );
      })
      .build();

    ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sourceOp.getId());
        b.output(
          "output",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorA.getId())
              .range(new ZRange(ZPoint.of(1, 0), ZPoint.of(2, 3)))
              .build()
          )
        );
      })
      .build();

    var sinkOp = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("sink");
        b.input(
          "input",
          List.of(
            new TensorSelection(
              tensorA.getId(),
              tensorA.viewBodyAs(TensorNode.Body.class).getRange()
            )
          )
        );
      })
      .build();

    ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sinkOp.getId());
        b.input(
          "input",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorA.getId())
              .range(new ZRange(ZPoint.of(0, 0), ZPoint.of(2, 1)))
              .build()
          )
        );
      })
      .build();

    ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sinkOp.getId());
        b.input(
          "input",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorA.getId())
              .range(new ZRange(ZPoint.of(0, 1), ZPoint.of(2, 3)))
              .build()
          )
        );
      })
      .build();

    graph.validate();
  }

  @Test
  public void test_no_shards() {
    var graph = createGraph();

    var tensorA = TensorNode
      .builder(graph)
      .configure(b -> {
        b.dtype("int32");
        b.shape(new ZPoint(2, 3));
      })
      .label("A")
      .build();

    var op = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("source");
        b.output(
          "output",
          List.of(
            new TensorSelection(
              tensorA.getId(),
              tensorA.viewBodyAs(TensorNode.Body.class).getRange()
            )
          )
        );
      })
      .build();

    var constraint = graph
      .assertEnv()
      .assertConstraint(OperationReferenceAgreementConstraint.class);
    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.assertEnv(), graph, issueCollector);
    assertValidationIssues(
      issueCollector.getIssues(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .summary("Operation Signature has no Application shards")
        .context(op.asValidationContext("Operation Node"))
        .build()
    );
  }

  @Test
  public void test_application_operation_disagreement() {
    var graph = createGraph();
    var tensorA = TensorNode
      .builder(graph)
      .configure(b -> {
        b.dtype("int32");
        b.shape(new ZPoint(100));
      })
      .label("A")
      .build();

    var sourceOp = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("source");
        b.input(
          "foo",
          List.of(
            new TensorSelection(
              tensorA.getId(),
              tensorA.viewBodyAs(TensorNode.Body.class).getRange()
            )
          )
        );
      })
      .build();

    var app1 = ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sourceOp.getId());
        b.input("foo", List.of());
      })
      .label("Wrong List Size")
      .build();

    var app2 = ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sourceOp.getId());
        b.input("bar", List.of());
      })
      .label("Misaligned Input Keys")
      .build();
    var app3 = ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sourceOp.getId());
        b.input(
          "foo",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorA.getId())
              .range(new ZRange(ZPoint.of(0), ZPoint.of(100)))
              .build()
          )
        );
        b.output("bar", List.of());
      })
      .label("Misaligned Output Keys")
      .build();

    var constraint = graph
      .assertEnv()
      .assertConstraint(OperationReferenceAgreementConstraint.class);
    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.assertEnv(), graph, issueCollector);
    assertValidationIssues(
      issueCollector.getIssues(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .summary(
          "Application inputs key \"foo\" selection size (0) != Signature selection size (1)"
        )
        .context(app1.asValidationContext("Application Node"))
        .context(sourceOp.asValidationContext("Operation Node"))
        .build(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .summary("Application Node inputs keys [bar] != Operation Signature inputs keys [foo]")
        .context(app2.asValidationContext("Application Node"))
        .context(sourceOp.asValidationContext("Operation Node"))
        .build(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .summary("Application Node outputs keys [bar] != Operation Signature outputs keys []")
        .context(app3.asValidationContext("Application Node"))
        .context(sourceOp.asValidationContext("Operation Node"))
        .build()
    );
  }

  @SuppressWarnings("unused")
  @Test
  public void test_application_shard_range_disagreement() {
    var graph = createGraph();

    var tensorA = TensorNode
      .builder(graph)
      .configure(b -> {
        b.dtype("int32");
        b.shape(new ZPoint(2, 3));
      })
      .label("A")
      .build();

    var sourceOp = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("source");
        b.output(
          "output",
          List.of(
            new TensorSelection(
              tensorA.getId(),
              tensorA.viewBodyAs(TensorNode.Body.class).getRange()
            )
          )
        );
      })
      .build();

    var app1 = ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sourceOp.getId());
        b.output(
          "output",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorA.getId())
              .range(new ZRange(ZPoint.of(0, 0), ZPoint.of(1, 2)))
              .build()
          )
        );
      })
      .build();

    var app2 = ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sourceOp.getId());
        b.output(
          "output",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorA.getId())
              .range(new ZRange(ZPoint.of(1, 0), ZPoint.of(2, 2)))
              .build()
          )
        );
      })
      .build();

    var sinkOp = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("sink");
        b.input(
          "input",
          List.of(
            new TensorSelection(
              tensorA.getId(),
              tensorA.viewBodyAs(TensorNode.Body.class).getRange()
            )
          )
        );
      })
      .build();

    var app3 = ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sinkOp.getId());
        b.input(
          "input",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorA.getId())
              .range(new ZRange(ZPoint.of(0, 0), ZPoint.of(2, 1)))
              .build()
          )
        );
      })
      .build();

    var app4 = ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sinkOp.getId());
        b.input(
          "input",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorA.getId())
              .range(new ZRange(ZPoint.of(0, 1), ZPoint.of(2, 2)))
              .build()
          )
        );
      })
      .build();

    var constraint = graph
      .assertEnv()
      .assertConstraint(OperationReferenceAgreementConstraint.class);

    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.assertEnv(), graph, issueCollector);
    assertValidationIssues(
      issueCollector.getIssues(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .summary(
          "Operation Signature inputs key \"input[0]\" range zr[0:2, 0:3] != shard bounding range zr[0:2, 0:2]"
        )
        .context(context ->
          context
            .name("Application Shard Ranges")
            .data(
              Map.of(
                app3.getId(),
                new ZRange(ZPoint.of(0, 0), ZPoint.of(2, 1)),
                app4.getId(),
                new ZRange(ZPoint.of(0, 1), ZPoint.of(2, 2))
              )
            )
        )
        .context(sinkOp.asValidationContext("Operation Node"))
        .build(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .summary(
          "Operation Signature outputs key \"output[0]\" range zr[0:2, 0:3] != shard bounding range zr[0:2, 0:2]"
        )
        .context(context ->
          context
            .name("Application Shard Ranges")
            .data(
              Map.of(
                app1.getId(),
                new ZRange(ZPoint.of(0, 0), ZPoint.of(1, 2)),
                app2.getId(),
                new ZRange(ZPoint.of(1, 0), ZPoint.of(2, 2))
              )
            )
        )
        .context(sourceOp.asValidationContext("Operation Node"))
        .build(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .summary("Overlapping Application output key \"output[0]\" ranges")
        .context(context ->
          context
            .name("Application Shard Ranges")
            .data(
              Map.of(
                app1.getId(),
                new ZRange(ZPoint.of(0, 0), ZPoint.of(1, 2)),
                app2.getId(),
                new ZRange(ZPoint.of(1, 0), ZPoint.of(2, 2))
              )
            )
        )
        .context(sourceOp.asValidationContext("Operation Node"))
        .build()
    );
  }

  @SuppressWarnings({ "unused", "SequencedCollectionMethodCanBeUsed" })
  @Test
  public void test_application_broken_reference() {
    var graph = createGraph();

    var tensorA = TensorNode
      .builder(graph)
      .configure(b -> {
        b.dtype("int32");
        b.shape(new ZPoint(2, 3));
      })
      .label("A")
      .build();
    var tensorB = TensorNode
      .builder(graph)
      .configure(b -> {
        b.dtype("int32");
        b.shape(new ZPoint(2, 3));
      })
      .label("A")
      .build();

    var opSig = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("source");
        b.output(
          "output",
          List.of(
            new TensorSelection(
              tensorA.getId(),
              tensorA.viewBodyAs(TensorNode.Body.class).getRange()
            )
          )
        );
      })
      .build();

    var app1 = ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(opSig.getId());
        b.output(
          "output",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorB.getId())
              .range(tensorB.viewBodyAs(TensorNode.Body.class).getRange())
              .build()
          )
        );
      })
      .build();
    var app2 = ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(opSig.getId());
        b.output(
          "output",
          List.of(
            TensorSelection
              .builder()
              .tensorId(tensorA.getId())
              .range(new ZRange(ZPoint.of(0, -3), ZPoint.of(1, 2)))
              .build()
          )
        );
      })
      .build();

    var constraint = graph
      .assertEnv()
      .assertConstraint(OperationReferenceAgreementConstraint.class);
    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.assertEnv(), graph, issueCollector);
    assertValidationIssues(
      issueCollector.getIssues(),
      ValidationIssue
        .builder()
        .type(NODE_VALIDATION_ERROR)
        .summary("Application Tensor Selection Tensor Id != Signature Tensor Id")
        .context(context ->
          context
            .name("Application Tensor Selection")
            .jsonpath(app1.getJsonPath(), "body.outputs.output[0]")
            .data(app1.viewBodyAs(ApplicationNode.Body.class).getOutputs().get("output").get(0))
        )
        .context(context ->
          context
            .name("Operation Tensor Selection")
            .jsonpath(opSig.getJsonPath(), "body.outputs.output[0]")
            .data(opSig.viewBodyAs(OperationNode.Body.class).getOutputs().get("output").get(0))
        )
        .context(app1.asValidationContext("Application Node"))
        .context(opSig.asValidationContext("Operation Node"))
        .build(),
      ValidationIssue
        .builder()
        .type(NODE_VALIDATION_ERROR)
        .summary(
          "Application Tensor Selection range %s is outside signature range %s",
          app2.viewBodyAs(ApplicationNode.Body.class).getOutputs().get("output").get(0).getRange(),
          opSig.viewBodyAs(OperationNode.Body.class).getOutputs().get("output").get(0).getRange()
        )
        .context(context ->
          context
            .name("Application Tensor Selection")
            .jsonpath(app2.getJsonPath(), "body.outputs.output[0]")
            .data(app2.viewBodyAs(ApplicationNode.Body.class).getOutputs().get("output").get(0))
        )
        .context(context ->
          context
            .name("Operation Tensor Selection")
            .jsonpath(opSig.getJsonPath(), "body.outputs.output[0]")
            .data(opSig.viewBodyAs(OperationNode.Body.class).getOutputs().get("output").get(0))
        )
        .context(app2.asValidationContext("Application Node"))
        .context(opSig.asValidationContext("Operation Node"))
        .build()
    );
  }

  @Test
  public void test_missing_operation() {
    var graph = createGraph();
    var missingOperationId = UUID.randomUUID();

    var app = ApplicationNode
      .builder(graph)
      .configure(b -> b.operationId(missingOperationId))
      .build();

    var constraint = graph
      .assertEnv()
      .assertConstraint(OperationReferenceAgreementConstraint.class);
    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.assertEnv(), graph, issueCollector);
    assertValidationIssues(
      issueCollector.getIssues(),
      ValidationIssue
        .builder()
        .type(NODE_REFERENCE_ERROR)
        .param("nodeId", missingOperationId)
        .param("nodeType", OperationNode.TYPE)
        .summary("Referenced node does not exist")
        .context(context ->
          context
            .name("Reference")
            .jsonpath(app.getJsonPath(), "body.operationId")
            .data(missingOperationId)
        )
        .context(app.asValidationContext("Application Node"))
        .build()
    );
  }

  @Test
  public void test_missing_tensor() {
    var graph = createGraph();

    var missingInputId = UUID.randomUUID();
    var missingOutputId = UUID.randomUUID();

    @SuppressWarnings("unused")
    var op = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("source");
        b.input("source", List.of(new TensorSelection(missingInputId, ZRange.newFromShape(1, 2))));
        b.output("result", List.of(new TensorSelection(missingOutputId, ZRange.newFromShape(10))));
      })
      .build();
    ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(op.getId());
        b.input(
          "source",
          List.of(
            TensorSelection
              .builder()
              .tensorId(missingInputId)
              .range(ZRange.newFromShape(1, 2))
              .build()
          )
        );
        b.output(
          "result",
          List.of(
            TensorSelection
              .builder()
              .tensorId(missingOutputId)
              .range(ZRange.newFromShape(10))
              .build()
          )
        );
      })
      .build();

    var constraint = graph
      .assertEnv()
      .assertConstraint(OperationReferenceAgreementConstraint.class);

    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.assertEnv(), graph, issueCollector);
    assertValidationIssues(
      issueCollector.getIssues(),
      ValidationIssue
        .builder()
        .type(NODE_REFERENCE_ERROR)
        .param("nodeId", missingInputId)
        .param("nodeType", TensorNode.TYPE)
        .summary("Referenced node does not exist")
        .context(context ->
          context
            .name("Reference")
            .jsonpath(op.getJsonPath(), "body.inputs.source[0]")
            .data(missingInputId)
        )
        .context(op.asValidationContext("Operation Node"))
        .build(),
      ValidationIssue
        .builder()
        .type(NODE_REFERENCE_ERROR)
        .param("nodeId", missingOutputId)
        .param("nodeType", TensorNode.TYPE)
        .summary("Referenced node does not exist")
        .context(context ->
          context
            .name("Reference")
            .jsonpath(op.getJsonPath(), "body.outputs.result[0]")
            .data(missingOutputId)
        )
        .context(op.asValidationContext("Operation Node"))
        .build()
    );
  }

  @Test
  public void test_wrong_reference_type() {
    var graph = createGraph();

    var noteNode = NoteNode.builder(graph).configure(b -> b.message("hello")).build();

    @SuppressWarnings("unused")
    var op = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("source");
        b.input(
          "source",
          List.of(new TensorSelection(noteNode.getId(), ZRange.newFromShape(1, 2)))
        );
        b.output("result", List.of(new TensorSelection(noteNode.getId(), ZRange.newFromShape(10))));
      })
      .build();
    ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(op.getId());
        b.input(
          "source",
          List.of(
            TensorSelection
              .builder()
              .tensorId(noteNode.getId())
              .range(ZRange.newFromShape(1, 2))
              .build()
          )
        );
        b.output(
          "result",
          List.of(
            TensorSelection
              .builder()
              .tensorId(noteNode.getId())
              .range(ZRange.newFromShape(10))
              .build()
          )
        );
      })
      .build();

    var constraint = graph
      .assertEnv()
      .assertConstraint(OperationReferenceAgreementConstraint.class);

    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.assertEnv(), graph, issueCollector);

    assertValidationIssues(
      issueCollector.getIssues(),
      ValidationIssue
        .builder()
        .type(NODE_REFERENCE_ERROR)
        .param("nodeId", noteNode.getId())
        .param("expectedType", TensorNode.TYPE)
        .param("actualType", noteNode.getType())
        .summary("Referenced node has the wrong type")
        .context(context ->
          context
            .name("Reference")
            .jsonpath(op.getJsonPath(), "body.inputs.source[0]")
            .data(noteNode.getId())
        )
        .context(op.asValidationContext("Operation Node"))
        .build(),
      ValidationIssue
        .builder()
        .type(NODE_REFERENCE_ERROR)
        .param("nodeId", noteNode.getId())
        .param("expectedType", TensorNode.TYPE)
        .param("actualType", noteNode.getType())
        .summary("Referenced node has the wrong type")
        .context(context ->
          context
            .name("Reference")
            .jsonpath(op.getJsonPath(), "body.outputs.result[0]")
            .data(noteNode.getId())
        )
        .context(op.asValidationContext("Operation Node"))
        .build()
    );
  }

  @Test
  public void test_referenced_tensor_has_wrong_shape() {
    var graph = createGraph();

    var tensorA = TensorNode
      .builder(graph)
      .configure(b -> {
        b.dtype("int32");
        b.shape(new ZPoint(2, 3));
      })
      .label("A")
      .build();

    var sourceOp = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("source");
        b.output("output", List.of(new TensorSelection(tensorA.getId(), ZRange.newFromShape(200))));
      })
      .build();

    ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sourceOp.getId());
        b.output("output", List.of(new TensorSelection(tensorA.getId(), ZRange.newFromShape(200))));
      })
      .build();

    var sinkOp = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("sink");
        b.input("input", List.of(new TensorSelection(tensorA.getId(), ZRange.newFromShape(200))));
      })
      .build();

    ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sinkOp.getId());
        b.input("input", List.of(new TensorSelection(tensorA.getId(), ZRange.newFromShape(200))));
      })
      .build();

    var constraint = graph
      .assertEnv()
      .assertConstraint(OperationReferenceAgreementConstraint.class);

    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.assertEnv(), graph, issueCollector);
    assertValidationIssues(
      issueCollector.getIssues(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .param("nodeType", OperationNode.TYPE)
        .param("expectedDimensions", 1)
        .param("actualDimensions", 2)
        .summary("Tensor selection has the wrong number of dimensions")
        .context(context ->
          context
            .name("Selection Range")
            .jsonpath(sourceOp.getJsonPath(), "body.outputs.output[0]")
            .data(ZRange.newFromShape(200))
        )
        .context(tensorA.asValidationContext("Tensor Node"))
        .context(sourceOp.asValidationContext("Operation Node"))
        .build(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .param("nodeType", OperationNode.TYPE)
        .param("expectedDimensions", 1)
        .param("actualDimensions", 2)
        .summary("Tensor selection has the wrong number of dimensions")
        .context(context ->
          context
            .name("Selection Range")
            .jsonpath(sinkOp.getJsonPath(), "body.inputs.input[0]")
            .data(ZRange.newFromShape(200))
        )
        .context(tensorA.asValidationContext("Tensor Node"))
        .context(sinkOp.asValidationContext("Operation Node"))
        .build()
    );
  }

  @Test
  public void test_tensor_selection_is_outside_range() {
    var graph = createGraph();

    var tensorA = TensorNode
      .builder(graph)
      .configure(b -> {
        b.dtype("int32");
        b.shape(new ZPoint(2, 3));
      })
      .label("A")
      .build();

    var sourceOp = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("source");
        b.output(
          "output",
          List.of(new TensorSelection(tensorA.getId(), ZRange.newFromShape(5, 10)))
        );
      })
      .build();

    ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sourceOp.getId());
        b.output(
          "output",
          List.of(new TensorSelection(tensorA.getId(), ZRange.newFromShape(5, 10)))
        );
      })
      .build();

    var sinkOp = OperationNode
      .builder(graph)
      .configure(b -> {
        b.kernel("sink");
        b.input("input", List.of(new TensorSelection(tensorA.getId(), ZRange.newFromShape(2, 8))));
      })
      .build();

    ApplicationNode
      .builder(graph)
      .configure(b -> {
        b.operationId(sinkOp.getId());
        b.input("input", List.of(new TensorSelection(tensorA.getId(), ZRange.newFromShape(2, 8))));
      })
      .build();

    var constraint = graph
      .assertEnv()
      .assertConstraint(OperationReferenceAgreementConstraint.class);
    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.assertEnv(), graph, issueCollector);
    assertValidationIssues(
      issueCollector.getIssues(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .param("nodeType", OperationNode.TYPE)
        .summary("Tensor selection is out of bounds")
        .context(context ->
          context
            .name("Selection Range")
            .jsonpath(sinkOp.getJsonPath(), "body.inputs.input[0]")
            .data(ZRange.newFromShape(2, 8))
        )
        .context(tensorA.asValidationContext("Tensor Node"))
        .context(sinkOp.asValidationContext("Operation Node"))
        .build(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
        .param("nodeType", OperationNode.TYPE)
        .summary("Tensor selection is out of bounds")
        .context(context ->
          context
            .name("Selection Range")
            .jsonpath(sourceOp.getJsonPath(), "body.outputs.output[0]")
            .data(ZRange.newFromShape(5, 10))
        )
        .context(tensorA.asValidationContext("Tensor Node"))
        .context(sourceOp.asValidationContext("Operation Node"))
        .build()
    );
  }

  @Test
  public void test_Cycles() {
    var graph = createGraph();

    var tensorA = TensorNode
      .builder(graph)
      .label("A")
      .configure(b -> b.dtype("int32").shape(new ZPoint(2, 3)))
      .build();

    var tensorB = TensorNode
      .builder(graph)
      .label("B")
      .configure(b -> b.dtype("int32").shape(new ZPoint(4, 5)))
      .build();

    var tensorC = TensorNode
      .builder(graph)
      .label("C")
      .configure(b -> b.dtype("int32").shape(new ZPoint(6, 7)))
      .build();

    var opNode = OperationNode
      .builder(graph)
      .label("Add")
      .configure(b -> {
        b.kernel("increment");
        b.input(
          "x",
          List.of(
            TensorSelection.builder().tensorId(tensorA.getId()).range(tensorA.getRange()).build(),
            TensorSelection.builder().tensorId(tensorB.getId()).range(tensorB.getRange()).build()
          )
        );
        b.output(
          "y",
          List.of(
            TensorSelection.builder().tensorId(tensorC.getId()).range(tensorC.getRange()).build(),
            TensorSelection.builder().tensorId(tensorA.getId()).range(tensorA.getRange()).build()
          )
        );
      })
      .build();

    ApplicationNode
      .builder(graph)
      .configure(b ->
        b
          .operationId(opNode.getId())
          .input(
            "x",
            List.of(
              TensorSelection.builder().tensorId(tensorA.getId()).range(tensorA.getRange()).build(),
              TensorSelection.builder().tensorId(tensorB.getId()).range(tensorB.getRange()).build()
            )
          )
          .output(
            "y",
            List.of(
              TensorSelection.builder().tensorId(tensorC.getId()).range(tensorC.getRange()).build(),
              TensorSelection.builder().tensorId(tensorA.getId()).range(tensorA.getRange()).build()
            )
          )
      )
      .build();

    var constraint = graph
      .assertEnv()
      .assertConstraint(OperationReferenceAgreementConstraint.class);
    var issueCollector = new ListValidationIssueCollector();
    constraint.validateConstraint(graph.assertEnv(), graph, issueCollector);
    assertValidationIssues(
      issueCollector.getIssues(),
      ValidationIssue
        .builder()
        .type(LoomConstants.Errors.REFERENCE_CYCLE_ERROR)
        .summary("Reference Cycle detected")
        .context(
          ValidationIssue.Context
            .builder()
            .name("Cycle")
            .data(
              List.of(
                Map.of("id", opNode.getId(), "type", OperationNode.TYPE, "label", "Add"),
                Map.of("id", tensorA.getId(), "type", TensorNode.TYPE, "label", "A")
              )
            )
        )
        .build()
    );
  }
}
