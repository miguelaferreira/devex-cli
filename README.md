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
sdk install java 21.2.0.r16-grl
```

Load the installed GraalVM on the current terminal.

```bash
sdk use java 21.2.0.r16-grl
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

To build a GraalVM native binary run the `nativeImage` gradle task.

```bash
GITLAB_TOKEN="..." ./gradlew clean nativeImage
```

The binary will be created under `build/native-image/application`. To execute the native binary run it.

```bash
build/native-image/application -h
```

### GraalVM Config

In order to properly build a native binary some configuration needs to be generated from running the app as a jar. That
configuration is then included as a resource for the application, and the native image builder will load that to
properly create the native binary. That can be done by running the app from jar while setting a JVM agent to collect the
configuration. During the app run all functionality should be exercised.

```
./gradlew clean build
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar build/libs/devex-*-all.jar ...
```

However, not all functionality of the app can be exercised in a single run (eg. cloning via SSH vs HTTPS). Therefore,
different executions need to be made (as many as different and independent features of the app), each generating a set
of config, which at the end needs to be merged. See
the [native-image manual](https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/#the-native-image-configure-tool)
for more information on how this works. Since the tool that merges the configuration (`native-image-configure-launcher`)
is not shipped with graalvm releases, it has to be built. This project includes two binaries of the tool, one for macOS
and the other for Linux, under [graalvm/bin](https://github.com/miguelaferreira/devex-cli/blob/master/graalvm/bin). During CI workflows that run on PR to `main` branch, the
app is executed with the `native-image-agent` producing different sets of configurations for different combinations of
input options and parameters. This is done in
script [.github/scripts/create-native-image-build-config.sh](https://github.com/miguelaferreira/devex-cli/blob/master/.github/scripts/create-native-image-build-config.sh). Then
the generated configurations are merged into the project's sources by another
script, [.github/scripts/merge-native-image-build-config.sh](https://github.com/miguelaferreira/devex-cli/blob/master/.github/scripts/merge-native-image-build-config.sh).
Finally, a new commit is made to the PR branch with the updated configuration.
