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

SDKMAN automates the process of installing and upgrading sdks, namely Java sdks. Install is via.

```bash
curl -s "https://get.sdkman.io" | bash
```

Then install GraalVM.

```bash
sdk install java 21.3.0.r17-grl
```

Load the installed GraalVM on the current terminal.

```bash
sdk use java 21.3.0.r17-grl
```

To build native images using GraalVM it is necessary to install the `native-image` tool.

```
~/.sdkman/candidates/java/current/bin/gu install native-image
```

You should be ready to build the tool.

### Build with Gradle

Gradle is configured to build both executable jars and GraalVM native images. Gradle will also want to run tests, and
the tests require a GitLab token with `read_api` scope. The token is picked up from the environment
variable `GITLAB_TOKEN`. The tests can be skipped with the gradle flag `-x test`, in which case the GitLab token isn't
needed anymore.

To build an executable jar run gradle task `build`.

```bash
GITLAB_TOKEN="..." ./gradlew clean build
```

The executable jar is created under `build/libs/`, and it will be called something like `devex-VERSION-all.jar`.
To execute that jar run `java`.

```bash
java -jar build/libs/devex-*-all.jar -h
```

To build a GraalVM native binary run the `nativeCompile` gradle task.

```bash
GITLAB_TOKEN="..." ./gradlew clean nativeCompile
```

The binary will be created under `build/native/nativeCompile/devex`. To execute the native binary run it.

```bash
build/native/nativeCompile/devex -h
```
