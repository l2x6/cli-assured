/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.Objects;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import org.l2x6.cli.assured.StreamExpectationsSpec;
import org.l2x6.cli.assured.StreamExpectationsSpec.ProcessOutput;
import org.l2x6.cli.assured.asserts.Assert.Internal.ExcludeFromJacocoGeneratedReport;

/**
 * An assertion on a number of bytes produced by a command.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  0.0.1
 */
public class ByteCountAssert implements Assert {
    private final LongPredicate expected;
    private volatile long actualCount;
    private final String description;
    private final StreamExpectationsSpec.ProcessOutput stream;

    /**
     * Assert that upon termination of the associated process, the underlying output stream has produced the given number of
     * bytes.
     *
     * @param  stream            the output stream to watch
     * @param  expectedByteCount the number of bytes expected
     *
     * @return                   a new {@link ByteCountAssert}
     * @since                    0.0.1
     */
    public static ByteCountAssert hasByteCount(ProcessOutput stream, long expectedByteCount) {
        return new ByteCountAssert(actual -> isEqual(actual, expectedByteCount),
                "Expected " + expectedByteCount + " bytes in ${stream} but found ${actual} bytes",
                stream);
    }

    /**
     * Assert that upon termination of the associated process, the underlying output stream's number of produced byted
     * satisfies the given {@link Predicate}.
     *
     * @param  stream      the output stream to watch
     * @param  expected    the condition the number of actual bytes must satisfy
     * @param  description the description of a failure, typically something like
     *                     {@code "Expected number of bytes <condition> in ${stream} but found ${actual} bytes"} where
     *                     {@code <condition>} is your human readable criteria, like {@code greater that 42},
     *                     <code>${stream}</code> is a placeholder that CLI Assured will replace by {@code stdout}
     *                     or {@code stderr} and <code>${actual}</code> is a placeholder that CLI Assured will replace
     *                     by the actual number of bytes found in the associated output stream
     * @return             a new {@link ByteCountAssert}
     * @since              0.0.1
     */
    public static ByteCountAssert hasByteCount(
            StreamExpectationsSpec.ProcessOutput stream,
            LongPredicate expected,
            String description) {
        return new ByteCountAssert(expected, description, stream);
    }

    private ByteCountAssert(
            LongPredicate expected,
            String description,
            StreamExpectationsSpec.ProcessOutput stream) {
        this.expected = Objects.requireNonNull(expected, "expected");
        this.description = Objects.requireNonNull(description, "description");
        this.stream = Objects.requireNonNull(stream, "stream");
    }

    @Override
    public FailureCollector evaluate(FailureCollector failureCollector) {
        if (!expected.test(actualCount)) {
            failureCollector.failure(Assert.Internal.formatMessage(description, this::eval));
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

    @ExcludeFromJacocoGeneratedReport
    String eval(String key) {
        switch (key) {
        case "actual":
            return String.valueOf(actualCount);
        case "stream":
            return stream.name();
        default:
            throw new IllegalArgumentException("Unexpected placeholder '" + key + "' in " + ByteCountAssert.class.getName()
                    + " description '" + description + "'.");
        }
    }

    @ExcludeFromJacocoGeneratedReport
    static boolean isEqual(long actual, long expectedByteCount) {
        return actual == expectedByteCount;
    }

}
