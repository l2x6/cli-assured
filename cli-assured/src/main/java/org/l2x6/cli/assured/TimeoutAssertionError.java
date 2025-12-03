/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

/**
 * Thrown when a command times out.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  0.0.1
 */
public class TimeoutAssertionError extends AssertionError {

    private static final long serialVersionUID = 2447109090380093692L;

    TimeoutAssertionError(String detailMessage) {
        super(detailMessage);
    }
}
