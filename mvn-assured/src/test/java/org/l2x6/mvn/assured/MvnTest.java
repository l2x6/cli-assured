/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.mvn.assured;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class MvnTest {
    @Test
    void hashString() {
        Assertions
                .assertThat(
                        Mvn.hashString(
                                "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip"))
                .isEqualTo("a2d47e15");
    }

}
