/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.l2x6.cli.assured.CliAssertUtils.ExcludeFromJacocoGeneratedReport;

/**
 * Entry methods for building command assertions.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CliAssured {

    @ExcludeFromJacocoGeneratedReport
    private CliAssured() {
    }

    /**
     * @return a plain {@link CommandSpec}
     * @since  0.0.1
     */
    public static CommandSpec given() {
        return new CommandSpec(null, Collections.emptyList());
    }

    /**
     * @return a {@link CommandSpec} with the java exectuable of the current JVM set as
     *         {@link CommandSpec#executable(String)}
     * @since  0.0.1
     */
    public static CommandSpec java() {
        return new CommandSpec(CommandSpec::javaExecutable, Collections.emptyList());
    }

    /**
     * @param  executable the executable to set on the returned {@link CommandSpec}
     * @param  args       the arguments to set on the returned {@link CommandSpec}
     * @return            a {@link CommandSpec} with the specified {@code executable} and {@code args} set
     * @since             0.0.1
     */
    public static CommandSpec command(String executable, String... args) {
        return new CommandSpec(() -> executable, Collections.unmodifiableList(new ArrayList<>(Arrays.asList(args))));
    }
}
