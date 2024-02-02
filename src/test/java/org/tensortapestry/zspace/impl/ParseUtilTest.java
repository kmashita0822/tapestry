package org.tensortapestry.zspace.impl;

import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;

public class ParseUtilTest implements ZSpaceTestAssertions {

  @Test
  public void test_splitCommas() {
    assertThat(ParseUtil.splitCommas("")).containsExactly("");
    assertThat(ParseUtil.splitCommas("a, b , c")).containsExactly("a", "b", "c");
    assertThat(ParseUtil.splitCommas("a ")).containsExactly("a");
  }

  @Test
  public void test_splitColons() {
    assertThat(ParseUtil.splitColons("")).containsExactly("");
    assertThat(ParseUtil.splitColons("a: b : c")).containsExactly("a", "b", "c");
    assertThat(ParseUtil.splitColons("a ")).containsExactly("a");
  }
}
