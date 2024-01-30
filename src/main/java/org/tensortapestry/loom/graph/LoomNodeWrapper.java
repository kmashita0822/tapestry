package org.tensortapestry.loom.graph;

import java.util.UUID;
import javax.annotation.Nonnull;
import org.tensortapestry.loom.common.collections.Wrapper;

/**
 * Wrapper for a LoomNode.
 */
public interface LoomNodeWrapper extends Wrapper<LoomNode> {
  /**
   * Get the ID of the node.
   * @return the ID.
   */
  @Nonnull
  default UUID getId() {
    return unwrap().getId();
  }

  /**
   * Unwrap the node.
   * @return the unwrapped node.
   */
  @Nonnull
  LoomNode unwrap();
}
