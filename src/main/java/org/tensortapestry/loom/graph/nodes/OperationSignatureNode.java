package org.tensortapestry.loom.graph.nodes;

import static org.tensortapestry.loom.graph.LoomConstants.LOOM_CORE_NODE_TYPE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.common.json.HasToJsonString;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.constraints.OperationReferenceAgreementConstraint;

@Jacksonized
@SuperBuilder
@Getter
@Setter
@LoomEnvironment.WithConstraints({ OperationReferenceAgreementConstraint.class })
public final class OperationSignatureNode
  extends LoomNode<OperationSignatureNode, OperationSignatureNode.Body> {

  public static final String TYPE = LOOM_CORE_NODE_TYPE.apply("OperationSignature");

  @SuppressWarnings("unused")
  public abstract static class OperationSignatureNodeBuilder<
    C extends OperationSignatureNode, B extends OperationSignatureNodeBuilder<C, B>
  >
    extends LoomNodeBuilder<OperationSignatureNode, Body, C, B> {
    {
      // Set the node type.
      type(TYPE);
    }
  }

  @Value
  @Jacksonized
  @Builder
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonPropertyOrder({ "kernel", "params", "inputs", "outputs" })
  public static class Body implements HasToJsonString {

    @JsonProperty(required = true)
    @Nonnull
    String kernel;

    @Singular
    Map<String, Object> params;

    @Singular
    @Nonnull
    Map<String, List<TensorSelection>> inputs;

    @Singular
    @Nonnull
    Map<String, List<TensorSelection>> outputs;
  }

  public static OperationSignatureNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate(excludes = { HasToJsonString.class })
  @Nonnull
  private Body body;

  @JsonIgnore
  public List<ApplicationNode> getApplicationNodes() {
    var id = getId();
    return assertGraph()
      .nodeScan()
      .type(ApplicationNode.TYPE)
      .nodeClass(ApplicationNode.class)
      .asStream()
      .filter(appNode -> appNode.getOperationId().equals(id))
      .sorted(Comparator.comparing(LoomNode::getId))
      .toList();
  }
}
