package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.common.json.HasToJsonString;
import org.tensortapestry.loom.graph.dialects.common.JsdType;

/**
 * The body of the ApplicationNode.
 */
@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({ "operationId", "inputs", "outputs" })
@JsdType(ApplicationNode.TYPE)
public class ApplicationBody implements HasToJsonString {

  @Nonnull
  UUID operationId;

  @Singular
  @Nonnull
  Map<String, List<TensorSelection>> inputs;

  @Singular
  @Nonnull
  Map<String, List<TensorSelection>> outputs;
}
