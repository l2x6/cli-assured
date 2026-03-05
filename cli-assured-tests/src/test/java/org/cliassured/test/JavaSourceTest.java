/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cliassured.test;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.assertj.core.api.Assertions;
import org.cliassured.CliAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

public class JavaSourceTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_9, JRE.JAVA_10 })
    void helloJava() {

        final Path helloJava = Paths.get("src/test/java/" + Hello.class.getName().replace('.', '/') + ".java");
        Assertions.assertThat(helloJava).isRegularFile();

        CliAssured
                .java()
                .args(helloJava.toString())
                .args("Joe")
                .then()
                .stdout()
                .hasLines("Hello Joe")
                .hasLineCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        CliAssured.given()
                .java()
                .args(helloJava.toString())
                .args("Joe")
                .then()
                .stdout()
                .hasLines("Hello Joe")
                .hasLineCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

    }
}
