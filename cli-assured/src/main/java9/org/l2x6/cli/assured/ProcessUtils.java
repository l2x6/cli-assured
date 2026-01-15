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
        return process.pid();
    }

    @ExcludeFromJacocoGeneratedReport
    static void kill(Process process, boolean forcibly, boolean withDescendants) {
        if (process.isAlive()) {
            if (forcibly) {
                if (withDescendants) {
                    process.toHandle().descendants().forEach(child -> {
                        try {
                            child.destroyForcibly();
                        } catch (Exception e) {
                            LoggerFactory
                                .getLogger(ProcessUtils.class)
                                .warn("Could not forcibly kill descendant process " + child.pid() + " of process "+ process.pid());
                        }
                    });
                }
                process.destroyForcibly();
            } else {
                if (withDescendants) {
                    process.toHandle().descendants().forEach(child -> {
                        try {
                            child.destroy();
                        } catch (Exception e) {
                            LoggerFactory
                                .getLogger(ProcessUtils.class)
                                .warn("Could not kill descendant process " + child.pid() + " of process "+ process.pid());
                        }
                    });
                }
                process.destroy();
            }
        }
    }

    @ExcludeFromJacocoGeneratedReport
    static LongStream children(Process process) {
        return process.children().mapToLong(ProcessHandle::pid);
    }

    @ExcludeFromJacocoGeneratedReport
    static LongStream descendants(Process process) {
        return process.descendants().mapToLong(ProcessHandle::pid);
    }
}
