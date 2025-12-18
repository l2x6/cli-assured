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
import org.l2x6.cli.assured.StreamExpectationsSpec.ProcessOutput;
import org.l2x6.cli.assured.asserts.Assert;
import org.l2x6.cli.assured.asserts.LineAssert;

public class LineAssertTest {

    @Test
    void hasLines() {
        LineAssert
                .hasLines(ProcessOutput.stdout, Arrays.asList("foo", "bar"))
                .line("foo")
                .line("bar")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        LineAssert.hasLines(ProcessOutput.stdout, Arrays.asList("foo", "bar"))
                .line("bar")
                .line("foo")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.hasLines(ProcessOutput.stdout, Arrays.asList("foo", "bar"))
                .line("maz")
                .line("baz")
                .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected lines\n\n    foo\n    bar\n\nto occur in stdout in any order, but lines\n\n    foo\n    bar\n\ndid not occur");

    }

    @Test
    void doesNotHaveLines() {
        LineAssert
                .doesNotHaveLines(ProcessOutput.stdout, Arrays.asList("baz", "bam"))
                .line("foo")
                .line("bar")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotHaveLines(ProcessOutput.stdout, Arrays.asList("foo"))
                .line("maz")
                .line("foo")
                .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected none of the lines\n\n    foo\n\nto occur in stdout, but the following lines occurred:\n\n    foo\n\n");

    }

    @Test
    void hasLinesContaining() {
        LineAssert
                .hasLinesContaining(ProcessOutput.stdout, Arrays.asList("oo", "ba", "fo"))
                .line("foo")
                .line("bar")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.hasLinesContaining(ProcessOutput.stdout, Arrays.asList("foo", "bar"))
                .line("ma")
                .line("az")
                .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected lines containing\n\n    foo\n    bar\n\nto occur in stdout, but the following substrings did not occur:\n\n    foo\n    bar\n\n");

    }

    @Test
    void doesNotHaveLinesContaining() {
        LineAssert
                .doesNotHaveLinesContaining(ProcessOutput.stdout, Arrays.asList("baz", "bam"))
                .line("ma")
                .line("az")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotHaveLinesContaining(ProcessOutput.stdout, Arrays.asList("oo"))
                .line("maz")
                .line("foo")
                .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected no lines containing\n\n    oo\n\nto occur in stdout, but some of the substrings occur in lines\n\n    foo\n\n");

    }

    @Test
    void doesNotHaveAnyLines() {
        LineAssert
                .doesNotHaveAnyLines(ProcessOutput.stderr)
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotHaveAnyLines(ProcessOutput.stderr)
                .line("maz")
                .line("foo")
                .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected no content to occur in stderr, but the following occurred:\n\n    maz\n    foo\n\n");

    }

    @Test
    void hasLinesContainingCaseInsensitive() {
        LineAssert
                .hasLinesContainingCaseInsensitive(ProcessOutput.stdout, Arrays.asList("oo", "ba", "fo"), Locale.US)
                .line("fOO")
                .line("Bar")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions
                .assertThatThrownBy(LineAssert
                        .hasLinesContainingCaseInsensitive(ProcessOutput.stdout, Arrays.asList("foo", "bar"), Locale.US)
                        .line("ma")
                        .line("az")
                        .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected lines containing using case insensitive comparison\n\n    foo\n    bar\n\nto occur in stdout, but the following substrings did not occur:\n\n    foo\n    bar\n\n");

    }

    @Test
    void doesNotHaveLinesContainingCaseInsensitive() {
        LineAssert
                .doesNotHaveLinesContainingCaseInsensitive(ProcessOutput.stdout, Arrays.asList("baz", "bam"), Locale.US)
                .line("ma")
                .line("az")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions
                .assertThatThrownBy(LineAssert
                        .doesNotHaveLinesContainingCaseInsensitive(ProcessOutput.stdout, Arrays.asList("oo"), Locale.US)
                        .line("maz")
                        .line("FOO")
                        .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected no lines containing using case insensitive comparison\n\n    oo\n\nto occur in stdout, but some of the substrings occur in lines\n\n    foo\n\n");

    }

    @Test
    void hasLinesMatchingPatterns() {
        LineAssert
                .hasLinesMatchingPatterns(ProcessOutput.stdout, Arrays.asList(Pattern.compile("o+", Pattern.CASE_INSENSITIVE)))
                .line("FOO")
                .line("bar")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions
                .assertThatThrownBy(LineAssert
                        .hasLinesMatchingPatterns(ProcessOutput.stdout,
                                Arrays.asList(Pattern.compile("o+", Pattern.CASE_INSENSITIVE),
                                        Pattern.compile("b.*", Pattern.CASE_INSENSITIVE)))
                        .line("ma")
                        .line("az")
                        .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected lines matching\n\n    o+\n    b.*\n\nto occur in stdout, but the following patterns did not match:\n\n    o+\n    b.*\n\n");

    }

    @Test
    void hasLinesMatching() {
        LineAssert
                .hasLinesMatching(ProcessOutput.stdout, Arrays.asList("O+"))
                .line("FOO")
                .line("bar")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions.assertThatThrownBy(
                LineAssert.hasLinesMatching(ProcessOutput.stdout,
                        Arrays.asList("o+", "b.*"))
                        .line("ma")
                        .line("az")
                        .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected lines matching\n\n    o+\n    b.*\n\nto occur in stdout, but the following patterns did not match:\n\n    o+\n    b.*\n\n");

    }

    @Test
    void doesNotHaveLinesMatchingPatterns() {
        LineAssert
                .doesNotHaveLinesMatchingPatterns(ProcessOutput.stdout, Arrays.asList(Pattern.compile("b.z")))
                .line("ma")
                .line("az")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions
                .assertThatThrownBy(
                        LineAssert.doesNotHaveLinesMatchingPatterns(ProcessOutput.stdout, Arrays.asList(Pattern.compile("o+")))
                                .line("maz")
                                .line("foo")
                                .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected no lines matching\n\n    o+\n\nto occur in stdout, but some of the patterns matched the lines\n\n    foo\n\n");
    }

    @Test
    void doesNotHaveLinesMatching() {
        LineAssert
                .doesNotHaveLinesMatching(ProcessOutput.stdout, Arrays.asList("b.z"))
                .line("ma")
                .line("az")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.doesNotHaveLinesMatching(ProcessOutput.stdout, Arrays.asList("o+"))
                .line("maz")
                .line("foo")
                .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith(
                        "Failure 1/1: Expected no lines matching\n\n    o+\n\nto occur in stdout, but some of the patterns matched the lines\n\n    foo\n\n");
    }

    @Test
    void hasLineCount() {
        LineAssert
                .hasLineCount(ProcessOutput.stdout, 2)
                .line("ma")
                .line("az")
                .evaluate(new Assert.FailureCollector("test-command"))
                .assertSatisfied();

        Assertions.assertThatThrownBy(LineAssert.hasLineCount(ProcessOutput.stdout, 3)
                .line("maz")
                .line("foo")
                .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected 3 lines in stdout but found 2 lines");
        Assertions
                .assertThatThrownBy(
                        LineAssert
                                .hasLineCount(ProcessOutput.stdout, i -> i < 1,
                                        "Expected number of lines < 1 in ${stream} but found ${actual} lines")
                                .line("maz")
                                .line("foo")
                                .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected number of lines < 1 in stdout but found 2 lines");
    }

}
