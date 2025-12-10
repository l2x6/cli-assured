/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test.asserts;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.asserts.LineAssert;

public class LineAssertTest {

    @Test
    void hasLines() {
        LineAssert
                .hasLines(Arrays.asList("foo", "bar"))
                .line("foo")
                .line("bar")
                .assertSatisfied();

        LineAssert.hasLines(Arrays.asList("foo", "bar"))
                .line("bar")
                .line("foo")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.hasLines(Arrays.asList("foo", "bar"))
                .line("maz")
                .line("baz")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected lines\n\n    foo\n    bar\n\nto occur in any order, but lines\n\n    foo\n    bar\n\ndid not occur");

    }

    @Test
    void doesNotHaveLines() {
        LineAssert
                .doesNotHaveLines(Arrays.asList("baz", "bam"))
                .line("foo")
                .line("bar")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotHaveLines(Arrays.asList("foo"))
                .line("maz")
                .line("foo")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected none of the lines\n\n    foo\n\nto occur, but the following lines occurred:\n\n    foo\n\n");

    }

    @Test
    void hasLinesContaining() {
        LineAssert
                .hasLinesContaining(Arrays.asList("oo", "ba", "fo"))
                .line("foo")
                .line("bar")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.hasLinesContaining(Arrays.asList("foo", "bar"))
                .line("ma")
                .line("az")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected lines containing\n\n    foo\n    bar\n\nto occur, but the following substrings did not occur:\n\n    foo\n    bar\n\n");

    }

    @Test
    void doesNotHaveLinesContaining() {
        LineAssert
                .doesNotHaveLinesContaining(Arrays.asList("baz", "bam"))
                .line("ma")
                .line("az")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotHaveLinesContaining(Arrays.asList("oo"))
                .line("maz")
                .line("foo")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected no lines containing\n\n    oo\n\nto occur, but some of the substrings occur in lines\n\n    foo\n\n");

    }

    @Test
    void hasLinesContainingCaseInsensitive() {
        LineAssert
                .hasLinesContainingCaseInsensitive(Arrays.asList("oo", "ba", "fo"), Locale.US)
                .line("fOO")
                .line("Bar")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.hasLinesContainingCaseInsensitive(Arrays.asList("foo", "bar"), Locale.US)
                .line("ma")
                .line("az")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected lines containing using case insensitive comparison\n\n    foo\n    bar\n\nto occur, but the following substrings did not occur:\n\n    foo\n    bar\n\n");

    }

    @Test
    void doesNotHaveLinesContainingCaseInsensitive() {
        LineAssert
                .doesNotHaveLinesContainingCaseInsensitive(Arrays.asList("baz", "bam"), Locale.US)
                .line("ma")
                .line("az")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotHaveLinesContainingCaseInsensitive(Arrays.asList("oo"), Locale.US)
                .line("maz")
                .line("FOO")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected no lines containing using case insensitive comparison\n\n    oo\n\nto occur, but some of the substrings occur in lines\n\n    foo\n\n");

    }

    @Test
    void hasLinesMatchingPatterns() {
        LineAssert
                .hasLinesMatchingPatterns(Arrays.asList(Pattern.compile("o+", Pattern.CASE_INSENSITIVE)))
                .line("FOO")
                .line("bar")
                .assertSatisfied();

        Assertions
                .assertThatThrownBy(LineAssert
                        .hasLinesMatchingPatterns(Arrays.asList(Pattern.compile("o+", Pattern.CASE_INSENSITIVE),
                                Pattern.compile("b.*", Pattern.CASE_INSENSITIVE)))
                        .line("ma")
                        .line("az")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected lines matching\n\n    o+\n    b.*\n\nto occur, but the following patterns did not match:\n\n    o+\n    b.*\n\n");

    }

    @Test
    void hasLinesMatching() {
        LineAssert
                .hasLinesMatching(Arrays.asList("O+"))
                .line("FOO")
                .line("bar")
                .assertSatisfied();

        Assertions.assertThatThrownBy(
                LineAssert.hasLinesMatching(
                        Arrays.asList("o+", "b.*"))
                        .line("ma")
                        .line("az")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected lines matching\n\n    o+\n    b.*\n\nto occur, but the following patterns did not match:\n\n    o+\n    b.*\n\n");

    }

    @Test
    void doesNotHaveLinesMatchingPatterns() {
        LineAssert
                .doesNotHaveLinesMatchingPatterns(Arrays.asList(Pattern.compile("b.z")))
                .line("ma")
                .line("az")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotHaveLinesMatchingPatterns(Arrays.asList(Pattern.compile("o+")))
                .line("maz")
                .line("foo")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected no lines matching\n\n    o+\n\nto occur, but some of the patterns matched the lines\n\n    foo\n\n");
    }

    @Test
    void doesNotHaveLinesMatching() {
        LineAssert
                .doesNotHaveLinesMatching(Arrays.asList("b.z"))
                .line("ma")
                .line("az")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotHaveLinesMatching(Arrays.asList("o+"))
                .line("maz")
                .line("foo")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected no lines matching\n\n    o+\n\nto occur, but some of the patterns matched the lines\n\n    foo\n\n");
    }

    @Test
    void hasLineCount() {
        LineAssert
                .hasLineCount(2)
                .line("ma")
                .line("az")
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.hasLineCount(3)
                .line("maz")
                .line("foo")::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        "Expected 3 lines but found 2");
    }

}
