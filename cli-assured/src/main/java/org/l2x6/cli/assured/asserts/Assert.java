/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An abstract assertion.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface Assert {

    /**
     * Throw any {@link AssertionError}s if the expectations are not met
     *
     * @throws AssertionError if the expectations are not met
     * @since                 0.0.1
     */
    void assertSatisfied();

    /**
     * Creates a new aggregate {@link Assert} failing if any of the component {@link Assert}s fails.
     *
     * @param  asserts the {@link Assert}s to aggregate.
     * @return         a new aggregate {@link Assert}
     * @since          0.0.1
     */
    static Assert all(Assert... asserts) {
        final List<Assert> copy = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(asserts)));
        return () -> copy.stream().forEach(Assert::assertSatisfied);
    }

}
