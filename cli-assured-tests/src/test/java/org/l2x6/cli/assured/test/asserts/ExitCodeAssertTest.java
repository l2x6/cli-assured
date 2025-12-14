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
    void exitCode() {

        ExitCodeAssert.of(0).exitCode(0).evaluate(new Assert.FailureCollector("test-command")).assertSatisfied();

        Assertions.assertThatThrownBy(
                ExitCodeAssert.of(0).exitCode(1).evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected exit code 0 but was 1");

        int[] codes = { 0, 1, 2 };
        for (int i : codes) {
            ExitCodeAssert.any(codes)
                    .exitCode(i)
                    .evaluate(new Assert.FailureCollector("test-command")).assertSatisfied();
        }

        Assertions.assertThatThrownBy(
                ExitCodeAssert.any(codes).exitCode(4).evaluate(new Assert.FailureCollector("test-command"))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Expected any of exit codes 0, 1, 2 but was 4");

    }
}
