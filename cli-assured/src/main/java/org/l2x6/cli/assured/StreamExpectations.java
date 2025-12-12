/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.l2x6.cli.assured.asserts.ByteCountAssert;
import org.l2x6.cli.assured.asserts.LineAssert;

public class StreamExpectations implements LineAssert {

    private final List<LineAssert> lineAsserts;
    private final ByteCountAssert byteCountAssert;
    final Charset charset;
    final Supplier<OutputStream> redirect;
    final OutputConsumer.Stream stream;

    StreamExpectations(
            List<LineAssert> lineAsserts,
            ByteCountAssert byteCountAssert,
            Charset charset,
            Supplier<OutputStream> redirect,
            OutputConsumer.Stream stream) {
        this.lineAsserts = Objects.requireNonNull(lineAsserts, "lineAsserts");
        this.byteCountAssert = byteCountAssert;
        this.charset = Objects.requireNonNull(charset, "charset");
        this.redirect = redirect;
        this.stream = stream;
    }

    @Override
    public void assertSatisfied() {
        lineAsserts.stream().forEach(LineAssert::assertSatisfied);
    }

    public void assertSatisfied(long byteCount) {
        lineAsserts.stream().forEach(LineAssert::assertSatisfied);
        if (byteCountAssert != null) {
            byteCountAssert.byteCount(byteCount).assertSatisfied();
        }
    }

    @Override
    public StreamExpectations line(String line) {
        lineAsserts.stream().forEach(a -> a.line(line));
        return this;
    }

    public Charset charset() {
        return charset;
    }

    public Supplier<OutputStream> redirect() {
        return redirect;
    }

    public boolean hasLineAsserts() {
        return lineAsserts.size() > 0;
    }
}
