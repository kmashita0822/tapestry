package loom.graph.nodes;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.WithSchema;
import loom.graph.LoomNode;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class NoteNode extends LoomNode<NoteNode, NoteNode.Body> {
  public static final String TYPE = "NoteNode";

  @Data
  @Jacksonized
  @Builder
  @WithSchema(
      """
  {
      "type": "object",
      "properties": {
        "message": {
            "type": "string"
        }
      },
      "required": ["message"],
      "additionalProperties": false
  }
  """)
  public static final class Body {

    @Nonnull private String message;
  }

  public abstract static class NoteNodeBuilder<C extends NoteNode, B extends NoteNodeBuilder<C, B>>
      extends LoomNodeBuilder<NoteNode, Body, C, B> {
    {
      type(TYPE);
    }
  }

  public static NoteNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate @Nonnull private Body body;
}
