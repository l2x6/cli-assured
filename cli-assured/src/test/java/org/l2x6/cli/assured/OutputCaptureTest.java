/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.StreamExpectationsSpec.OutputCapture;
import org.l2x6.cli.assured.StreamExpectationsSpec.ProcessOutput;

public class OutputCaptureTest {
    @Test
    void captureHead3Tail4() {
        final OutputCapture capture = new OutputCapture(3, 4, ProcessOutput.stdout);
        for (int i = 0; i < 10; i++) {
            capture.capture("Line " + i);
        }
        StringBuilder expected = new StringBuilder("stdout:\n");
        for (int i = 0; i < 3; i++) {
            expected.append("\n    Line ").append(i);
        }
        expected.append(
                "\n    ...\n    [3 lines omitted; set stdout().capture(maxHeadLines, maxTailLines) or stdout().captureAll() to capure more lines]\n    ...");
        for (int i = 6; i < 10; i++) {
            expected.append("\n    Line ").append(i);
        }
        Assertions.assertThat(capture.toString()).isEqualTo(expected.toString());
    }

    @Test
    void capture10LinesHead0Tail4() {
        final OutputCapture capture = new OutputCapture(0, 4, ProcessOutput.stdout);
        for (int i = 0; i < 10; i++) {
            capture.capture("Line " + i);
        }
        StringBuilder expected = new StringBuilder("stdout:\n");
        expected.append(
                "\n    [6 lines omitted; set stdout().capture(maxHeadLines, maxTailLines) or stdout().captureAll() to capure more lines]\n    ...");
        for (int i = 6; i < 10; i++) {
            expected.append("\n    Line ").append(i);
        }
        Assertions.assertThat(capture.toString()).isEqualTo(expected.toString());
    }

    @Test
    void capture2LinesHead0Tail4() {
        final OutputCapture capture = new OutputCapture(0, 4, ProcessOutput.stdout);
        for (int i = 0; i < 2; i++) {
            capture.capture("Line " + i);
        }
        StringBuilder expected = new StringBuilder("stdout:\n");
        for (int i = 0; i < 2; i++) {
            expected.append("\n    Line ").append(i);
        }
        Assertions.assertThat(capture.toString()).isEqualTo(expected.toString());
    }

    @Test
    void capture10LinesHead0Tail0() {
        final OutputCapture capture = new OutputCapture(0, 0, ProcessOutput.stdout);
        for (int i = 0; i < 10; i++) {
            capture.capture("Line " + i);
        }
        Assertions.assertThat(capture.toString()).isEqualTo("stdout: <no lines captured>");
    }

    @Test
    void capture6LinesHead4Tail4() {
        final OutputCapture capture = new OutputCapture(4, 4, ProcessOutput.stdout);
        StringBuilder expected = new StringBuilder("stdout:\n");
        for (int i = 0; i < 6; i++) {
            capture.capture("Line " + i);
            expected.append("\n    Line ").append(i);
        }
        Assertions.assertThat(capture.toString()).isEqualTo(expected.toString());
    }

    @Test
    void capture2LinesHead4Tail0() {
        final OutputCapture capture = new OutputCapture(4, 0, ProcessOutput.stdout);
        for (int i = 0; i < 2; i++) {
            capture.capture("Line " + i);
        }
        StringBuilder expected = new StringBuilder("stdout:\n");
        for (int i = 0; i < 2; i++) {
            expected.append("\n    Line ").append(i);
        }
        Assertions.assertThat(capture.toString()).isEqualTo(expected.toString());
    }

    @Test
    void capture6LinesHead4Tail0() {
        final OutputCapture capture = new OutputCapture(4, 0, ProcessOutput.stdout);
        for (int i = 0; i < 6; i++) {
            capture.capture("Line " + i);
        }
        StringBuilder expected = new StringBuilder("stdout:\n");
        for (int i = 0; i < 4; i++) {
            expected.append("\n    Line ").append(i);
        }
        expected.append(
                "\n    ...\n    [2 lines omitted; set stdout().capture(maxHeadLines, maxTailLines) or stdout().captureAll() to capure more lines]");
        Assertions.assertThat(capture.toString()).isEqualTo(expected.toString());
    }

    @Test
    void captureAll() {
        final OutputCapture capture = OutputCapture.captureAll(ProcessOutput.stdout);

        StringBuilder expected = new StringBuilder("stdout:\n");
        for (int i = 0; i < 35; i++) {
            capture.capture("Line " + i);
            expected.append("\n    Line ").append(i);
        }
        Assertions.assertThat(capture.toString()).isEqualTo(expected.toString());
    }
}
