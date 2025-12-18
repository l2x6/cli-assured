/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

public class CancellationException extends RuntimeException {

    private static final long serialVersionUID = -9198506143776251186L;

    CancellationException(String message) {
        super(message);
    }

}
