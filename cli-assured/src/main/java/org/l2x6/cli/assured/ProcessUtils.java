/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import org.l2x6.cli.assured.CliAssertUtils.ExcludeFromJacocoGeneratedReport;
import org.slf4j.LoggerFactory;

class ProcessUtils {

    @ExcludeFromJacocoGeneratedReport
    private ProcessUtils() {
    }

    @ExcludeFromJacocoGeneratedReport
    static long getPid(Process process) {
        return -1;
    }

    @ExcludeFromJacocoGeneratedReport
    static void kill(Process process, boolean forcibly) {
        if (process.isAlive()) {
            if (forcibly) {
                process.destroyForcibly();
                LoggerFactory
                        .getLogger(ProcessUtils.class)
                        .warn(
                                "Killing a process forcibly on Java 8 or older will not kill its descendant processes. Use Java 9+ to ensure that also descendant processes are killed");
            } else {
                process.destroy();
            }
        }
    }

}
