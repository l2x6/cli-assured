/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;
import org.l2x6.cli.assured.asserts.ExitCodeAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assertions applicable to an output of a {@link CommandProcess} or its exit code.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Expectations {
    static final Logger log = LoggerFactory.getLogger(Expectations.class);
    final Function<InputStream, OutputConsumer> stdout;
    final Function<InputStream, OutputConsumer> stderr;
    final ExitCodeAssert exitCodeAssert;

    Expectations(
            Function<InputStream, OutputConsumer> stdout,
            Function<InputStream, OutputConsumer> stderr,
            ExitCodeAssert exitCodeAssert) {
        this.stdout = Objects.requireNonNull(stdout, "stdout");
        this.stderr = stderr;
        this.exitCodeAssert = Objects.requireNonNull(exitCodeAssert, "exitCodeAssert");
    }
}
