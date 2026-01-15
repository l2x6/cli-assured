/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.util.stream.LongStream;
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
    static void kill(Process process, boolean forcibly, boolean withDescendants) {
        if (process.isAlive()) {
            if (forcibly) {
                process.destroyForcibly();
            } else {
                process.destroy();
            }
            if (withDescendants) {
                LoggerFactory
                        .getLogger(ProcessUtils.class)
                        .warn(
                                "Killing a process on Java 8 or older will not kill its descendant processes. Use Java 9+ to ensure that also descendant processes are killed");
            }
        }
    }

    @ExcludeFromJacocoGeneratedReport
    static LongStream children(Process process) {
        throw new UnsupportedOperationException(
                Process.class.getName() + ".children() is not supported before Java version 9; current Java version: "
                        + System.getProperty("java.version"));
    }

    @ExcludeFromJacocoGeneratedReport
    static LongStream descendants(Process process) {
        throw new UnsupportedOperationException(
                Process.class.getName() + ".descendants() is not supported before Java version 9; current Java version: "
                        + System.getProperty("java.version"));
    }

}
