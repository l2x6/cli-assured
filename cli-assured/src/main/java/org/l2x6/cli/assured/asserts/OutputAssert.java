/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface OutputAssert {

    void assertSatisfied();

    static OutputAssert all(OutputAssert... asserts) {
        final List<OutputAssert> copy = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(asserts)));
        return () -> copy.stream().forEach(OutputAssert::assertSatisfied);
    }

}
