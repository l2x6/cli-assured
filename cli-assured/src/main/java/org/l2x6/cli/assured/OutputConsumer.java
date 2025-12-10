/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import org.l2x6.cli.assured.asserts.Assert;

public abstract class OutputConsumer extends Thread implements Assert {
    protected volatile boolean cancelled;
    protected IOException exception;
    protected final InputStream in;

    OutputConsumer(InputStream in) {
        this.in = in;
        start();
    }

    @Override
    public abstract void run();

    public void cancel() {
        this.cancelled = true;
    }

    public static class DevNull extends OutputConsumer {

        public DevNull(InputStream in) {
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
        private final Function<Function<InputStream, OutputConsumer>, Expectations.Builder> expectations;
        private Function<InputStream, OutputConsumer> createConsumer;

        Builder(Function<Function<InputStream, OutputConsumer>, Expectations.Builder> expectations) {
            this.expectations = expectations;
        }

        /**
         * @return new {@link OutputStreamAsserts.Builder}
         */
        public OutputStreamAsserts.Builder lines() {
            return new OutputStreamAsserts.Builder(this);
        }

        Builder createConsumer(Function<InputStream, OutputConsumer> cc) {
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

        Function<InputStream, OutputConsumer> build() {
            return createConsumer;
        }

        /**
         * Create a new {@link OutputConsumer} out of this {@link Builder} and set it on the parent
         * {@link Expectations.Builder}
         *
         * @return {@link Expectations.Builder}
         * @since  0.0.1
         */
        public Expectations.Builder parent() {
            return expectations.apply(this.build());
        }

        /**
         * A shorthand for {@link #parent()}..{@link Expectations.Builder#start() start()}
         *
         * @return a started {@link CommandProcess}
         * @since  0.0.1
         */
        public CommandProcess start() {
            return parent().start();
        }

        /**
         * A shorthand for {@link #parent()}..{@link Expectations.Builder#stderr() stderr()}
         *
         * @return a new {@link OutputConsumer.Builder} to configure assertions for stderr
         * @since  0.0.1
         */
        public Builder stderr() {
            return parent().stderr();
        }

    }
}
