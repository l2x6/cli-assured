/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class CliAssertUtils {
    private CliAssertUtils() {
    }

    static <T> List<T> join(List<T> base, T[] array) {
        final List<T> args = new ArrayList<>(base.size() + array.length);
        args.addAll(base);
        for (T a : array) {
            args.add(a);
        }
        return Collections.unmodifiableList(args);
    }

    static <T> List<T> join(List<T> base, Collection<T> newElements) {
        final List<T> args = new ArrayList<>(base.size() + newElements.size());
        args.addAll(base);
        args.addAll(newElements);
        return Collections.unmodifiableList(args);
    }

    static <T> List<T> join(List<T> base, T elem) {
        final List<T> args = new ArrayList<>(base.size() + 1);
        args.addAll(base);
        args.add(elem);
        return Collections.unmodifiableList(args);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.CONSTRUCTOR, ElementType.METHOD })
    public @interface ExcludeFromJacocoGeneratedReport {
    }

}
