/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An assertion on an exit code of a process.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface ExitCodeAssert {

    /**
     * Assert that the actual exit code fulfills the expectations.
     *
     * @param actualExitCode the actual exit code of a process
     * @since                0.0.1
     */
    void assertSatisfied(int actualExitCode);

    /**
     * Assert that the process exits with the given {@code expectedExitCode}
     *
     * @param  expectedExitCode the exit code to assert
     * @return                  a new {@link ExitCodeAssert} expecting the given {@code expectedExitCode}
     * @since                   0.0.1
     */
    static ExitCodeAssert of(int expectedExitCode) {
        return actualExitCode -> {
            if (expectedExitCode != actualExitCode) {
                throw new AssertionError("Expected exit code " + expectedExitCode + " but was " + actualExitCode);
            }
        };
    }

    /**
     * Assert that the process exits with any of the given {@code expectedExitCodes}
     *
     * @param  expectedExitCodes the exit codes to assert
     * @return                   a new {@link ExitCodeAssert} expecting any of the given {@code expectedExitCodes}
     * @since                    0.0.1
     */
    static ExitCodeAssert any(int... expectedExitCodes) {
        final int[] sortedCodes = new int[expectedExitCodes.length];
        System.arraycopy(expectedExitCodes, 0, sortedCodes, 0, expectedExitCodes.length);
        Arrays.sort(sortedCodes);
        return actualExitCode -> {
            if (Arrays.binarySearch(sortedCodes, actualExitCode) >= 0) {
                throw new AssertionError("Expected any of exit codes "
                        + Stream.of(sortedCodes).map(String::valueOf).collect(Collectors.joining(", ")) + " but was "
                        + actualExitCode);
            }
        };
    }

}
