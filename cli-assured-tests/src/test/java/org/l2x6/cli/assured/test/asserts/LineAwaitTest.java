/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test.asserts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.Await;
import org.l2x6.cli.assured.Await.LineAwait;
import org.l2x6.cli.assured.CommandProcess;
import org.l2x6.cli.assured.test.JavaTest;

public class LineAwaitTest {

    @Test
    void lineSuccess() {
        assertAwaitSuccess(Await.line("Sleeped for 100 ms"));
    }

    @Test
    void lineSuccessMap() {
        LineAwait<String> await = Await.line("Sleeped for 100 ms").map(s -> s.toUpperCase(Locale.US));
        try (CommandProcess proc = JavaTest.command("sleep", "100", "5000")
                .then()
                .stdout()
                .await(await)
                .start()) {
            Assertions.assertThat(await.await(Duration.ofSeconds(10))).isEqualTo("SLEEPED FOR 100 MS");
        }
    }

    @Test
    void lineContainingSuccess() {
        assertAwaitSuccess(Await.lineContaining("ed for 100 "));
    }

    @Test
    void lineContainingCaseInsensitiveSuccess() {
        assertAwaitSuccess(Await.lineContainingCaseInsensitive("ed FOR 100 "));
    }

    @Test
    void lineMatchingSuccess() {
        assertAwaitSuccess(Await.lineMatching("ed for 1\\d\\d "));
    }

    @Test
    void lineMatchingPatternSuccess() {
        assertAwaitSuccess(Await.lineMatching(Pattern.compile("ed FOR 1\\d\\d ", Pattern.CASE_INSENSITIVE)));
    }

    @Test
    void lineCountSuccess() {
        assertAwaitSuccess(Await.lineCount(2));
    }

    @Test
    void lineSatifyingSuccess() {
        assertAwaitSuccess(Await.lineSatifying("foo", l -> l.equals("Sleeped for 100 ms")));
    }

    static void assertAwaitSuccess(LineAwait<String> await) {
        try (CommandProcess proc = JavaTest.command("sleep", "100", "5000")
                .then()
                .stdout()
                .await(await)
                .start()) {
            Assertions.assertThat(await.await(Duration.ofSeconds(10))).isEqualTo("Sleeped for 100 ms");
        }
    }

    @Test
    void awaitLineTimeout() {
        assertAwaitTimeout("line 'Sleeped for 123 ms'", Await.line("Sleeped for 123 ms"));
    }

    @Test
    void lineContainingTimeout() {
        assertAwaitTimeout("line containing 'ed for 123 '", Await.lineContaining("ed for 123 "));
    }

    @Test
    void lineContainingCaseInsensitiveTimeout() {
        assertAwaitTimeout("line containing case insensitive 'ed FOR 123 '",
                Await.lineContainingCaseInsensitive("ed FOR 123 "));
    }

    @Test
    void lineMatchingTimeout() {
        assertAwaitTimeout("line matching 'ed for 1\\d5 '", Await.lineMatching("ed for 1\\d5 "));
    }

    @Test
    void lineMatchingPatternTimeout() {
        assertAwaitTimeout("line matching 'ed FOR 1\\d5 '",
                Await.lineMatching(Pattern.compile("ed FOR 1\\d5 ", Pattern.CASE_INSENSITIVE)));
    }

    @Test
    void lineCountTimeout() {
        assertAwaitTimeout("line count 5", Await.lineCount(5));
    }

    @Test
    void lineSatifyingTimeout() {
        assertAwaitTimeout("foo", Await.lineSatifying("foo", l -> false));
    }

    static void assertAwaitTimeout(String description, LineAwait<String> await) {
        try (CommandProcess proc = JavaTest.command("sleep", "100")
                .then()
                .stdout()
                .await(await)
                .start()) {

            Assertions.assertThatThrownBy(() -> await.await(Duration.ofMillis(200)))
                    .hasMessage("Awaiting " + description + " has not finished within 200 ms");
        }
    }

    @Test
    void badPredicate() {
        LineAwait<String> await = Await.lineSatifying("bar", new Predicate<String>() {
            public boolean test(String t) {
                throw new RuntimeException("foo");
            }
        });
        try (CommandProcess proc = JavaTest.command("sleep", "100")
                .then()
                .stdout()
                .await(await)
                .start()) {
            Assertions.assertThatThrownBy(() -> await.await(Duration.ofSeconds(2)))
                    .hasMessage("Exception thrown when awaiting bar").rootCause().hasMessage("foo");

        }
    }

    @Test
    void awaitPort() {
        final LineAwait<Integer> awaitPort = Await
                .lineMatching("\\Qhello-server listening on port: \\E(\\d+)")
                .map(Integer::parseInt);
        try (
        // @formatter:off
                CommandProcess proc = JavaTest.command("hello-server")
                    .then()
                        .stdout()
                        .await(awaitPort)
                            .log()
                        .stderr()
                            .log()
                    .start()) {
        // @formatter:on

            final int port = awaitPort.await(Duration.ofSeconds(10));
            final String uuid = UUID.randomUUID().toString();
            /* Try to connect */
            try (
                    Socket socket = new Socket("localhost", port);
                    OutputStream out = socket.getOutputStream();
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));) {
                out.write((uuid + "\n").getBytes(StandardCharsets.UTF_8));
                String response = in.readLine();
                Assertions.assertThat(response).isEqualTo("Hello " + uuid);
            } catch (UnknownHostException e) {
                throw new RuntimeException("Could not connect to localhost:" + port, e);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not connect to localhost:" + port, e);
            }
        }
    }
}
