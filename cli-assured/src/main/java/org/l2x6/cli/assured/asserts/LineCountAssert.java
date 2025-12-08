/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.concurrent.atomic.AtomicInteger;

class LineCountAssert implements LineAssert {
    private final int expectedCount;
    private final AtomicInteger actualCount = new AtomicInteger();

    LineCountAssert(int expectedCount) {
        this.expectedCount = expectedCount;
    }

    @Override
    public void assertSatisfied() {
        if (actualCount.get() != expectedCount) {
            throw new AssertionError("");
        }
    }

    @Override
    public void line(String line) {
        actualCount.incrementAndGet();
    }
}
