/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.CliAssured;
import org.l2x6.cli.assured.GlobalThreadPoolSpec;
import org.l2x6.cli.assured.LocalThreadPoolSpec;
import org.l2x6.cli.assured.test.app.TestApp;

public class ThreadPoolTest {

    @Test
    void localThreadPool() {
        local()
                .then()
                .stdout()
                .hasLines("Hello Joe")
                .hasLines(Collections.singleton("Hello Joe"))
                .start()
                .awaitTermination()
                .assertSuccess();
        local()
                .execute()
                .assertSuccess();

        local()
                .execute(10000)
                .assertSuccess();

        local()
                .execute(Duration.ofSeconds(10))
                .assertSuccess();

        local()
                .start()
                .awaitTermination()
                .assertSuccess();

    }

    @Test
    void globalThreadPool() {
        Path testAppJar = JavaTest.testAppJar();

        global()
                .java()
                .args("-cp", testAppJar.toString(), TestApp.class.getName(), "hello", "Joe")
                .then()
                .stdout()
                .hasLines("Hello Joe")
                .hasLines(Collections.singleton("Hello Joe"))
                .execute()
                .assertSuccess();
        global()
                .command(javaExecutable(), "-cp", testAppJar.toString(), TestApp.class.getName(), "hello", "Joe")
                .then()
                .stdout()
                .hasLines("Hello Joe")
                .hasLines(Collections.singleton("Hello Joe"))
                .execute()
                .assertSuccess();

        /* Cannot edit after it was used */

        Assertions.assertThatThrownBy(() -> CliAssured.globalThreadPool()
                .coreSize(2)).isInstanceOf(IllegalStateException.class);
        Assertions.assertThatThrownBy(() -> CliAssured.globalThreadPool()
                .maxSize(2)).isInstanceOf(IllegalStateException.class);

        Assertions.assertThatThrownBy(() -> CliAssured.globalThreadPool()
                .keepAlive(Duration.ofSeconds(20))).isInstanceOf(IllegalStateException.class);

    }

    public LocalThreadPoolSpec local() {
        return JavaTest.command("hello", "Joe")
                .threadPool()
                .coreSize(2)
                .maxSize(3)
                .keepAlive(Duration.ofSeconds(20));
    }

    public GlobalThreadPoolSpec global() {
        return CliAssured.globalThreadPool()
                .coreSize(0)
                .maxSize(Integer.MAX_VALUE)
                .keepAlive(Duration.ofSeconds(60));
    }

    static String javaExecutable() {
        final Path javaHome = Paths.get(System.getProperty("java.home"));
        Path java = javaHome.resolve("bin/java");
        final String exec;
        if (Files.isRegularFile(java)) {
            exec = java.toString();
        } else if (Files.isRegularFile(java = javaHome.resolve("bin/java.exe"))) {
            exec = java.toString();
        } else {
            throw new IllegalStateException("Could not locate java or java.exe in " + javaHome.resolve("bin"));
        }
        return exec;
    }

}
