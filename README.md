[![Continuous Integration](https://github.com/miguelaferreira/devex-cli/actions/workflows/development.yml/badge.svg)](https://github.com/miguelaferreira/devex-cli/actions/workflows/development.yml)
[![Continuous Delivery](https://github.com/miguelaferreira/devex-cli/actions/workflows/create-release.yaml/badge.svg)](https://github.com/miguelaferreira/devex-cli/actions/workflows/create-release.yaml)
[![CodeQL](https://github.com/miguelaferreira/devex-cli/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/miguelaferreira/devex-cli/actions/workflows/codeql-analysis.yml)
[![Known Vulnerabilities](https://snyk.io/test/github/miguelaferreira/devex-cli/badge.svg)](https://snyk.io/test/github/miguelaferreira/devex-cli)

# devex-cli

Development Experience command line tools. Automating away the development gruntwork.

## Installing

The `devex` tool is built for two operating systems Linux and macOS. Each release on this repository provides
binaries for these two operating systems. To install the tool, either download the binary from the latest release, make
it executable and place it on a reachable path; or use `brew`.

```bash
brew install miguelaferreira/tools/devex-cli
```

## Documentation

The usage of the tool is documented at https://miguelaferreira.gitbook.io/devex/devex-cli/overview.

## Development

### Setup

SDKMAN automates the process of installing and upgrading SDKs, namely Java SDKs. Install it via:

```bash
curl -s "https://get.sdkman.io" | bash
```

Then install GraalVM Community Edition for Java 25 (the project baseline):

```bash
sdk install java 25.0.2-graalce
```

Load the installed GraalVM in the current terminal:

```bash
sdk use java 25.0.2-graalce
```

Modern GraalVM ships `native-image` out of the box, so no extra installation step is needed.

You should be ready to build the tool.

### Build with Gradle

Gradle (9.x) is configured to build both executable jars and GraalVM native images. Tests require a GitLab token with
`read_api` scope and a GitHub token with `read:org` + `repo` scope. The tokens are picked up from the environment
variables `GITLAB_TOKEN` and `GITHUB_TOKEN`. Tests can be skipped with `-x test`, in which case the tokens are not
needed.

A subset of integration tests clones real repositories over SSH and is only run when
`DEVEX_SSH_INTEGRATION_TESTS=true` is set. They require either a passphraseless SSH key authorised on
`gitlab.com` / `github.com`, or an `ssh-agent` with the relevant keys already loaded. CI sets this env var
automatically. Locally these tests are skipped by default.

To build an executable jar run the gradle task `build`:

```bash
GITLAB_TOKEN="..." GITHUB_TOKEN="..." ./gradlew clean build
```

The executable jar is created under `build/libs/` (e.g. `devex-VERSION-all.jar`). To execute that jar run `java`:

```bash
java -jar build/libs/devex-*-all.jar -h
```

To build a GraalVM native binary run the `nativeCompile` gradle task:

```bash
GITLAB_TOKEN="..." GITHUB_TOKEN="..." ./gradlew clean nativeCompile
```

The binary will be created under `build/native/nativeCompile/devex`. To execute the native binary run it:

```bash
build/native/nativeCompile/devex -h
```

### GraalVM native-image notes

Reflection / serialization metadata for third-party libraries is provided by the
[GraalVM Reachability Metadata repository](https://github.com/oracle/graalvm-reachability-metadata), which the Micronaut
Gradle plugin wires in automatically. The few classes that need build-time initialisation
(`org.apache.sshd`, `org.slf4j`, `ch.qos.logback`, `org.xml.sax`) are declared in the `graalvmNative` block of
`build.gradle`. There is no longer a need to manually run the JVM agent to capture configuration.
