/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import org.l2x6.cli.assured.CliAssertUtils.ExcludeFromJacocoGeneratedReport;

/**
 * Entry methods for building command assertions.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CliAssured {

    private static final GlobalThreadPoolSpec globalThreadPool = new GlobalThreadPoolSpec();

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

    /**
     * Return the global thread pool specification.
     * Unlike the rest of CLI Assured API, the returned instance of {@link GlobalThreadPoolSpec} is mutable.
     * It must be configured before {@link GlobalThreadPoolSpec#getOrCreateExecutorService()} is called for the first time.
     * Otherwise an {@link IllegalStateException} is thrown.
     * <p>
     * The {@link ExecutorService} instance returned by {@link GlobalThreadPoolSpec#getOrCreateExecutorService()}
     * will be used for consuming {@code stdout} and {@code stderr} and producing {@code stdin} of all commands that
     * do not have a local thread pool configured via {@link CommandSpec#threadPool()} or
     * {@link CommandSpec#threadPool(Supplier)}.
     * <p>
     * The default thread pool is created lazily just before its first use and it is never shut down by CLI Assured.
     *
     * @return the global thread pool specification
     * @since  0.0.1
     */
    public static GlobalThreadPoolSpec globalThreadPool() {
        return globalThreadPool;
    }
}
