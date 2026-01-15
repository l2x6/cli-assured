/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import org.l2x6.cli.assured.CliAssertUtils.ExcludeFromJacocoGeneratedReport;

class PidLookup {

    @ExcludeFromJacocoGeneratedReport
    private PidLookup() {
    }

    @ExcludeFromJacocoGeneratedReport
    public static long getPid(Process process) {
        return process.pid();
    }

}
