/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            for (int i = 1; i < args.length; i++) {
                final long delay = Long.parseLong(args[i]);
                System.out.println("About to sleep for " + delay + " ms");
                Thread.sleep(delay);
                System.out.println("Sleeped for " + delay + " ms");
            }
            break;
        case "outputLines":
            final int cnt = Integer.parseInt(args[1]);
            for (int i = 0; i < cnt; i++) {
                System.out.println("Line " + i);
            }
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
        case "hello-server":
            helloServer();
            System.out.flush();
            break;
        default:
            throw new RuntimeException("Unsupported subcommand " + args[0]);
        }

        System.out.flush();
        System.err.flush();

    }

    static void helloServer() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(0);
        serverSocket.setReuseAddress(true);
        final ExecutorService pool = Executors.newSingleThreadExecutor();

        // Ensure clean shutdown on Ctrl+C / SIGTERM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            pool.shutdownNow();
        }));
        System.out.println("hello-server listening on port: " + serverSocket.getLocalPort());
        System.out.flush();

        while (true) {
            try {
                Socket client = serverSocket.accept();
                pool.submit(() -> {
                    try (
                            Socket s = client;
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                            OutputStreamWriter out = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = in.readLine()) != null) {
                            System.out.println("Sending response Hello " + line);
                            System.out.flush();
                            out.write("Hello " + line + "\n");
                            out.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

}
