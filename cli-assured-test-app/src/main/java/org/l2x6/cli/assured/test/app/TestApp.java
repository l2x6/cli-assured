/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestApp {
    public static void main(String[] args) throws InterruptedException, IOException {
        switch (args[0]) {
        case "hello":
            System.out.println("Hello " + args[1]);
            break;
        case "helloErr":
            System.err.println("Hello stderr " + args[1]);
            break;
        case "write":
            final String msg = args[1];
            final String path = args[2];
            Files.write(Paths.get(path), msg.getBytes(StandardCharsets.UTF_8));
            break;
        case "sleep":
            final long delay = Long.parseLong(args[1]);
            System.out.println("About to sleep for " + delay + " ms");
            Thread.sleep(delay);
            System.out.println("Sleeped for " + delay + " ms");
            break;
        case "exitCode":
            final int exitCode = Integer.parseInt(args[1]);
            System.out.println("Returning exit code " + exitCode);
            System.out.flush();
            System.exit(exitCode);
            break;
        case "stdin":
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = System.in.read(buffer)) >= 0) {
                System.out.write(buffer, 0, bytesRead);
            }
            System.out.flush();
            break;
        default:
            throw new RuntimeException("Unsupported subcommand " + args[0]);
        }

        System.out.flush();
        System.err.flush();

    }
}
