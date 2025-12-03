/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;

/**
 * An output of a {@link CommandProcess}.
 * Both {@code stdout} and {@code stderr} are supposed to append to this {@link CommandOutput}.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CommandOutput {

    /**
     * Kind of an output stream of a process
     */
    public static enum Stream {
        stdout, stderr, any() {
            public boolean contains(Stream other) {
                return true;
            }
        };

        public boolean contains(Stream other) {
            return this == other;
        }
    };

    private final List<Line> lines = new ArrayList<>();
    private final Object lock = new Object();

    CommandOutput() {
    }

    void add(Stream stream, String t) {
        synchronized (lock) {
            lines.add(new Line(stream, t));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        synchronized (lock) {
            lines.forEach(l -> sb.append(l.stream).append(": ").append(l.line).append('\n'));
        }
        return sb.toString();
    }

    /**
     * Asserts that the given line is present in either {@code stdout} or {@code stderr}.
     *
     * @param  line the line text to look for
     * @return      this {@link CommandOutput}
     * @since       0.0.1
     */
    public CommandOutput hasLine(String line) {
        synchronized (lock) {
            Assertions.assertThat(lines).map(Line::line).contains(line);
        }
        return this;
    }

    /**
     * Asserts that the given line is present in either {@code stdout} or {@code stderr}.
     *
     * @param  stream the stream to seek in
     * @param  line   the line text to look for
     * @return        this {@link CommandOutput}
     * @since         0.0.1
     */
    public CommandOutput hasLine(Stream stream, String line) {
        synchronized (lock) {
            Assertions.assertThat(lines).filteredOn(l -> stream.contains(l.stream)).map(Line::line).contains(line);
        }
        return this;
    }

    /**
     * Asserts that the specified number of lines was captured by in either {@code stdout} or {@code stderr}.
     *
     * @param  cound the expected line count
     * @return       this {@link CommandOutput}
     * @since        0.0.1
     */
    public CommandOutput hasLineCount(int count) {
        synchronized (lock) {
            Assertions.assertThat(lines).hasSize(count);
        }
        return this;
    }

    /**
     * Assert that all lines satisfy the given {@link Consumer}. Handy with AssertJ assertions.
     *
     * @param  consumer the assertion
     * @return          this {@link CommandOutput}
     * @since           0.0.1
     */
    public CommandOutput satisfies(Consumer<? super List<? extends Line>> consumer) {
        synchronized (lock) {
            Assertions.assertThat(lines).satisfies(consumer);
        }
        return this;
    }

    /**
     * @return a copy of {@link #lines}
     * @since  0.0.1
     */
    public List<Line> lines() {
        synchronized (lock) {
            return new ArrayList<>(lines);
        }
    }

    /**
     * A line in an output of a command.
     *
     * @since 0.0.1
     */
    public static class Line {
        private final Stream stream;
        private final String line;

        Line(Stream stream, String line) {
            this.stream = stream;
            this.line = line;
        }

        /**
         * @return {@link Stream#stdout} or {@value Stream#stderr} depending on where this line originates from
         */
        public Stream stream() {
            return stream;
        }

        /**
         * @return the text of the output line
         */
        public String line() {
            return line;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((line == null) ? 0 : line.hashCode());
            result = prime * result + ((stream == null) ? 0 : stream.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Line other = (Line) obj;
            if (line == null) {
                if (other.line != null)
                    return false;
            } else if (!line.equals(other.line))
                return false;
            if (stream != other.stream)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return stream + ": " + line;
        }

    }

}
