package loom.zspace;

import com.fasterxml.jackson.annotation.JsonValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Data;
import loom.common.json.HasToJsonString;

/**
 * Base class for immutable wrappers around ZTensors.
 *
 * @param <T> the type of the subclass.
 */
@ThreadSafe
@Immutable
@Data
public abstract class ImmutableZTensorWrapper<T> implements HasZTensor, Cloneable, HasToJsonString {

  @JsonValue
  @Nonnull
  @SuppressWarnings("Immutable")
  protected final ZTensor tensor;

  /**
   * Create a new instance of {@code T} from a {@link ZTensor}.
   *
   * @param tensor the tensor.
   */
  public ImmutableZTensorWrapper(@Nonnull HasZTensor tensor) {
    this.tensor = tensor.getTensor().asImmutable();
  }

  /**
   * Create a new instance of {@code T} from a {@link ZTensor}.
   *
   * @param tensor the tensor.
   * @return the new instance.
   */
  @Nonnull
  protected abstract T create(@Nonnull HasZTensor tensor);

  /**
   * Returns {@code this} as {@code T}.
   *
   * @return {@code this} as {@code T}.
   */
  @SuppressWarnings("unchecked")
  @Nonnull
  private T self() {
    return (T) this;
  }

  /**
   * Immutable, so returns {@code this}.
   *
   * @return {@code this}.
   */
  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public final T clone() {
    // Immutable, so no need to clone.
    return self();
  }

  @Override
  public final int hashCode() {
    return tensor.hashCode();
  }

  @Override
  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  public final boolean equals(@Nullable Object other) {
    return tensor.equals(other);
  }

  @Override
  public final String toString() {
    return tensor.toString();
  }

  /**
   * Are all cells {@code > 0}?
   *
   * @return true if all cells are {@code > 0}.
   */
  public boolean isStrictlyPositive() {
    return tensor.isStrictlyPositive();
  }

  /**
   * Are all cells {@code >= 0}?
   *
   * @return true if all cells are {@code >= 0}.
   */
  public boolean isNonNegative() {
    return tensor.isNonNegative();
  }

  /**
   * Is {@code this == rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean eq(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.eq(this, rhs);
  }

  /**
   * Is {@code this != rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean ne(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.ne(this, rhs);
  }

  /**
   * Is {@code this < rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean lt(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.lt(this, rhs);
  }

  /**
   * Is {@code this <= rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean le(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.le(this, rhs);
  }

  /**
   * Is {@code this > rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean gt(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.gt(this, rhs);
  }

  /**
   * Is {@code this >= rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean ge(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.ge(this, rhs);
  }

  /**
   * Returns the sum of all elements.
   *
   * @return the sum of all elements.
   */
  public final int sumAsInt() {
    return tensor.sumAsInt();
  }

  /**
   * Returns the product of all elements.
   *
   * @return the product of all elements.
   */
  public final int prodAsInt() {
    return tensor.prodAsInt();
  }

  /**
   * Returns the maximum element value.
   *
   * @return the maximum element value.
   */
  public final int maxAsInt() {
    return tensor.maxAsInt();
  }

  /**
   * Returns the minimum element value.
   *
   * @return the minimum element value.
   */
  public final int minAsInt() {
    return tensor.minAsInt();
  }

  /**
   * Returns the cell-wise negation as a new {@code T}.
   *
   * @return the cell-wise negation as a new {@code T}.
   */
  public final T neg() {
    return create(ZTensorOperations.neg(this));
  }

  /**
   * Returns the cell-wise absolute value as a new {@code T}.
   *
   * @return the cell-wise absolute value as a new {@code T}.
   */
  public final T abs() {
    return create(ZTensorOperations.abs(this));
  }

  /**
   * Returns the broadcast addition with {@code rhs} as a new {@code T}.
   *
   * @param rhs the right-hand side.
   * @return the broadcast addition with {@code rhs} as a new {@code T}.
   */
  public final T add(@Nonnull HasZTensor rhs) {
    return create(ZTensorOperations.add(this, rhs));
  }

  /**
   * Returns the cell-wise addition with {@code rhs} as a new {@code T}.
   *
   * @param rhs the right-hand side.
   * @return the cell-wise addition with {@code rhs} as a new {@code T}.
   */
  public final T add(int rhs) {
    return create(ZTensorOperations.add(this, rhs));
  }

  /**
   * Returns the broadcast subtraction with {@code rhs} as a new {@code T}.
   *
   * @param rhs the right-hand side.
   * @return the broadcast subtraction with {@code rhs} as a new {@code T}.
   */
  public final T sub(@Nonnull HasZTensor rhs) {
    return create(ZTensorOperations.sub(this, rhs));
  }

  /**
   * Returns the cell-wise subtraction with {@code rhs} as a new {@code T}.
   *
   * @param rhs the right-hand side.
   * @return the cell-wise subtraction with {@code rhs} as a new {@code T}.
   */
  public final T sub(int rhs) {
    return create(ZTensorOperations.sub(this, rhs));
  }

  /**
   * Returns the broadcast multiplication with {@code rhs} as a new {@code T}.
   *
   * @param rhs the right-hand side.
   * @return the broadcast multiplication with {@code rhs} as a new {@code T}.
   */
  public final T mul(@Nonnull HasZTensor rhs) {
    return create(ZTensorOperations.mul(this, rhs));
  }

  /**
   * Returns the cell-wise multiplication with {@code rhs} as a new {@code T}.
   *
   * @param rhs the right-hand side.
   * @return the cell-wise multiplication with {@code rhs} as a new {@code T}.
   */
  public final T mul(int rhs) {
    return create(ZTensorOperations.mul(this, rhs));
  }

  /**
   * Returns the broadcast division with {@code rhs} as a new {@code T}.
   *
   * @param rhs the right-hand side.
   * @return the broadcast division with {@code rhs} as a new {@code T}.
   */
  public final T div(@Nonnull HasZTensor rhs) {
    return create(ZTensorOperations.div(this, rhs));
  }

  /**
   * Returns the cell-wise division with {@code rhs} as a new {@code T}.
   *
   * @param rhs the right-hand side.
   * @return the cell-wise division with {@code rhs} as a new {@code T}.
   */
  public final T div(int rhs) {
    return create(ZTensorOperations.div(this, rhs));
  }

  /**
   * Returns the broadcast modulo with {@code rhs} as a new {@code T}.
   *
   * @param base the right-hand side.
   * @return the broadcast modulo with {@code rhs} as a new {@code T}.
   */
  public final T mod(@Nonnull HasZTensor base) {
    return create(ZTensorOperations.mod(this, base));
  }

  /**
   * Returns the cell-wise modulo with {@code rhs} as a new {@code T}.
   *
   * @param base the right-hand side.
   * @return the cell-wise modulo with {@code rhs} as a new {@code T}.
   */
  public final T mod(int base) {
    return create(ZTensorOperations.mod(this, base));
  }

  /**
   * Returns the broadcast power with {@code rhs} as a new {@code T}.
   *
   * @param exp the right-hand side.
   * @return the broadcast power with {@code rhs} as a new {@code T}.
   */
  public final T pow(@Nonnull HasZTensor exp) {
    return create(ZTensorOperations.pow(this, exp));
  }

  /**
   * Returns the cell-wise power with {@code rhs} as a new {@code T}.
   *
   * @param exp the right-hand side.
   * @return the cell-wise power with {@code rhs} as a new {@code T}.
   */
  public final T pow(int exp) {
    return create(ZTensorOperations.pow(this, exp));
  }

  /**
   * Returns the broadcast log with {@code rhs} as a new {@code T}.
   *
   * @param base the right-hand side.
   * @return the broadcast log with {@code rhs} as a new {@code T}.
   */
  public final T log(@Nonnull HasZTensor base) {
    return create(ZTensorOperations.log(this, base));
  }

  /**
   * Returns the cell-wise log with {@code rhs} as a new {@code T}.
   *
   * @param base the right-hand side.
   * @return the cell-wise log with {@code rhs} as a new {@code T}.
   */
  public final T log(int base) {
    return create(ZTensorOperations.log(this, base));
  }
}
