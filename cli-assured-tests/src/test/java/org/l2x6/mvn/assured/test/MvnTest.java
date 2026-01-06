/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.mvn.assured.test;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.CliAssured;
import org.l2x6.mvn.assured.Mvn;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

public class MvnTest {
    static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    @Test
    void fromMvnw() {
        final Path projectRoot = Paths.get("..").toAbsolutePath().normalize();
        final Path mvnwPath = projectRoot.resolve("mvnw" + (isWindows ? ".cmd" : ""));
        Assertions.assertThat(mvnwPath).isRegularFile();

        CliAssured.command(mvnwPath.toString(), "-v")
                .then()
                .stdout()
                .hasLines("Apache Maven 3.9.11 (3e54c93a704957b63ee3494413a2b544fd3d825b)")
                .execute()
                .assertSuccess();

        Mvn mvnw = Mvn.fromMvnw(Paths.get(".").toAbsolutePath().normalize());

        Assertions.assertThat(mvnw.isInstalled()).isTrue();

        mvnw.args("--version")
                .then()
                .stdout()
                .hasLines("Apache Maven 3.9.11 (3e54c93a704957b63ee3494413a2b544fd3d825b)")
                .execute()
                .assertSuccess();
    }

    @Test
    void fromMvnwWithM2Directory() {

        final Path m2Dir = Paths.get("target/m2-" + UUID.randomUUID());

        final Mvn mvnw = Mvn.fromMvnw(
                Paths.get("target/test-classes/test-project").toAbsolutePath().normalize(),
                m2Dir);

        Assertions.assertThat(mvnw.isInstalled()).isFalse();

        mvnw
                .installIfNeeded()
                .args("--version")
                .then()
                .stdout()
                .hasLines("Apache Maven 3.9.11 (3e54c93a704957b63ee3494413a2b544fd3d825b)")
                .execute()
                .assertSuccess();

        Assertions.assertThat(m2Dir.resolve("wrapper/dists/apache-maven-3.9.11/a2d47e15/bin/mvn")).isRegularFile();

    }

    @Test
    void installIfNeeded() throws IOException {
        final Path m2Dir = Paths.get("target/m2-" + UUID.randomUUID());
        final String version = "3.9.11";
        Mvn mvn = Mvn.version(version)
                .m2Directory(m2Dir);
        Assertions.assertThat(mvn.isInstalled()).isFalse();

        Files.createDirectories(m2Dir);
        Assertions.assertThat(mvn.isInstalled()).isFalse();

        Assertions.assertThatThrownBy(mvn::assertInstalled)
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Maven 3.9.11 is not installed ");

        mvn = mvn.installIfNeeded();

        Assertions.assertThat(mvn.isInstalled()).isTrue();

        Assertions.assertThatThrownBy(mvn::install)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith("because it exists already");

        CliAssured.command(mvn.executable(), "-v")
                .then()
                .stdout()
                .hasLines("Apache Maven 3.9.11 (3e54c93a704957b63ee3494413a2b544fd3d825b)")
                .execute()
                .assertSuccess();

        final String nameBase = "target/maven-" + version + "-" + UUID.randomUUID();
        final Path localZipPath = Paths.get(nameBase + ".zip");
        CliAssured.command(
                "curl" + (isWindows ? ".exe" : ""),
                "-sSfL",
                "-o", localZipPath.toString(),
                "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/"
                        + version + "/apache-maven-" + version + "-bin.zip")
                .then()
                .execute()
                .assertSuccess();

        /* Unzip */
        if (isWindows) {
            CliAssured.command(
                    "pwsh.exe",
                    "-NoProfile",
                    "-Command", "Expand-Archive " + localZipPath + " -DestinationPath " + nameBase)
                    .then()
                    .execute()
                    .assertSuccess();
        } else {
            CliAssured.command(
                    "unzip",
                    localZipPath.toString(),
                    "-d", nameBase).then()
                    .execute()
                    .assertSuccess();
        }

        final Path unzipDirRootDir = Paths.get(nameBase).resolve("apache-maven-" + version);
        Assertions.assertThat(unzipDirRootDir).isDirectory();

        List<Difference> diffs = compareTrees(mvn.home(), unzipDirRootDir);
        org.assertj.core.api.Assertions.assertThat(diffs)
                .withFailMessage("POSIX attribute differences:\n%s",
                        diffs.stream().map(Object::toString).collect(java.util.stream.Collectors.joining("\n")))
                .isEmpty();

        /* Because isInstalled() returns true */
        Assertions.assertThat(mvn.installIfNeeded()).isSameAs(mvn);

        Mvn customHome = Mvn.version(version).home(unzipDirRootDir);
        Assertions.assertThat(customHome.isInstalled()).isTrue();
        customHome.args("--version")
                .then()
                .stdout()
                .hasLines("Apache Maven 3.9.11 (3e54c93a704957b63ee3494413a2b544fd3d825b)")
                .execute()
                .assertSuccess();

    }

    @Test
    void distributionUrl() throws IOException {
        final String version = "3.9.12";
        Mvn mvn = Mvn.version(version)
                .m2Directory(Paths.get("src/test/resources/m2"))
                .distributionUrl("https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/" + version
                        + "/apache-maven-" + version + "-bin.zip");
        Assertions.assertThat(mvn.home())
                .isEqualTo(Paths.get("src/test/resources/m2/wrapper/dists/apache-maven-3.9.12/6068d197"));
        Assertions.assertThat(mvn.isInstalled()).isFalse();
    }

    static final class Difference {
        public final Path relativePath;
        public final String message;

        public Difference(Path relativePath, String message) {
            this.relativePath = relativePath;
            this.message = message;
        }

        @Override
        public String toString() {
            return relativePath + ": " + message;
        }
    }

    static List<Difference> compareTrees(Path leftRoot, Path rightRoot) throws IOException {
        Objects.requireNonNull(leftRoot, "leftRoot");
        Objects.requireNonNull(rightRoot, "rightRoot");

        if (!Files.isDirectory(leftRoot, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("leftRoot is not a directory: " + leftRoot);
        }
        if (!Files.isDirectory(rightRoot, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("rightRoot is not a directory: " + rightRoot);
        }

        // Ensure both support POSIX attributes
        FileStore leftStore = Files.getFileStore(leftRoot);
        FileStore rightStore = Files.getFileStore(rightRoot);
        final boolean leftSupportsFileAttributeView = leftStore.supportsFileAttributeView(PosixFileAttributeView.class);
        final boolean rightSupportsFileAttributeView = rightStore.supportsFileAttributeView(PosixFileAttributeView.class);
        if (leftSupportsFileAttributeView != rightSupportsFileAttributeView) {
            throw new IllegalStateException(
                    "POSIX attributes not supported on both or none of " + leftRoot + " and " + rightRoot);
        }

        Map<Path, Path> leftEntries = listAllRelativeEntries(leftRoot);
        Map<Path, Path> rightEntries = listAllRelativeEntries(rightRoot);

        Set<Path> allRel = new TreeSet<>();
        allRel.addAll(leftEntries.keySet());
        allRel.addAll(rightEntries.keySet());

        List<Difference> diffs = new ArrayList<>();

        for (Path rel : allRel) {
            Path left = leftEntries.get(rel);
            Path right = rightEntries.get(rel);

            if (left == null) {
                diffs.add(new Difference(rel, "Missing on LEFT; exists on RIGHT: " + right));
                continue;
            }
            if (right == null) {
                diffs.add(new Difference(rel, "Missing on RIGHT; exists on LEFT: " + left));
                continue;
            }

            compareOne(rel, left, right, diffs, leftSupportsFileAttributeView);
        }

        return diffs;
    }

    private static Map<Path, Path> listAllRelativeEntries(Path root) throws IOException {
        // We include directories too (except root), so directory perms/owner/group are compared.
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(p -> !p.equals(root))
                    .collect(Collectors.toMap(
                            p -> root.relativize(p),
                            p -> p,
                            (a, b) -> a,
                            LinkedHashMap::new));
        }
    }

    private static void compareOne(Path rel, Path left, Path right, List<Difference> diffs, boolean posixPermissions)
            throws IOException {
        boolean leftIsSym = Files.isSymbolicLink(left);
        boolean rightIsSym = Files.isSymbolicLink(right);

        if (leftIsSym != rightIsSym) {
            diffs.add(new Difference(rel, "Type mismatch: left isSymlink=" + leftIsSym + ", right isSymlink=" + rightIsSym));
            return;
        }

        if (leftIsSym) {
            Path leftTarget = Files.readSymbolicLink(left);
            Path rightTarget = Files.readSymbolicLink(right);
            if (!Objects.equals(leftTarget, rightTarget)) {
                diffs.add(new Difference(rel, "Symlink target differs: left->" + leftTarget + ", right->" + rightTarget));
            }
            // Compare symlink's own POSIX metadata (NOT target): use NOFOLLOW_LINKS
            if (posixPermissions) {
                comparePosixAttrs(rel, left, right, diffs, true);
            }
            return;
        }

        boolean leftIsDir = Files.isDirectory(left, NOFOLLOW_LINKS);
        boolean rightIsDir = Files.isDirectory(right, NOFOLLOW_LINKS);
        if (leftIsDir != rightIsDir) {
            diffs.add(new Difference(rel, "Type mismatch: left isDir=" + leftIsDir + ", right isDir=" + rightIsDir));
            return;
        }

        // Compare POSIX metadata for regular files/directories (not following links anyway)
        if (posixPermissions) {
            comparePosixAttrs(rel, left, right, diffs, false);
        }
    }

    private static void comparePosixAttrs(Path rel, Path left, Path right, List<Difference> diffs, boolean noFollow)
            throws IOException {
        PosixFileAttributes a = readPosix(left, noFollow);
        PosixFileAttributes b = readPosix(right, noFollow);

        if (!a.permissions().equals(b.permissions())) {
            diffs.add(new Difference(rel, "Permissions differ: left=" + a.permissions() + ", right=" + b.permissions()));
        }
        if (!a.owner().getName().equals(b.owner().getName())) {
            diffs.add(new Difference(rel, "Owner differs: left=" + a.owner().getName() + ", right=" + b.owner().getName()));
        }
        if (!a.group().getName().equals(b.group().getName())) {
            diffs.add(new Difference(rel, "Group differs: left=" + a.group().getName() + ", right=" + b.group().getName()));
        }
    }

    private static PosixFileAttributes readPosix(Path p, boolean noFollow) throws IOException {
        return Files.readAttributes(
                p,
                PosixFileAttributes.class,
                noFollow ? new LinkOption[] { NOFOLLOW_LINKS } : new LinkOption[0]);
    }
}
