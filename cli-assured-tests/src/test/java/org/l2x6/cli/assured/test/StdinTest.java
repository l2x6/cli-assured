/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.CommandProcess;
import org.l2x6.cli.assured.CommandResult;
import org.l2x6.cli.assured.CommandSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StdinTest {

    private static final Logger log = LoggerFactory.getLogger(StdinTest.class);

    @Test
    void stdinString() {
        JavaTest.command("stdin")
                .stdin("CLI Assured rocks!")
                .then()
                .stdout()
                .hasLines("CLI Assured rocks!")
                .hasLineCount(1)
                .execute()
                .assertSuccess();
    }

    @Test
    void stdinMultiple() {
        final CommandSpec base = JavaTest.command("stdin")
                .stdin("CLI Assured rocks!");
        Assertions.assertThatThrownBy(() -> base.stdin("Foo"))
                .isInstanceOf(IllegalStateException.class)
                .message().contains(
                        "stdin was already defined for this org.l2x6.cli.assured.CommandSpec. You may want to keep ony one stdin(...) call for the given CommandSpec chain");

        Assertions.assertThatThrownBy(() -> base.stdin(Paths.get("src/test/resources/utf-8.txt").toAbsolutePath().normalize()))
                .isInstanceOf(IllegalStateException.class)
                .message().contains(
                        "stdin was already defined for this org.l2x6.cli.assured.CommandSpec. You may want to keep ony one stdin(...) call for the given CommandSpec chain");

        Assertions.assertThatThrownBy(() -> base.stdin(out -> {
        }))
                .isInstanceOf(IllegalStateException.class)
                .message().contains(
                        "stdin was already defined for this org.l2x6.cli.assured.CommandSpec. You may want to keep ony one stdin(...) call for the given CommandSpec chain");
    }

    @Test
    void stdinFile() {
        JavaTest.command("stdin")
                .stdin(Paths.get("src/test/resources/utf-8.txt").toAbsolutePath().normalize())
                .then()
                .stdout()
                .hasLines(
                        "Muchos años después, frente al pelotón de fusilamiento,",
                        "el coronel Aureliano Buendía había de recordar aquella",
                        "tarde remota en que su padre le llevó a conocer el hielo.")
                .hasLineCount(3)
                .execute()
                .assertSuccess();
    }

    @Test
    void stdinStream() {
        final byte[] bytes = new byte[8192];
        Random random = new Random();
        random.nextBytes(bytes);

        JavaTest.command("stdin")
                .stdin(out -> {
                    try {
                        for (int i = 0; i < 1024; i++) {
                            out.write(bytes);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }

                })
                .then()
                .stdout()
                .hasByteCount(1024 * 8192)
                .execute()
                .assertSuccess();
    }

    @Test
    void stdinCancel() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        final BlockingQueue<String> pipe = new LinkedBlockingQueue<>();
        final List<String> lines = Collections.synchronizedList(new ArrayList<>());

        CommandProcess p = JavaTest.command("stdin")
                .stdin(out -> {
                    try {
                        while (true) {
                            final String line = pipe.take();
                            out.write(line.getBytes(StandardCharsets.UTF_8));
                            out.flush();
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException("IOException when writing to stdout", e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("InterruptedException when writing to stdout", e);
                    }
                })
                .then()
                .stdout()
                .log(lines::add)
                .start();

        pipe.add("line 1\n");

        /* Await the first message on stdout */
        while (lines.isEmpty()) {
            Thread.sleep(50);
        }

        p.kill(true);

        pipe.add("line 2\n");

        CommandResult result = p.awaitTermination();

        Assertions.setMaxStackTraceElementsDisplayed(Integer.MAX_VALUE);

        Assertions.assertThatThrownBy(result::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().contains("Exception 1/1: org.l2x6.cli.assured.CancellationException: The process was cancelled");

        Assertions.assertThat(result.byteCountStdout()).isEqualTo(7);
    }

}
