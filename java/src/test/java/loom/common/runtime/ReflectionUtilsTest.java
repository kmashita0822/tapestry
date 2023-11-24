package loom.common.runtime;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class ReflectionUtilsTest implements CommonAssertions {
  @Test
  public void testCheckIsSubclass() {
    @SuppressWarnings("SameReturnValue")
    interface I {
      default int foo() {
        return 2;
      }
    }

    class A {}
    class B extends A implements I {}

    assertThat(new B().foo()).isEqualTo(2);

    ReflectionUtils.checkIsSubclass(B.class, A.class);
    assertThatExceptionOfType(ClassCastException.class)
        .isThrownBy(() -> ReflectionUtils.checkIsSubclass(A.class, B.class));

    ReflectionUtils.checkIsSubclass(B.class, I.class);
    assertThatExceptionOfType(ClassCastException.class)
        .isThrownBy(() -> ReflectionUtils.checkIsSubclass(A.class, I.class));
  }
}
