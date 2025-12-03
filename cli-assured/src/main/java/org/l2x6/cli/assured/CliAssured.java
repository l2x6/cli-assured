/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

/**
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CliAssured {
    /**
     * @return a plain {@link Command.Builder}
     * @since  0.0.1
     */
    public static Command.Builder given() {
        return Command.builder();
    }

    /**
     * @return a {@link Command.Builder} with the java exectuable of the current JVM set as
     *         {@link Command.Builder#executable(String)}
     * @since  0.0.1
     */
    public static Command.Builder java() {
        return Command.builder().java();
    }

    /**
     * @return a {@link Command.Builder} with the specified command set
     * @since  0.0.1
     */
    public static Command.Builder command(String executable, String... args) {
        return Command.builder().command(executable, args);
    }
}
