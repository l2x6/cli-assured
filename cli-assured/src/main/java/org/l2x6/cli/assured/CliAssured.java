/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import org.l2x6.cli.assured.Command.CommandBuilder;

/**
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CliAssured {
    /**
     * @return a plain {@link Command.CommandBuilder}
     * @since  0.0.1
     */
    public static Command.CommandBuilder given() {
        return new CommandBuilder();
    }

    /**
     * @return a {@link Command.CommandBuilder} with the java exectuable of the current JVM set as
     *         {@link Command.CommandBuilder#executable(String)}
     * @since  0.0.1
     */
    public static Command.CommandBuilder java() {
        return new CommandBuilder().java();
    }

    /**
     * @return a {@link Command.CommandBuilder} with the specified command set
     * @since  0.0.1
     */
    public static Command.CommandBuilder command(String executable, String... args) {
        return new CommandBuilder().command(executable, args);
    }
}
