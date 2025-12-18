/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test.asserts;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.asserts.Assert;
import org.l2x6.cli.assured.asserts.ExitCodeAssert;

public class ExitCodeAssertTest {

    @Test
    void exitCodeIs() {

        ExitCodeAssert.exitCodeIs(0).exitCode(0).evaluate(new Assert.FailureCollector("test-command")).assertSatisfied();

        Assertions.assertThatThrownBy(
                ExitCodeAssert.exitCodeIs(0).exitCode(1).evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected exit code 0 but actually terminated with exit code 1");
    }

    @Test
    void exitCodeSatisfies() {

        ExitCodeAssert.exitCodeSatisfies(i -> i == 42, "Expected 42 but got ${actual}").exitCode(42)
                .evaluate(new Assert.FailureCollector("test-command")).assertSatisfied();

        Assertions.assertThatThrownBy(
                ExitCodeAssert.exitCodeSatisfies(i -> i == 42, "Expected 42 but got ${actual}").exitCode(1)
                        .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected 42 but got 1");
    }

    @Test
    void exitCodeIsAnyOf() {

        int[] codes = { 0, 1, 2 };
        for (int i : codes) {
            ExitCodeAssert.exitCodeIsAnyOf(codes)
                    .exitCode(i)
                    .evaluate(new Assert.FailureCollector("test-command")).assertSatisfied();

            ExitCodeAssert.exitCodeIsAnyOf(i)
                    .exitCode(i)
                    .evaluate(new Assert.FailureCollector("test-command")).assertSatisfied();

        }

        Assertions.assertThatThrownBy(
                ExitCodeAssert.exitCodeIsAnyOf(codes).exitCode(4)
                        .evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Expected any of exit codes 0, 1, 2 but actually terminated with exit code 4");

    }

    @Test
    void exitCodeIsAnyOfEmpty() {

        Assertions.assertThatThrownBy(
                ExitCodeAssert::exitCodeIsAnyOf)
                .isInstanceOf(IllegalArgumentException.class)
                .message()
                .isEqualTo(
                        "Pass at least one expected exit code to org.l2x6.cli.assured.asserts.ExitCodeAssert.exitCodeIsAnyOf(int...)");

    }

}
