/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test.asserts;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.asserts.Assert;

public class AssertTest {

    @Test
    void collector() {
        Assertions.assertThatThrownBy(
                new Assert.FailureCollector("test-command")
                        .failure("f1")
                        .failure("f2")
                        .exception(exception(1))
                        .exception(exception(2))
                        .exception(exception(3))::assertSatisfied)
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

    static Throwable exception(int index) {
        RuntimeException e = new RuntimeException("Hello " + index);
        e.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("Hello" + index, "hello" + index, "Hello" + index + ".java", index),
                new StackTraceElement("Hello" + index, "hola", "Hello" + index + ".java", 42)
        });
        return e;
    }

}
