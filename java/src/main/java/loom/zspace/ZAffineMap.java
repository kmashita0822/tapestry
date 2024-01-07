package loom.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.JsonUtil;

/** A linear map from {@code Z^inDim} to {@code Z^outDim}. */
@Value
@ThreadSafe
@Immutable
@Jacksonized
@Builder(toBuilder = true)
public class ZAffineMap
    implements HasPermuteInput<ZAffineMap>, HasPermuteOutput<ZAffineMap>, HasToJsonString {

  /**
   * Parse a ZAffineMap from a string.
   *
   * @param str the string to parse.
   * @return the parsed ZAffineMap.
   */
  @Nonnull
  public static ZAffineMap parse(@Nonnull String str) {
    return JsonUtil.fromJson(str, ZAffineMap.class);
  }

  /**
   * Create a ZAffineMap from a matrix.
   *
   * <p>Will have a zero bias.
   *
   * @param rows the rows of the matrix.
   * @return the ZAffineMap.
   */
  @Nonnull
  public static ZAffineMap fromMatrix(int[]... rows) {
    return fromMatrix(ZTensor.newMatrix(rows));
  }

  /**
   * Create a ZAffineMap from a matrix.
   *
   * <p>Will have a zero bias.
   *
   * @param matrix the matrix.
   * @return the ZAffineMap.
   */
  @Nonnull
  public static ZAffineMap fromMatrix(@Nonnull ZTensor matrix) {
    return new ZAffineMap(matrix);
  }

  @Nonnull public ZTensor projection;

  @Nonnull public ZTensor offset;

  /**
   * Create a new ZAffineMap.
   *
   * @param projection the matrix.
   * @param offset the bias; if null, will be a zero vector of size `A.shape[0]`.
   */
  @JsonCreator
  public ZAffineMap(
      @JsonProperty(value = "A", required = true) ZTensor projection,
      @Nullable @JsonProperty(value = "b") ZTensor offset) {
    this.projection = projection.asImmutable();
    if (offset == null) {
      offset = ZTensor.newZeros(projection.shape(0));
    }
    this.offset = offset.asImmutable();

    projection.assertNDim(2);
    offset.assertNDim(1);
    if (offset.shape(0) != outputNDim()) {
      throw new IllegalArgumentException(
          String.format(
              "A.shape[1] != b.shape[0]: %s != %s",
              projection.shapeAsList(), offset.shapeAsList()));
    }
  }

  /**
   * Create a new ZAffineMap.
   *
   * @param projection the matrix.
   */
  public ZAffineMap(@Nonnull ZTensor projection) {
    this(projection, null);
  }

  /**
   * Get the output dimension of this affine map.
   *
   * @return the output dimension.
   */
  public int outputNDim() {
    return projection.shape(0);
  }

  /**
   * Get the input dimension of this affine map.
   *
   * @return the input dimension.
   */
  // This seems like a backwards way to represent this;
  // but `Ax + b` is the standard form.
  public int inputNDim() {
    return projection.shape(1);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projection, offset);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ZAffineMap that)) return false;
    return projection.equals(that.projection) && offset.equals(that.offset);
  }

  @Override
  public String toString() {
    return "λx." + projection.toJsonString() + "⋅x + " + offset.toJsonString();
  }

  @Override
  public ZAffineMap permuteInput(@Nonnull int... permutation) {
    return new ZAffineMap(projection.reorderedDimCopy(permutation, 1), offset);
  }

  @Override
  public ZAffineMap permuteOutput(@Nonnull int... permutation) {
    return new ZAffineMap(
        projection.reorderedDimCopy(permutation, 0), offset.reorderedDimCopy(permutation, 0));
  }

  /**
   * Apply this affine map to the given ZPoint.
   *
   * @param x a ZPoint of dim `inDim`.
   * @return a ZPoint of dim `outDim`.
   */
  @Nonnull
  public ZPoint apply(@Nonnull ZPoint x) {
    return new ZPoint(apply(x.coords));
  }

  /**
   * Apply this affine map to the given vector.
   *
   * @param x a 1-dim tensor of length `inDim`.
   * @return a 1-dim tensor of length `outDim`.
   */
  @Nonnull
  public ZTensor apply(@Nonnull ZTensor x) {
    // denoted in the `in` dim.
    x.assertNDim(1);
    return ZTensor.Ops.matmul(projection, x).add(offset);
  }

  /**
   * Translate this affine map by the given vector.
   *
   * @param x a 1-dim tensor of length `inDim`.
   * @return a new affine map.
   */
  @Nonnull
  public ZAffineMap translate(@Nonnull ZTensor x) {
    return new ZAffineMap(projection, offset.add(x));
  }

  /**
   * Translate this affine map by the given vector.
   *
   * @param x a ZPoint of dim `inDim`.
   * @return a new affine map.
   */
  @Nonnull
  public ZAffineMap translate(@Nonnull ZPoint x) {
    return translate(x.coords);
  }

  /**
   * Translate this affine map by the given vector.
   *
   * @param x a 1-dim array of length `inDim`.
   * @return a new affine map.
   */
  @Nonnull
  public ZAffineMap translate(@Nonnull int... x) {
    return translate(ZTensor.newVector(x));
  }
}
