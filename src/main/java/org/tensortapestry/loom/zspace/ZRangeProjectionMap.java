package org.tensortapestry.loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.Value;
import org.tensortapestry.loom.zspace.impl.HasJsonOutput;

/**
 * A function which maps coordinates in a space to ranges in another space.
 */
@Value
@Immutable
@ThreadSafe
public class ZRangeProjectionMap implements HasJsonOutput {

  @SuppressWarnings("unused")
  public static class ZRangeProjectionMapBuilder {

    /**
     * Build an ZAffineMap from a matrix.
     *
     * @param matrix the matrix.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    public ZRangeProjectionMapBuilder affineMap(@Nonnull int[][] matrix) {
      return affineMap(ZAffineMap.fromMatrix(matrix));
    }

    /**
     * Set a ZAffineMap on the builder.
     *
     * @param affineMap the ZAffineMap.
     * @return {@code this}
     */
    @Nonnull
    @JsonSetter
    public ZRangeProjectionMapBuilder affineMap(@Nonnull ZAffineMap affineMap) {
      this.affineMap = affineMap;
      return this;
    }

    /**
     * Translate the ZAffineMap by the given offset.
     *
     * @param offset the offset.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    public ZRangeProjectionMapBuilder translate(@Nonnull int... offset) {
      return affineMap(affineMap.translate(offset));
    }

    /**
     * Translate the ZAffineMap by the given offset.
     *
     * @param offset the offset.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    public ZRangeProjectionMapBuilder translate(@Nonnull ZTensorWrapper offset) {
      return affineMap(affineMap.translate(offset));
    }

    /**
     * Set the shape of the output.
     *
     * @param shape the shape.
     * @return {@code this}
     */
    @Nonnull
    @JsonIgnore
    public ZRangeProjectionMapBuilder shape(@Nonnull int... shape) {
      return shape(ZPoint.of(shape));
    }

    /**
     * Set the shape of the output.
     *
     * @param shape the shape.
     * @return {@code this}
     */
    @Nonnull
    @JsonSetter
    public ZRangeProjectionMapBuilder shape(@Nonnull ZTensorWrapper shape) {
      this.shape = shape.unwrap().newZPoint();
      return this;
    }
  }

  /**
   * Create a new ZRangeProjectionMap.
   *
   * <p>This is a private builder to force the type of the {@link #affineMap} and {@link #shape}
   * used
   * in the builder to be {@link ZAffineMap} and {@link ZPoint}, respectively; and to prevent
   * collision with {@link ZTensorWrapper}.
   *
   * @param affineMap the affine map.
   * @param shape the shape.
   * @return the new ZRangeProjectionMap.
   */
  @JsonCreator
  @Builder(toBuilder = true)
  static ZRangeProjectionMap privateBuilder(
    @Nonnull @JsonProperty(value = "affineMap") ZAffineMap affineMap,
    @Nonnull @JsonProperty(value = "shape") ZPoint shape
  ) {
    return new ZRangeProjectionMap(affineMap, shape);
  }

  @Nonnull
  ZAffineMap affineMap;

  @Nonnull
  ZPoint shape;

  /**
   * Create a new ZRangeProjectionMap.
   *
   * @param affineMap the affine map.
   * @param shape the shape, or {@code null} to use one's in the affine map's output
   *         dims.
   */
  public ZRangeProjectionMap(@Nonnull ZAffineMap affineMap, @Nullable ZTensorWrapper shape) {
    this.affineMap = affineMap;
    this.shape =
      shape == null ? ZPoint.newOnes(affineMap.getOutputNDim()) : shape.unwrap().newZPoint();

    if (this.affineMap.getOutputNDim() != this.shape.getNDim()) {
      throw new IllegalArgumentException(
        String.format(
          "affineMap.outputDim() (%d) != shape.dim() (%d)",
          this.affineMap.getOutputNDim(),
          this.shape.getNDim()
        )
      );
    }
  }

  @Override
  public String toString() {
    return String.format("ipf(affineMap=%s, shape=%s)", affineMap, shape);
  }

  /**
   * Applies the projection function to the given point.
   *
   * @param source The point to project.
   * @return The projected range.
   */
  @Nonnull
  public ZRange apply(@Nonnull ZTensorWrapper source) {
    ZTensorWrapper start = affineMap.apply(source).newZPoint();
    return ZRange.builder().start(start).shape(shape).build();
  }

  /**
   * Applies the projection function to the given range.
   *
   * @param source The range to project.
   * @return the union of the projected ranges.
   */
  @Nonnull
  public ZRange apply(@Nonnull ZRange source) {
    // TODO: Does the linear nature of the affine map mean that this is sufficient?
    ZRange r1 = apply(source.getStart());
    if (source.isEmpty()) {
      ZTensorWrapper shape1 = ZPoint.newZeros(r1.getNDim());
      return ZRange.builder().start(r1.getStart()).shape(shape1).build();
    }
    return ZRange.boundingRange(r1, apply(source.getInclusiveEnd()));
  }

  /**
   * Translates the projection function by the given offset.
   *
   * @param offset The offset to translate by.
   * @return The translated projection function.
   */
  @Nonnull
  public ZRangeProjectionMap translate(@Nonnull ZTensorWrapper offset) {
    return ZRangeProjectionMap
      .builder()
      .affineMap(affineMap.translate(offset))
      .shape(shape)
      .build();
  }
}
