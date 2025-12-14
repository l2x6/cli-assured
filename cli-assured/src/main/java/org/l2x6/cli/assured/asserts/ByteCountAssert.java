/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.function.Predicate;
import org.l2x6.cli.assured.StreamExpectationsSpec.ProcessOutput;

/**
 * An assertion on a number of bytes produced by a command.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  0.0.1
 */
public class ByteCountAssert implements Assert {
    private final Predicate<Long> expected;
    private volatile long actualCount;
    private final String description;

    private ByteCountAssert(Predicate<Long> expected, String description) {
        this.expected = expected;
        this.description = description;
    }

    @Override
    public FailureCollector evaluate(FailureCollector failureCollector) {
        if (!expected.test(actualCount)) {
            failureCollector.failure(String.format(description, actualCount));
        }
        return failureCollector;
    }

    /**
     * Record the actual number of bytes.
     * Any assertion failures should be reported via {@link #evaluate()} rather than by throwing an exception from this
     * method.
     *
     * @param  byteCount the number of bytes to record
     * @return           this {@link ByteCountAssert}
     * @since            0.0.1
     */
    public ByteCountAssert byteCount(long actualCount) {
        this.actualCount = actualCount;
        return this;
    }

    /**
     * Assert that upon termination of the associated process, the underlying output stream has produced the given number of
     * bytes.
     *
     * @param  expectedByteCount the number of bytes expected
     * @param  stream            the output stream to watch
     * @return                   a new {@link ByteCountAssert}
     * @since                    0.0.1
     */
    public static ByteCountAssert hasByteCount(long expectedByteCount, ProcessOutput stream) {
        return new ByteCountAssert(actual -> isEqual(actual.longValue(), expectedByteCount),
                "Expected " + expectedByteCount + " bytes in " + stream + " but found %d bytes");
    }

    @ExcludeFromJacocoGeneratedReport
    static boolean isEqual(long actual, long expectedByteCount) {
        return actual == expectedByteCount;
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
        return new ByteCountAssert(expected, description);
    }

}
