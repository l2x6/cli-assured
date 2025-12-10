/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.function.Predicate;

/**
 * An assertion on a number of bytes produced by a command.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  0.0.1
 */
public interface ByteCountAssert extends Assert {

    /**
     * Record the actual number of bytes and throw any {@link AssertionError}s from {@link #assertSatisfied()} rather than
     * from this method.
     *
     * @param  byteCount the number of bytes to record
     * @return           this {@link ByteCountAssert}
     * @since            0.0.1
     */
    ByteCountAssert byteCount(long byteCount);

    /**
     * Assert that upon termination of the associated process, the underlying output stream has produced the given number of
     * bytes.
     *
     * @param  expectedByteCount
     * @return                   a new {@link ByteCountAssert}
     * @since                    0.0.1
     */
    static ByteCountAssert hasByteCount(long expectedByteCount) {
        return new BytesCountAssert(actual -> actual.longValue() == expectedByteCount,
                "Expected " + expectedByteCount + " bytes but found %d bytes");
    }

    /**
     * Assert that upon termination of the associated process, the underlying output stream's number of produced byted
     * satisfies the given {@link Predicate}.
     *
     * @param  expected    the condition the number of actual bytes must satisfy
     * @param  description the description of a failure, typically something like
     *                     {@code "Expected number of bytes <condition> but found %d bytes"}
     * @return             a new {@link ByteCountAssert}
     * @since              0.0.1
     */
    public static ByteCountAssert hasByteCount(Predicate<Long> expected, String description) {
        return new BytesCountAssert(expected, description);
    }

    static class BytesCountAssert implements ByteCountAssert {
        private final Predicate<Long> expected;
        private volatile long actualCount;
        private final String description;

        BytesCountAssert(Predicate<Long> expected, String description) {
            this.expected = expected;
            this.description = description;
        }

        @Override
        public void assertSatisfied() {
            if (!expected.test(actualCount)) {
                throw new AssertionError(String.format(description, actualCount));
            }
        }

        @Override
        public ByteCountAssert byteCount(long actualCount) {
            this.actualCount = actualCount;
            return this;
        }
    }

}
