package org.tensortapestry.loom.zspace.ops;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.zspace.HasZTensor;
import org.tensortapestry.loom.zspace.ZTensor;

/**
 * ZTensor cell wise operations.
 */
@UtilityClass
public class CellWise {

  /**
   * Elementwise negation of a tensor.
   *
   * @param tensor the input tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor neg(@Nonnull HasZTensor tensor) {
    return map(x -> -x, tensor);
  }

  /**
   * An element-wise unary operation.
   *
   * @param op the operation.
   * @param tensor the input tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor map(@Nonnull IntUnaryOperator op, @Nonnull HasZTensor tensor) {
    var result = ZTensor.newZerosLike(tensor);
    result.assignFromMap_(op, tensor);
    return result;
  }

  /**
   * Elementwise absolute value of a tensor.
   *
   * @param tensor the input tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor abs(@Nonnull HasZTensor tensor) {
    return map(Math::abs, tensor);
  }

  /**
   * Element-wise broadcast minimum.
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor minimum(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return zipWith(Math::min, lhs, rhs);
  }

  /**
   * An element-wise broadcast binary operation.
   *
   * @param op the operation.
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor zipWith(
    @Nonnull IntBinaryOperator op,
    @Nonnull HasZTensor lhs,
    @Nonnull HasZTensor rhs
  ) {
    var result = ZTensor.newZeros(lhs.commonBroadcastShape(rhs));
    result.assignFromZipWith_(op, lhs, rhs);
    return result;
  }

  /**
   * Element-wise broadcast minimum.
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor minimum(@Nonnull HasZTensor lhs, int rhs) {
    return zipWith(Math::min, lhs, rhs);
  }

  /**
   * An element-wise broadcast binary operation.
   *
   * @param op the operation.
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side scalar.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor zipWith(@Nonnull IntBinaryOperator op, @Nonnull HasZTensor lhs, int rhs) {
    return zipWith(op, lhs, ZTensor.newScalar(rhs));
  }

  /**
   * Element-wise broadcast minimum.
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor minimum(int lhs, @Nonnull HasZTensor rhs) {
    return zipWith(Math::min, lhs, rhs);
  }

  /**
   * An element-wise broadcast binary operation.
   *
   * @param op the operation.
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor zipWith(@Nonnull IntBinaryOperator op, int lhs, @Nonnull HasZTensor rhs) {
    return zipWith(op, ZTensor.newScalar(lhs), rhs);
  }

  /**
   * Element-wise broadcast maximum.
   *
   * @param lhs the left-hand side.
   * @param rhs the right-hand side.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor maximum(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return zipWith(Math::max, lhs, rhs);
  }

  /**
   * Element-wise broadcast maximum.
   *
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side scalar.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor maximum(@Nonnull HasZTensor lhs, int rhs) {
    return zipWith(Math::max, lhs, rhs);
  }

  /**
   * Element-wise broadcast maximum.
   *
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side scalar.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor maximum(int lhs, @Nonnull HasZTensor rhs) {
    return zipWith(Math::max, lhs, rhs);
  }

  /**
   * Element-wise broadcast addition.
   *
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side scalar.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor add(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return zipWith(Integer::sum, lhs, rhs);
  }

  /**
   * Element-wise broadcast addition.
   *
   * @param lhs the left-hand side tensor.
   * @param rhs the right-hand side scalar.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor add(@Nonnull HasZTensor lhs, int rhs) {
    return zipWith(Integer::sum, lhs, rhs);
  }

  /**
   * Element-wise broadcast addition.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor add(int lhs, @Nonnull HasZTensor rhs) {
    return zipWith(Integer::sum, lhs, rhs);
  }

  /**
   * Element-wise broadcast in-place addition on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param rhs the right-hand side tensor.
   */
  public static void add_(@Nonnull ZTensor lhs, @Nonnull HasZTensor rhs) {
    lhs.zipWith_(Integer::sum, rhs);
  }

  /**
   * Element-wise broadcast in-place addition on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param rhs the right-hand side tensor.
   */
  public static void add_(@Nonnull ZTensor lhs, int rhs) {
    lhs.zipWith_(Integer::sum, rhs);
  }

  /**
   * Element-wise broadcast subtraction.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor sub(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return zipWith((l, r) -> l - r, lhs, rhs);
  }

  /**
   * Element-wise broadcast subtraction.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor sub(@Nonnull HasZTensor lhs, int rhs) {
    return zipWith((l, r) -> l - r, lhs, rhs);
  }

  /**
   * Element-wise broadcast subtraction.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor sub(int lhs, @Nonnull HasZTensor rhs) {
    return zipWith((l, r) -> l - r, lhs, rhs);
  }

  /**
   * Element-wise broadcast in-place subtraction on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param rhs the right-hand side tensor.
   */
  public static void sub_(@Nonnull ZTensor lhs, @Nonnull HasZTensor rhs) {
    lhs.zipWith_((l, r) -> l - r, rhs);
  }

  /**
   * Element-wise broadcast in-place subtraction on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param rhs the right-hand side tensor.
   */
  public static void sub_(@Nonnull ZTensor lhs, int rhs) {
    lhs.zipWith_((l, r) -> l - r, rhs);
  }

  /**
   * Element-wise broadcast multiplication.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor mul(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return zipWith((l, r) -> l * r, lhs, rhs);
  }

  /**
   * Element-wise broadcast multiplication.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor mul(@Nonnull HasZTensor lhs, int rhs) {
    return zipWith((l, r) -> l * r, lhs, rhs);
  }

  /**
   * Element-wise broadcast multiplication.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor mul(int lhs, @Nonnull HasZTensor rhs) {
    return zipWith((l, r) -> l * r, lhs, rhs);
  }

  /**
   * Element-wise broadcast in-place multiplication on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param rhs the right-hand side tensor.
   */
  public static void mul_(@Nonnull ZTensor lhs, @Nonnull HasZTensor rhs) {
    lhs.zipWith_((l, r) -> l * r, rhs);
  }

  /**
   * Element-wise broadcast in-place multiplication on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param rhs the right-hand side tensor.
   */
  public static void mul_(@Nonnull ZTensor lhs, int rhs) {
    lhs.zipWith_((l, r) -> l * r, rhs);
  }

  /**
   * Element-wise broadcast division.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor div(@Nonnull HasZTensor lhs, @Nonnull HasZTensor rhs) {
    return zipWith((l, r) -> l / r, lhs, rhs);
  }

  /**
   * Element-wise broadcast division.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor div(@Nonnull HasZTensor lhs, int rhs) {
    return zipWith((l, r) -> l / r, lhs, rhs);
  }

  /**
   * Element-wise broadcast division.
   *
   * @param lhs the left-hand side scalar.
   * @param rhs the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor div(int lhs, @Nonnull HasZTensor rhs) {
    return zipWith((l, r) -> l / r, lhs, rhs);
  }

  /**
   * Element-wise broadcast in-place division on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param rhs the right-hand side tensor.
   */
  public static void div_(@Nonnull ZTensor lhs, @Nonnull HasZTensor rhs) {
    lhs.zipWith_((l, r) -> l / r, rhs);
  }

  /**
   * Element-wise broadcast in-place division on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param rhs the right-hand side tensor.
   */
  public static void div_(@Nonnull ZTensor lhs, int rhs) {
    lhs.zipWith_((l, r) -> l / r, rhs);
  }

  /**
   * Element-wise broadcast mod.
   *
   * @param lhs the left-hand side scalar.
   * @param base the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor mod(@Nonnull HasZTensor lhs, @Nonnull HasZTensor base) {
    return zipWith((l, r) -> l % r, lhs, base);
  }

  /**
   * Element-wise broadcast mod.
   *
   * @param lhs the left-hand side scalar.
   * @param base the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor mod(@Nonnull HasZTensor lhs, int base) {
    return zipWith((l, r) -> l % r, lhs, base);
  }

  /**
   * Element-wise broadcast mod.
   *
   * @param lhs the left-hand side scalar.
   * @param base the right-hand side tensor.
   * @return a new tensor.
   */
  @Nonnull
  public static ZTensor mod(int lhs, @Nonnull HasZTensor base) {
    return zipWith((l, r) -> l % r, lhs, base);
  }

  /**
   * Element-wise broadcast in-place mod on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param base the right-hand side tensor.
   */
  public static void mod_(@Nonnull ZTensor lhs, @Nonnull HasZTensor base) {
    lhs.zipWith_((l, r) -> l % r, base);
  }

  /**
   * Element-wise broadcast in-place mod on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param base the right-hand side tensor.
   */
  public static void mod_(@Nonnull ZTensor lhs, int base) {
    lhs.zipWith_((l, r) -> l % r, base);
  }

  /**
   * Compute the integer power of a number.
   *
   * @param base the base.
   * @param exp the exponent.
   * @return the integer power.
   */
  public int intPow(int base, int exp) {
    if (exp < 0) {
      throw new IllegalArgumentException("exponent must be non-negative");
    }
    int result = 1;
    while (exp > 0) {
      if (exp % 2 == 1) {
        result *= base;
      }
      base *= base;
      exp /= 2;
    }
    return result;
  }

  /**
   * Element-wise broadcast power.
   *
   * @param lhs the left-hand side scalar.
   * @param exp the right-hand side tensor.
   * @return a new tensor.
   */
  public static ZTensor pow(@Nonnull HasZTensor lhs, @Nonnull HasZTensor exp) {
    return zipWith(CellWise::intPow, lhs, exp);
  }

  /**
   * Element-wise broadcast power.
   *
   * @param lhs the left-hand side tensor.
   * @param exp the right-hand side scalar.
   * @return a new tensor.
   */
  public static ZTensor pow(int lhs, @Nonnull HasZTensor exp) {
    return zipWith(CellWise::intPow, lhs, exp);
  }

  /**
   * Element-wise broadcast power.
   *
   * @param lhs the left-hand side tensor.
   * @param exp the right-hand side scalar.
   * @return a new tensor.
   */
  public static ZTensor pow(@Nonnull HasZTensor lhs, int exp) {
    return zipWith(CellWise::intPow, lhs, exp);
  }

  /**
   * Element-wise broadcast in-place power on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param exp the right-hand side tensor.
   */
  public static void pow_(@Nonnull ZTensor lhs, @Nonnull HasZTensor exp) {
    lhs.zipWith_(CellWise::intPow, exp);
  }

  /**
   * Element-wise broadcast in-place power on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param exp the right-hand side tensor.
   */
  public static void pow_(@Nonnull ZTensor lhs, int exp) {
    lhs.zipWith_(CellWise::intPow, exp);
  }

  /**
   * Compute the integer logarithm of a number.
   *
   * @param value the value.
   * @param base the base.
   * @return the integer logarithm.
   */
  public int intLog(int value, int base) {
    if (base <= 1) {
      throw new IllegalArgumentException("Base must be greater than 1");
    }
    if (value <= 0) {
      throw new IllegalArgumentException("Value must be positive");
    }

    int low = 0;
    int high = value;
    while (low < high) {
      int mid = (low + high) / 2;
      int pow = intPow(base, mid);

      if (pow == value) {
        return mid;
      } else if (pow < value) {
        low = mid + 1;
      } else {
        high = mid;
      }
    }

    return low - 1;
  }

  /**
   * Element-wise broadcast log.
   *
   * @param lhs the left-hand side tensor.
   * @param base the right-hand side tensor.
   * @return a new tensor.
   */
  public static ZTensor log(@Nonnull HasZTensor lhs, @Nonnull HasZTensor base) {
    return zipWith(CellWise::intLog, lhs, base);
  }

  /**
   * Element-wise broadcast log.
   *
   * @param lhs the left-hand side scalar.
   * @param base the right-hand side tensor.
   * @return a new tensor.
   */
  public static ZTensor log(int lhs, @Nonnull HasZTensor base) {
    return zipWith(CellWise::intLog, lhs, base);
  }

  /**
   * Element-wise broadcast log.
   *
   * @param lhs the left-hand side tensor.
   * @param base the right-hand side scalar.
   * @return a new tensor.
   */
  public static ZTensor log(@Nonnull HasZTensor lhs, int base) {
    return zipWith(CellWise::intLog, lhs, base);
  }

  /**
   * Element-wise broadcast in-place log on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param base the right-hand side tensor.
   */
  public static void log_(@Nonnull ZTensor lhs, @Nonnull HasZTensor base) {
    lhs.zipWith_(CellWise::intLog, base);
  }

  /**
   * Element-wise broadcast in-place log on the lhs.
   *
   * @param lhs the left-hand side tensor, modified in-place; must be mutable.
   * @param base the right-hand side tensor.
   */
  public static void log_(@Nonnull ZTensor lhs, int base) {
    lhs.zipWith_(CellWise::intLog, base);
  }
}
