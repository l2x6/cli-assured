/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test.app;

public class TestApp {
    public static void main(String[] args) throws InterruptedException {
        switch (args[0]) {
        case "hello":
            System.out.println("Hello " + args[1]);
            break;
        case "helloErr":
            System.err.println("Hello stderr " + args[1]);
            break;
        case "sleep":
            final long delay = Long.parseLong(args[1]);
            System.out.println("About to sleep for " + delay + " ms");
            Thread.sleep(delay);
            System.out.println("Sleeped for " + delay + " ms");
            break;
        default:
            throw new RuntimeException("Unsupported subcommand " + args[0]);
        }

    }
}
