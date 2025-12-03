/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test;

import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.CliAssured;

public class EchoTest {

    @Test
    void echo() {
        CliAssured.command("echo", "CLI Assured rocks!")
                .execute()
                .awaitTermination()
                .assertSuccess()
                .output()
                .hasLine("CLI Assured rocks!")
                .hasLineCount(1);
    }
}
