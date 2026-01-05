/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test.asserts;

import java.io.IOException;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.StreamExpectationsSpec.ProcessOutput;
import org.l2x6.cli.assured.asserts.Assert;
import org.l2x6.cli.assured.asserts.Assert.FailureCollector;
import org.l2x6.cli.assured.asserts.LineAssert;
import org.l2x6.cli.assured.test.JavaTest;

public class AssertTest {

    @Test
    void collector() {
        final ProcessOutput stream = ProcessOutput.stdout;
        Assertions.assertThatThrownBy(
                new Assert.FailureCollector("test-command")
                        .failure(stream, "f1")
                        .failure(stream, "f2")
                        .exception(stream, exception(1))
                        .exception(stream, exception(2))
                        .exception(stream, exception(3))::assertSatisfied)
                .isInstanceOf(AssertionError.class)
                .hasMessage("3 exceptions and 2 assertion failures occurred while executing\n"
                        + "\n"
                        + "    test-command\n"
                        + "\n"
                        + "Exception 1/3: java.lang.RuntimeException: Hello 1\n"
                        + "\tat Hello1.hello1(Hello1.java:1)\n"
                        + "\tat Hello1.hola(Hello1.java:42)\n"
                        + "\n"
                        + "Exception 2/3: java.lang.RuntimeException: Hello 2\n"
                        + "\tat Hello2.hello2(Hello2.java:2)\n"
                        + "\tat Hello2.hola(Hello2.java:42)\n"
                        + "\n"
                        + "Exception 3/3: java.lang.RuntimeException: Hello 3\n"
                        + "\tat Hello3.hello3(Hello3.java:3)\n"
                        + "\tat Hello3.hola(Hello3.java:42)\n"
                        + "\n"
                        + "Failure 1/2: f1\n"
                        + "\n"
                        + "Failure 2/2: f2");
    }

    @Test
    void lineAssert() throws IOException {

        Assertions.assertThatThrownBy(
                JavaTest.command("hello", "Joe")
                        .then()
                        .stdout()
                        .linesSatisfy(new ThrowingLineAssert(ProcessOutput.stdout))
                        .execute()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message()
                .matches("\\Q1 exceptions occurred while executing\n"
                        + "\n"
                        + "    \\E[^\n\r]+\\Q\n"
                        + "\n"
                        + "Exception 1/1: java.lang.RuntimeException: Hello 1\n"
                        + "\tat Hello1.hello1(Hello1.java:1)\n"
                        + "\tat Hello1.hola(Hello1.java:42)\n"
                        + "\n"
                        + "stdout:\n"
                        + "\n"
                        + "    Hello Joe\\E");

        Assertions.assertThatThrownBy(
                JavaTest.command("hello", "Joe")
                        .then()
                        .stdout()
                        .linesSatisfy(Collections.singletonList(new ThrowingLineAssert(ProcessOutput.stdout)))
                        .execute()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message()
                .matches("\\Q1 exceptions occurred while executing\n"
                        + "\n"
                        + "    \\E[^\n\r]+\\Q\n"
                        + "\n"
                        + "Exception 1/1: java.lang.RuntimeException: Hello 1\n"
                        + "\tat Hello1.hello1(Hello1.java:1)\n"
                        + "\tat Hello1.hola(Hello1.java:42)\n"
                        + "\n"
                        + "stdout:\n"
                        + "\n"
                        + "    Hello Joe\\E");
    }

    static class ThrowingLineAssert implements LineAssert {

        private final ProcessOutput stream;

        public ThrowingLineAssert(ProcessOutput stream) {
            this.stream = stream;
        }

        @Override
        public FailureCollector evaluate(FailureCollector failureCollector) {
            failureCollector.exception(stream, org.l2x6.cli.assured.test.asserts.AssertTest.exception(1));
            return failureCollector;
        }

        @Override
        public LineAssert line(String line) {
            return this;
        }

    }

    static Throwable exception(int index) {
        RuntimeException e = new RuntimeException("Hello " + index);
        e.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("Hello" + index, "hello" + index, "Hello" + index + ".java", index),
                new StackTraceElement("Hello" + index, "hola", "Hello" + index + ".java", 42)
        });
        return e;
    }

}
