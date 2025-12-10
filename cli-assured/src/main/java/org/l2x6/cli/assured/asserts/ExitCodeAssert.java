/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An assertion on an exit code of a process.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ExitCodeAssert implements Assert {

    private final int[] sortedCodes;
    private int actualExitCode;

    /**
     * Assert that the process exits with the given {@code expectedExitCode}
     *
     * @param  expectedExitCode the exit code to assert
     * @return                  a new {@link ExitCodeAssert} expecting the given {@code expectedExitCode}
     * @since                   0.0.1
     */
    public static ExitCodeAssert of(int expectedExitCode) {
        return new ExitCodeAssert(new int[] { expectedExitCode });
    }

    /**
     * Assert that the process exits with any of the given {@code expectedExitCodes}
     * )
     *
     * @param  expectedExitCodes the exit codes to assert
     * @return                   a new {@link ExitCodeAssert} expecting any of the given {@code expectedExitCodes}
     * @since                    0.0.1
     */
    public static ExitCodeAssert any(int... expectedExitCodes) {
        final int[] sortedCodes = new int[expectedExitCodes.length];
        System.arraycopy(expectedExitCodes, 0, sortedCodes, 0, expectedExitCodes.length);
        Arrays.sort(sortedCodes);
        return new ExitCodeAssert(sortedCodes);
    }

    private ExitCodeAssert(int[] sortedCodes) {
        this.sortedCodes = sortedCodes;
    }

    /**
     * Assert that the actual exit code fulfills the expectations.
     *
     * @param actualExitCode the actual exit code of a process
     * @since                0.0.1
     */
    public ExitCodeAssert exitCode(int actualExitCode) {
        this.actualExitCode = actualExitCode;
        return this;
    }

    @Override
    public void assertSatisfied() {
        if (sortedCodes.length == 1) {
            if (sortedCodes[0] != actualExitCode) {
                throw new AssertionError("Expected exit code " + sortedCodes[0] + " but was " + actualExitCode);
            }
        } else if (Arrays.binarySearch(sortedCodes, actualExitCode) < 0) {
            String codes = IntStream.of(sortedCodes).mapToObj(String::valueOf).collect(Collectors.joining(", "));
            throw new AssertionError("Expected any of exit codes "
                    + codes + " but was "
                    + actualExitCode);
        }
    }

}
