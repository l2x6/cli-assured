/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import org.assertj.core.api.Assertions;
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
                .log()
                .exitCodeIsAnyOf(0)
                .start()
                .awaitTermination()
                .assertSuccess();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void charset() {
        CliAssured.command("cat", "src/test/resources/iso-8859-2.txt")
                .then()
                .stdout()
                .charset(Charset.forName("iso-8859-2"))
                .hasLines("Poslušně hlásím, že jsem zase tady.")
                .hasLineCount(1)
                .log()
                .exitCodeIsAnyOf(0)
                .start()
                .awaitTermination()
                .assertSuccess();
    }

    @Test
    void executableArg() {
        CliAssured.given()
                .executable("echo")
                .arg("CLI Assured rocks!")
                .then()
                .stdout()
                .hasLines("CLI Assured rocks!")
                .hasLineCount(1)
                .exitCodeIs(0)
                .start()
                .awaitTermination()
                .assertSuccess();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void args() {
        CliAssured.command("echo")
                .args(Arrays.asList("CLI", "Assured", "rocks!"))
                .then()
                .stdout()
                .hasLines("CLI Assured rocks!")
                .hasLineCount(1)
                .exitCodeIsAnyOf(0)
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
                    .exitCodeIsAnyOf(0)
                .execute()
                .assertSuccess();
        // @formatter:on
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void cat() {
        // @formatter:off
        CliAssured
                .given()
                    .stdin("Hello world!")
                .when()
                    .command("cat")
                .then()
                    .stdout()
                        .hasLines("Hello world!")
                        .hasLineCount(1)
                    .exitCodeIsAnyOf(0)
                .execute()
                .assertSuccess();
        // @formatter:on
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void env() {
        CliAssured
                .given()
                .env(Collections.singletonMap("MESSAGE", "CLI Assured rocks!"))
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
                .exitCodeIsAnyOf(0)
                .execute()
                .assertSuccess();

        CliAssured
                .given()
                .env("MESSAGE", "CLI Assured rocks!", "ERR_MESSAGE", "Really!")
                .when()
                .command("sh", "-c",
                        "echo $MESSAGE;"
                                + "echo $ERR_MESSAGE 1>&2")
                .then()
                .stdout()
                .hasLines("CLI Assured rocks!")
                .hasLineCount(1)
                .stderr()
                .hasLines("Really!")
                .hasLineCount(1)
                .exitCodeIsAnyOf(0)
                .execute()
                .assertSuccess();

        Assertions.assertThatThrownBy(() -> CliAssured
                .given()
                .env("MESSAGE", "CLI Assured rocks!", "FOO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("env(String[]) accepts only even number of arguments");
    }
}
