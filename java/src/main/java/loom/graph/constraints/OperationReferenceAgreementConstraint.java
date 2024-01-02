package loom.graph.constraints;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import loom.common.json.JsonPathUtils;
import loom.common.lazy.LazyString;
import loom.common.lazy.Thunk;
import loom.graph.*;
import loom.graph.nodes.*;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZRange;

public class OperationReferenceAgreementConstraint implements LoomEnvironment.Constraint {
  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertClassForType(TensorNode.TYPE, TensorNode.class);
    env.assertClassForType(OperationSignatureNode.TYPE, OperationSignatureNode.class);
    env.assertClassForType(ApplicationNode.TYPE, ApplicationNode.class);
  }

  @Override
  public void validateConstraint(
      @SuppressWarnings("unused") LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector) {
    boolean valid = true;
    for (var node : graph.iterableNodes(ApplicationNode.TYPE, ApplicationNode.class)) {
      var opNode =
          ValidationUtils.validateNodeReference(
              graph,
              node.getOperationId(),
              OperationSignatureNode.TYPE,
              OperationSignatureNode.class,
              new LazyString(
                  () -> JsonPathUtils.concatJsonPath(node.getJsonPath(), "body", "operationId")),
              issueCollector,
              Thunk.of(() -> List.of(node.asValidationContext("Application Node"))));

      if (opNode == null) {
        valid = false;
      }
    }

    for (var node :
        graph.iterableNodes(OperationSignatureNode.TYPE, OperationSignatureNode.class)) {
      valid &= validateOperationSignatureNode(graph, node, issueCollector);
    }

    if (valid) {
      for (var cycle : TraversalUtils.findOperationSimpleCycles(graph)) {
        var cycleDesc =
            cycle.stream()
                .map(
                    item -> {
                      var desc = new HashMap<>();
                      desc.put("id", item.getId());
                      desc.put("type", item.getType());
                      if (item.getLabel() != null) {
                        desc.put("label", item.getLabel());
                      }
                      return desc;
                    })
                .toList();

        issueCollector.addIssue(
            ValidationIssue.builder()
                .type(LoomConstants.REFERENCE_CYCLE_ERROR)
                .summary("Reference Cycle detected")
                .context(b -> b.name("Cycle").data(cycleDesc)));
      }
    }
  }

  private static boolean validateOperationSignatureNode(
      LoomGraph graph,
      OperationSignatureNode opSignatureNode,
      ValidationIssueCollector issueCollector) {
    boolean valid = true;
    var lazyContexts =
        Thunk.of(() -> List.of(opSignatureNode.asValidationContext("Operation Node")));

    valid &=
        validateTensorSliceMap(
            graph,
            opSignatureNode,
            "inputs",
            opSignatureNode.getInputs(),
            lazyContexts,
            issueCollector);
    valid &=
        validateTensorSliceMap(
            graph,
            opSignatureNode,
            "outputs",
            opSignatureNode.getOutputs(),
            lazyContexts,
            issueCollector);

    var shards = opSignatureNode.getApplicationNodes();

    if (shards.isEmpty()) {
      // There are no shards, so we can't validate the shard ranges.
      issueCollector.addIssue(
          ValidationIssue.builder()
              .type(LoomConstants.NODE_VALIDATION_ERROR)
              .summary(
                  "Operation Signature %s has no Application shards", opSignatureNode.getLabel())
              .withContexts(lazyContexts));
      return false;
    }

    if (!shards.stream()
        .allMatch(shard -> validateApplicationNode(opSignatureNode, shard, issueCollector))) {
      // The shards are not valid, so we can't validate the shard ranges.
      return false;
    }

    // check the input range coverage:
    // 1. Every shard range is inside the input range.
    // 2. The bounding union of the shard ranges is equal to the input range.
    for (var entry : opSignatureNode.getInputs().entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();

      final var k = selections.size();

      var shardSelections = shards.stream().map(shard -> shard.getInputs().get(ioName)).toList();

      if (shardSelections.stream().anyMatch(s -> s.size() != k)) {
        // The shard selection map is not the same size as the operation signature selection map;
        // the errors for that are handled in the ApplicationNode validation.
        valid = false;
        continue;
      }

      for (int idx = 0; idx < k; ++idx) {
        var sigRange = selections.get(idx).getRange();
        var finalIdx = idx;
        var shardRanges = shardSelections.stream().map(s -> s.get(finalIdx).getRange()).toList();

        var boundingRange = ZRange.boundingRange(shardRanges);

        if (!sigRange.equals(boundingRange)) {
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .summary(
                      "Operation Signature %s input %s range %s != shard bounding range %s",
                      opSignatureNode.getLabel(), ioName, sigRange, boundingRange));
          valid = false;
        }
      }
    }

    // check the output range coverage:
    // 1. None of the shard ranges overlap.
    // 2. Every shard range is inside the output range.
    // 3. The sum of the shard range sizes is equal to the output range size.
    for (var entry : opSignatureNode.getOutputs().entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();

      final var k = selections.size();

      var shardSelections = shards.stream().map(shard -> shard.getOutputs().get(ioName)).toList();

      if (shardSelections.stream().anyMatch(s -> s.size() != k)) {
        // The shard selection map is not the same size as the operation signature selection map;
        // the errors for that are handled in the ApplicationNode validation.
        valid = false;
        continue;
      }

      for (int idx = 0; idx < k; ++idx) {
        var sigRange = selections.get(idx).getRange();
        var finalIdx = idx;

        var shardRanges = shardSelections.stream().map(s -> s.get(finalIdx).getRange()).toList();
        var boundingRange = ZRange.boundingRange(shardRanges);
        var totalSize = shardRanges.stream().mapToInt(ZRange::getSize).sum();

        if (!sigRange.equals(boundingRange)) {
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .summary(
                      "Operation Signature %s output %s range %s != shard bounding range %s",
                      opSignatureNode.getLabel(), ioName, sigRange, boundingRange));
          valid = false;
        }

        if (totalSize != sigRange.getSize()) {
          // There are overlapping shard ranges.
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .summary("Overlapping Application output ranges"));
          valid = false;
        }
      }
    }

    return valid;
  }

  private static boolean validateApplicationNode(
      OperationSignatureNode opSig, ApplicationNode node, ValidationIssueCollector issueCollector) {

    var lazyContexts = Thunk.of(() -> List.of(node.asValidationContext("Operation Node")));

    return validateApplicationSignatureAgreement(
        node, opSig, "inputs", node.getInputs(), opSig.getInputs(), lazyContexts, issueCollector);
  }

  public static boolean validateApplicationSignatureAgreement(
      ApplicationNode appNode,
      OperationSignatureNode opSig,
      String sliceMapName,
      Map<String, List<TensorSelection>> appSelectionMap,
      Map<String, List<TensorSelection>> sigSelectionMap,
      Supplier<List<ValidationIssue.Context>> contextsSupplier,
      ValidationIssueCollector issueCollector) {
    boolean valid = true;
    var appKeys = appSelectionMap.keySet();
    var sigKeys = sigSelectionMap.keySet();
    if (!appKeys.equals(sigKeys)) {
      issueCollector.addIssue(
          ValidationIssue.builder()
              .type(LoomConstants.NODE_VALIDATION_ERROR)
              .summary(
                  "Application Node %s keys %s != Operation Signature %s keys %s",
                  sliceMapName,
                  appKeys.stream().sorted().toList(),
                  sliceMapName,
                  sigKeys.stream().sorted().toList())
              .withContexts(contextsSupplier));
      valid = false;
    } else {
      for (var entry : appSelectionMap.entrySet()) {
        final var ioName = entry.getKey();
        final var appSelections = entry.getValue();
        final var sigSelections = sigSelectionMap.get(ioName);

        if (appSelections.size() != sigSelections.size()) {
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .summary(
                      "Application %s key \"%s\" selection size (%d) != Signature size (%d)",
                      sliceMapName, ioName, appSelections.size(), sigSelections.size())
                  .withContexts(contextsSupplier));
          valid = false;
        } else {
          for (int idx = 0; idx < appSelections.size(); ++idx) {
            var appSelection = appSelections.get(idx);
            var sigSelection = sigSelections.get(idx);

            var finalIdx = idx;

            Supplier<List<ValidationIssue.Context>> localContext =
                () ->
                    List.of(
                        ValidationIssue.Context.builder()
                            .name("Application Tensor Selection")
                            .jsonpath(
                                JsonPathUtils.concatJsonPath(
                                    appNode.getJsonPath(),
                                    "body",
                                    sliceMapName,
                                    ioName,
                                    "[%d]".formatted(finalIdx)))
                            .data(appSelection)
                            .build(),
                        ValidationIssue.Context.builder()
                            .name("Operation Tensor Selection")
                            .jsonpath(
                                JsonPathUtils.concatJsonPath(
                                    opSig.getJsonPath(),
                                    "body",
                                    sliceMapName,
                                    ioName,
                                    "[%d]".formatted(finalIdx)))
                            .data(sigSelection)
                            .build());

            if (!appSelection.getTensorId().equals(sigSelection.getTensorId())) {
              issueCollector.addIssue(
                  ValidationIssue.builder()
                      .type(LoomConstants.NODE_VALIDATION_ERROR)
                      .summary("Application Tensor Selection Tensor Id != Signature Tensor Id")
                      .withContexts(localContext)
                      .withContexts(contextsSupplier));
              valid = false;
            } else {
              if (!sigSelection.getRange().contains(appSelection.getRange())) {
                issueCollector.addIssue(
                    ValidationIssue.builder()
                        .type(LoomConstants.NODE_VALIDATION_ERROR)
                        .summary(
                            "Application Tensor Selection range %s is outside signature range %s",
                            appSelection.getRange(), sigSelection.getRange())
                        .withContexts(localContext)
                        .withContexts(contextsSupplier));
                valid = false;
              }
            }
          }
        }
      }
    }
    return valid;
  }

  private static boolean validateTensorSliceMap(
      LoomGraph graph,
      LoomNode<?, ?> node,
      String sliceMapName,
      Map<String, List<TensorSelection>> selectionMap,
      Supplier<List<ValidationIssue.Context>> contextsSupplier,
      ValidationIssueCollector issueCollector) {
    boolean valid = true;
    for (var entry : selectionMap.entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();

      var fieldPath =
          LazyString.of(
              () -> JsonPathUtils.concatJsonPath(node.getJsonPath(), "body", sliceMapName, ioName));

      for (int idx = 0; idx < selections.size(); ++idx) {
        var tensorSelection = selections.get(idx);

        var itemPath = LazyString.format("%s[%d]", fieldPath, idx);

        TensorNode tensorNode =
            ValidationUtils.validateNodeReference(
                graph,
                tensorSelection.getTensorId(),
                TensorNode.TYPE,
                TensorNode.class,
                itemPath,
                issueCollector,
                contextsSupplier);

        if (tensorNode == null) {
          valid = false;
          continue;
        }
        var tensorRange = tensorNode.getEffectiveRange();

        var selectionRange = tensorSelection.getRange();
        var lazySelectionRangeContext =
            Thunk.of(
                () ->
                    ValidationIssue.Context.builder()
                        .name("Selection Range")
                        .jsonpath(itemPath)
                        .data(selectionRange)
                        .build());

        if (selectionRange.getNDim() != tensorRange.getNDim()) {
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .param("nodeType", OperationSignatureNode.TYPE)
                  .param("expectedDimensions", selectionRange.getNDim())
                  .param("actualDimensions", tensorRange.getNDim())
                  .summary("Tensor selection has the wrong number of dimensions")
                  .context(lazySelectionRangeContext)
                  .context(tensorNode.asValidationContext("Tensor Node"))
                  .withContexts(contextsSupplier)
                  .build());
          valid = false;
          continue;
        }

        if (!tensorRange.contains(selectionRange)) {
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .param("nodeType", OperationSignatureNode.TYPE)
                  .summary("Tensor selection is out of bounds")
                  .context(lazySelectionRangeContext)
                  .context(tensorNode.asValidationContext("Tensor Node"))
                  .withContexts(contextsSupplier)
                  .build());
          valid = false;
        }
      }
    }
    return valid;
  }
}
