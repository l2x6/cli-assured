/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.mvn.assured.test.java21;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.Await;
import org.l2x6.cli.assured.Await.LineAwait;
import org.l2x6.cli.assured.CommandProcess;
import org.l2x6.mvn.assured.Mvn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuarkusDevModeTest {
    private static final Logger log = LoggerFactory.getLogger(QuarkusDevModeTest.class);

    @Test
    void devMode() throws InterruptedException, IOException {

        final String quarkusVersion = getQuarkusVersion();
        final String artifactId = "cli-assured-tests-quarkus-dev-mode";
        final Path tempProject = Paths.get("target/" + QuarkusDevModeTest.class.getSimpleName() + "-" + UUID.randomUUID())
                .resolve(artifactId)
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(tempProject.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create " + tempProject.getParent(), e);
        }

        final Mvn mvn = Mvn.fromMvnw().installIfNeeded();
        mvn
                .args(
                        "-ntp",
                        "io.quarkus:quarkus-maven-plugin:" + quarkusVersion + ":create",
                        "-DprojectGroupId=org.l2x6.cli-assured",
                        "-DprojectArtifactId=" + artifactId,
                        "-DclassName=org.l2x6.cli.assured.dev.Hello",
                        "-DplatformGroupId=io.quarkus",
                        "-DplatformVersion=" + quarkusVersion)
                .cd(tempProject.getParent())
                .then()
                .stdout().log()
                .stderr().log()
                .execute()
                .assertSuccess();

        /* Run in dev mode */
        final List<Long> pids = new ArrayList<>();
        final LineAwait<String> await = Await.lineContaining("Installed features: [");
        try (CommandProcess mvnProcess = mvn
                .args(
                        "-ntp",
                        "-Dquarkus.analytics.disabled=true",
                        "-Ddebug=false", // Disable Java debugger
                        "quarkus:dev")
                .cd(tempProject)
                .then()
                .stdout()
                .await(await)
                .log()
                .stderr().log()
                .start()) {

            await.await(Duration.ofSeconds(60));

            final long pid = mvnProcess.pid();
            pids.add(pid);
            ProcessHandle.of(pid).get().descendants()
                    .map(ProcessHandle::pid).forEach(pids::add);
            log.info("pids: " + pids);
            Assertions.assertThat(pids).hasSize(
                    System.getProperty("os.name").toLowerCase().contains("win")
                            ? 5 // Windows
                            : 2 // Linux / mac
            ); // there must be some child processes

            awaitResponse("Hello from Quarkus REST");

            /* Change the greeting in Hello.java */
            final Path helloJava = tempProject.resolve("src/main/java/org/l2x6/cli/assured/dev/Hello.java");
            Files.writeString(
                    helloJava,
                    Files.readString(helloJava, StandardCharsets.UTF_8)
                            .replace("return \"Hello from Quarkus REST\";", "return \"Hello from CLI Assured\";"),
                    StandardCharsets.UTF_8);

            awaitResponse("Hello from CLI Assured");

        }

        /* Make sure that all processes are terminated eventually */
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(
                        Assertions.assertThat(
                                pids.stream()
                                        .map(ProcessHandle::of)
                                        .noneMatch(handle -> handle.isPresent() && handle.get().isAlive()))::isTrue);
    }

    private void awaitResponse(String expected) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            ValidatableResponse response = null;
                            try {
                                final String url = "http://localhost:8080/hello";
                                log.info("Trying to get response from " + url);
                                response = RestAssured.given()
                                        .get(url)
                                        .then();
                            } catch (Exception ex) {
                                // AssertionError keeps Awaitility running
                                log.info("Request didn't work", ex);
                                throw new AssertionError("Error while getting response", ex);
                            }
                            response.statusCode(200)
                                    .body(Matchers.is(expected));
                        });
    }

    static String getQuarkusVersion() {
        Properties props = new Properties();
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("META-INF/maven/io.quarkus/quarkus-core/pom.properties")) {
            props.load(is);
            return props.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
