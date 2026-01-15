/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.l2x6.cli.assured.CliAssured;
import org.l2x6.cli.assured.CommandProcess;
import org.l2x6.cli.assured.CommandResult;

public class ProcessTest {
    static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    @Test
    void killForcibly() {
        assertKill(true, 143);
    }

    @Test
    void killGently() {
        assertKill(false, 137);
    }

    @Test
    @DisabledOnJre(value = JRE.JAVA_8) // Process.pid() exists since Java 9
    void close() {
        List<String> lines = Collections.synchronizedList(new ArrayList<>());
        long pid;
        try (CommandProcess proc = JavaTest.run("sleep", "1000")
                .log(lines::add)
                .exitCodeIsAnyOf(1, 137) // Windows, Linux
                .start()) {
            pid = proc.pid();
            Awaitility.waitAtMost(10, TimeUnit.SECONDS)
                    .until(() -> lines.size() == 1 && lines.contains("About to sleep for 1000 ms"));

            /* Make sure we can find the PID using some OS specific scripting */
            assertProcessExistence(pid, true);
        }

        /* Make sure we cannot find the PID using some OS specific scripting anymore */
        assertProcessExistence(pid, false);

    }

    @Test
    @DisabledOnJre(value = JRE.JAVA_8) // Process.pid() exists since Java 9
    void closeForcibly() {
        List<String> lines = Collections.synchronizedList(new ArrayList<>());
        long pid;
        try (CommandProcess proc = JavaTest.command("sleep", "1000")
                .autoCloseForcibly()
                .autoCloseTimeout(Duration.ofSeconds(10))
                .then()
                .stdout()
                .log(lines::add)
                .exitCodeIsAnyOf(1, 137) // Windows, Linux
                .start()) {
            pid = proc.pid();
            Awaitility.waitAtMost(10, TimeUnit.SECONDS)
                    .until(() -> lines.size() == 1 && lines.contains("About to sleep for 1000 ms"));

            /* Make sure we can find the PID using some OS specific scripting */
            assertProcessExistence(pid, true);
        }

        /* Make sure we cannot find the PID using some OS specific scripting anymore */
        assertProcessExistence(pid, false);

    }

    @Test
    @EnabledOnJre(value = JRE.JAVA_8)
    void pidJava8() {
        try (CommandProcess proc = JavaTest.run("sleep", "1000")
                .exitCodeIsAnyOf(1, 137) // Windows, Linux
                .start()) {
            Assertions
                    .assertThatThrownBy(proc::pid)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageStartingWith(
                            "java.lang.Process.pid() is not supported before Java version 9; current Java version: 1.8");
        }
    }

    static void assertProcessExistence(long pid, boolean expectedToExist) {
        (isWindows
                ? CliAssured.command(
                        "pwsh.exe",
                        "-Command", "[bool](Get-Process -Id " + pid + " -ErrorAction SilentlyContinue)")
                : CliAssured.command(
                        "bash",
                        "-c",
                        "kill -0 " + pid + " 2>/dev/null && echo true || echo false"))
                .then()
                .stdout().hasLinesMatching(Pattern.compile("^" + expectedToExist + "$", Pattern.CASE_INSENSITIVE))
                .execute()
                .assertSuccess();
    }

    static void assertKill(boolean forcibly, int exitCodeLinux) {
        List<String> lines = Collections.synchronizedList(new ArrayList<>());
        CommandProcess proc = JavaTest.run("sleep", "500")
                .log(lines::add)
                .exitCodeIsAnyOf(1, exitCodeLinux) // Windows, Linux
                .start();

        Awaitility.waitAtMost(10, TimeUnit.SECONDS)
                .until(() -> lines.size() == 1 && lines.contains("About to sleep for 500 ms"));
        proc.kill(forcibly);
        proc.kill(forcibly);

        proc.awaitTermination().assertSuccess();
    }

    @Test
    void timeout() {

        CommandResult result = JavaTest.run("sleep", "500")
                .hasLines("About to sleep for 500 ms")
                .hasLineCount(1)
                .start()
                .awaitTermination(200)
                .assertTimeout();

        Assertions.assertThat(result.duration()).isGreaterThan(Duration.ofMillis(200));

        Assertions
                .assertThatThrownBy(
                        JavaTest.command("hello", "Joe")
                                .then()
                                .stderr()
                                .hasLines("Hello Joe")
                                .start()
                                .awaitTermination()::assertTimeout)
                .isInstanceOf(AssertionError.class)
                .message().matches(Pattern.compile("Expected a timeout when running\n"
                        + "\n"
                        + "    [^\n\r]+\n"
                        + "\n"
                        + "but it terminated in [\\S]+ with exit code 0", Pattern.DOTALL));

        Assertions
                .assertThatThrownBy(
                        JavaTest.run("sleep", "500")
                                .hasLines("About to sleep for 500 ms")
                                .hasLineCount(1)
                                .exitCodeIsAnyOf(-1)
                                .execute(200)::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().matches(Pattern.compile("1 exceptions occurred while executing\n"
                        + "\n"
                        + "    [^\n\r]+\n"
                        + "\n"
                        + "Exception 1/1: org.l2x6.cli.assured.TimeoutAssertionError: Command has not terminated within 200 ms\n",
                        Pattern.DOTALL));

    }
}
