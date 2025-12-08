/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import org.l2x6.cli.assured.Expectations.Builder;
import org.l2x6.cli.assured.asserts.OutputAssert;

public abstract class OutputAsserts extends Thread implements OutputAssert {
    protected volatile boolean cancelled;
    protected IOException exception;
    protected final InputStream in;

    OutputAsserts(InputStream in) {
        this.in = in;
        start();
    }

    @Override
    public abstract void run();

    public void cancel() {
        this.cancelled = true;
    }

    public static Builder builder(Function<Function<InputStream, OutputAsserts>, Expectations.Builder> expectations) {
        return new Builder(expectations);
    }

    public static class DummyOutputConsumer extends OutputAsserts {

        public DummyOutputConsumer(InputStream in) {
            super(in);
        }

        @Override
        public void assertSatisfied() {
        }

        @Override
        public void run() {
            byte[] bytes = new byte[1024];
            try {
                while (!cancelled && in.read(bytes) >= 0) {
                }
            } catch (IOException e) {
                exception = e;
            }
        }

    }

    public static class Builder {
        private final Function<Function<InputStream, OutputAsserts>, Expectations.Builder> expectations;
        private Function<InputStream, OutputAsserts> createConsumer;

        Builder(Function<Function<InputStream, OutputAsserts>, Expectations.Builder> expectations) {
            this.expectations = expectations;
        }

        /**
         * @return new {@link OutputLineAsserts.Builder}
         */
        public OutputLineAsserts.Builder lines() {
            return OutputLineAsserts.builder(this);
        }

        Builder createConsumer(Function<InputStream, OutputAsserts> cc) {
            if (createConsumer != null) {
                // TODO: better error message; explain that lines() or the possible future alternatives can be called only once
                throw new IllegalStateException("Cannot create multiple Output consumers");
            }
            this.createConsumer = cc;
            return this;
        }

        public CommandProcess exitCode(int... exitCodes) {
            return parent().exitCode(exitCodes).start();
        }

        Function<InputStream, OutputAsserts> build() {
            return createConsumer;
        }

        /**
         * Create a new {@link OutputAsserts} out of this {@link Builder} and set it on the parent
         * {@link Expectations.Builder}
         *
         * @return {@link Expectations.Builder}
         * @since  0.0.1
         */
        public Expectations.Builder parent() {
            return expectations.apply(this.build());
        }

        public CommandProcess start() {
            return parent().start();
        }

        public Builder stderr() {
            return parent().stderr();
        }

    }
}
