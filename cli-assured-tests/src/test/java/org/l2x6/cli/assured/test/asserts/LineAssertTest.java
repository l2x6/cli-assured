/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test.asserts;

import java.util.Arrays;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.asserts.LineAssert;

public class LineAssertTest {

    @Test
    void contains() {
        LineAssert
                .contains(Arrays.asList("foo", "bar"))
                .line("foo")
                .line("bar")
                .assertSatisfied();

        LineAssert.contains(Arrays.asList("foo", "bar"))
                .line("bar")
                .line("foo")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.contains(Arrays.asList("foo", "bar"))
                .line("maz")
                .line("baz")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected lines\n\n    foo\n    bar\n\nto occur in any order, but lines\n\n    foo\n    bar\n\ndid not occur");

    }

    @Test
    void doesNotContain() {
        LineAssert
                .doesNotContain(Arrays.asList("baz", "bam"))
                .line("foo")
                .line("bar")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotContain(Arrays.asList("foo"))
                .line("maz")
                .line("foo")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected none of the lines\n\n    foo\n\nto occur, but the following lines occurred:\n\n    foo\n\n");

    }

    @Test
    void containsSubstrings() {
        LineAssert
                .containsSubstrings(Arrays.asList("oo", "ba", "fo"))
                .line("foo")
                .line("bar")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.containsSubstrings(Arrays.asList("foo", "bar"))
                .line("ma")
                .line("az")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected lines containing\n\n    foo\n    bar\n\nto occur, but the following substrings did not occur:\n\n    foo\n    bar\n\n");

    }

    @Test
    void doesNotContainSubstrings() {
        LineAssert
                .doesNotContainSubstrings(Arrays.asList("baz", "bam"))
                .line("ma")
                .line("az")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotContainSubstrings(Arrays.asList("oo"))
                .line("maz")
                .line("foo")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected no lines containing\n\n    oo\n\nto occur, but some of the substrings occur in lines\n\n    foo\n\n");

    }

    @Test
    void containsMatchingPatterns() {
        LineAssert
                .containsMatchingPatterns(Arrays.asList(Pattern.compile("o+", Pattern.CASE_INSENSITIVE)))
                .line("FOO")
                .line("bar")
                .assertSatisfied();

        Assertions
                .assertThatThrownBy(LineAssert
                        .containsMatchingPatterns(Arrays.asList(Pattern.compile("o+", Pattern.CASE_INSENSITIVE),
                                Pattern.compile("b.*", Pattern.CASE_INSENSITIVE)))
                        .line("ma")
                        .line("az")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected lines matching\n\n    o+\n    b.*\n\nto occur, but the following patterns did not match:\n\n    o+\n    b.*\n\n");

    }

    @Test
    void containsMatching() {
        LineAssert
                .containsMatching(Arrays.asList("O+"))
                .line("FOO")
                .line("bar")
                .assertSatisfied();

        Assertions.assertThatThrownBy(
                LineAssert.containsMatching(
                        Arrays.asList("o+", "b.*"))
                        .line("ma")
                        .line("az")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected lines matching\n\n    o+\n    b.*\n\nto occur, but the following patterns did not match:\n\n    o+\n    b.*\n\n");

    }

    @Test
    void doesNotContainMatchingPatterns() {
        LineAssert
                .doesNotContainMatchingPatterns(Arrays.asList(Pattern.compile("b.z")))
                .line("ma")
                .line("az")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotContainMatchingPatterns(Arrays.asList(Pattern.compile("o+")))
                .line("maz")
                .line("foo")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected no lines matching\n\n    o+\n\nto occur, but some of the patterns matched the lines\n\n    foo\n\n");
    }

    @Test
    void doesNotContainMatching() {
        LineAssert
                .doesNotContainMatching(Arrays.asList("b.z"))
                .line("ma")
                .line("az")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotContainMatching(Arrays.asList("o+"))
                .line("maz")
                .line("foo")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected no lines matching\n\n    o+\n\nto occur, but some of the patterns matched the lines\n\n    foo\n\n");
    }

    @Test
    void hasCount() {
        LineAssert
                .hasCount(2)
                .line("ma")
                .line("az")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.hasCount(3)
                .line("maz")
                .line("foo")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected 3 lines but found 2");
    }

}
