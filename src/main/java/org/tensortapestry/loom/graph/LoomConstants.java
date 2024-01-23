package org.tensortapestry.loom.graph;

import java.util.function.Function;

public final class LoomConstants {

  public static final String NODE_VALIDATION_ERROR = "NodeValidationError";

  public static final String NODE_REFERENCE_ERROR = "NodeReferenceError";

  public static final String REFERENCE_CYCLE_ERROR = "ReferenceCycle";
  public static final String LOOM_CORE_SCHEMA =
    "https://tensortapestry.org/schemas/loom/core.0.0.1.xsd";
  public static final Function<String, String> LOOM_CORE_SUB_SCHEMA = (String target) ->
    "%s#%s".formatted(LOOM_CORE_SCHEMA, target);
  public static final Function<String, String> LOOM_CORE_NODE_TYPE = (String target) ->
    LOOM_CORE_SUB_SCHEMA.apply("nodes/%s".formatted(target));

  public static final class Errors {

    public static final String NODE_SCHEMA_ERROR = "NodeSchemaError";

    private Errors() {}
  }

  private LoomConstants() {}
}
