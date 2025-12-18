/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.l2x6.cli.assured.asserts.Assert.Internal.ExcludeFromJacocoGeneratedReport;

/**
 * An assertion on an exit code of a process.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ExitCodeAssert implements Assert {

    private final IntPredicate expected;
    private final String description;
    private int actualExitCode = -1;

    /**
     * Assert that the process exits with the given {@code expectedExitCode}
     *
     * @param  expectedExitCode the exit code to assert
     * @return                  a new {@link ExitCodeAssert} expecting the given {@code expectedExitCode}
     * @since                   0.0.1
     */
    public static ExitCodeAssert exitCodeIs(int expectedExitCode) {
        return new ExitCodeAssert(
                actualExitCode -> actualExitCode == expectedExitCode,
                "Expected exit code " + expectedExitCode + " but actually terminated with exit code ${actual}");
    }

    /**
     * Assert that the process exits with any of the given {@code expectedExitCodes}
     *
     * @param  expectedExitCodes the exit codes to assert
     * @return                   a new {@link ExitCodeAssert} expecting any of the given {@code expectedExitCodes}
     * @since                    0.0.1
     */
    public static ExitCodeAssert exitCodeIsAnyOf(int... expectedExitCodes) {
        switch (expectedExitCodes.length) {
        case 0: {
            throw new IllegalArgumentException(
                    "Pass at least one expected exit code to " + ExitCodeAssert.class.getName() + ".exitCodeIsAnyOf(int...)");
        }
        case 1: {
            return exitCodeIs(expectedExitCodes[0]);
        }
        default:
            final int[] sortedCodes = new int[expectedExitCodes.length];
            System.arraycopy(expectedExitCodes, 0, sortedCodes, 0, expectedExitCodes.length);
            Arrays.sort(sortedCodes);
            final String codes = IntStream.of(sortedCodes).mapToObj(String::valueOf).collect(Collectors.joining(", "));
            return new ExitCodeAssert(
                    actualExitCode -> Arrays.binarySearch(sortedCodes, actualExitCode) >= 0,
                    "Expected any of exit codes " + codes + " but actually terminated with exit code ${actual}");
        }
    }

    /**
     * Assert that the process exits with an exit code that satisfies the given {@link IntPredicate}.
     *
     * @param  expected    the condition actual exit code must satisfy
     * @param  description the description of a failure typically something like
     *                     {@code "Expected exit code <condition> but actually terminated with exit code ${actual}"} where
     *                     {@code <condition>} is your human readable criteria, like {@code greater that 42},
     *                     and <code>${actual}</code> is a placeholder that CLI Assured will replace
     *                     by the actual exit code of the process
     * @return             a new {@link ExitCodeAssert} expecting the given {@code expectedExitCode}
     * @since              0.0.1
     */
    public static ExitCodeAssert exitCodeSatisfies(IntPredicate expected, String description) {
        return new ExitCodeAssert(expected, description);
    }

    private ExitCodeAssert(IntPredicate expected, String description) {
        this.expected = Objects.requireNonNull(expected, "expected");
        this.description = Objects.requireNonNull(description, "description");
    }

    /**
     * Record the actual exit code.
     * Any assertion failures should be reported via {@link #evaluate()} rather than by throwing an exception from this
     * method.
     *
     * @param actualExitCode the actual exit code of a process
     * @since                0.0.1
     */
    public ExitCodeAssert exitCode(int actualExitCode) {
        this.actualExitCode = actualExitCode;
        return this;
    }

    @Override
    public FailureCollector evaluate(FailureCollector failureCollector) {
        if (!expected.test(actualExitCode)) {
            failureCollector.failure(Assert.Internal.formatMessage(description, this::eval));
        }
        return failureCollector;
    }

    @ExcludeFromJacocoGeneratedReport
    String eval(String key) {
        switch (key) {
        case "actual":
            return String.valueOf(actualExitCode);
        default:
            throw new IllegalArgumentException("Unexpected placeholder '" + key + "' in " + ByteCountAssert.class.getName()
                    + " description '" + description + "'.");
        }
    }
}
