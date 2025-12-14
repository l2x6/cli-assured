/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.l2x6.cli.assured.CliAssured;

public class EchoTest {

    @Test
    void echo() {
        CliAssured.command("echo", "CLI Assured rocks!")
                .then()
                .stdout()
                .hasLines("CLI Assured rocks!")
                .hasLineCount(1)
                .exitCode(0)
                .start()
                .awaitTermination()
                .assertSuccess();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void echoGwd() {
        // @formatter:off
        CliAssured
                .given()
                    .env("MESSAGE", "CLI Assured rocks!")
                .when()
                    .command("sh", "-c",
                                "echo $MESSAGE;"
                              + "echo Really! 1>&2")
                .then()
                    .stdout()
                        .hasLines("CLI Assured rocks!")
                        .hasLineCount(1)
                    .stderr()
                        .hasLines("Really!")
                        .hasLineCount(1)
                    .exitCode(0)
                .execute()
                .assertSuccess();
        // @formatter:on
    }

}
