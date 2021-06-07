[![Continuous Integration](https://github.com/miguelaferreira/devex/actions/workflows/development.yml/badge.svg)](https://github.com/miguelaferreira/devex/actions/workflows/development.yml)
[![Continuous Delivery](https://github.com/miguelaferreira/devex/actions/workflows/create-release.yaml/badge.svg)](https://github.com/miguelaferreira/devex/actions/workflows/create-release.yaml)
[![CodeQL](https://github.com/miguelaferreira/devex-cli/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/miguelaferreira/devex-cli/actions/workflows/codeql-analysis.yml)
[![Known Vulnerabilities](https://snyk.io/test/github/miguelaferreira/devex/badge.svg)](https://snyk.io/test/github/miguelaferreira/devex-cli)

# devex-cli

Development Experience command line tools.

## Installing

The `devex` tool is built for two operating systems Linux and macOS. Each release on this repository provides
binaries for these two operating systems. To install the tool, either download the binary from the latest release, make
it executable and place it on a reachable path; or use `brew`.

```bash
brew install miguelaferreira/tools/devex-cli
```

## GitLab

GitLab offers the ability to create groups of repositories and then leverage those groups to manage multiple
repositories at one. Things like CI/CD, user membership can be defined at the group level and then inherited by all the
underlying repositories. Furthermore, it's also possible to create relationships between repositories simply by
leveraging the group structure. For example, one can include git sub-modules and reference them by their relative path.

It's handy, and sometimes needed, to clone the groups of repositories preserving the group structure. That is what this
tool does.

## Usage

Both the gitlab url (`GITLAB_URL`) and the private token (`GITLAB_TOKEN`) for accessing GitLab API are read from the
environment. The GitLab url defaults to [https://gitlab.com](https://gitlab.com) when not defined in the environment.
For cloning public groups no token is needed, for private groups a token with scope `read_api` is required. See
GitLab's [Limiting scopes of a personal access token](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html#limiting-scopes-of-a-personal-access-token)
for more details on token scopes.

### Public groups

Cloning public groups does not require a gitlab token, however without a token it won't be possible to detect the version of GitLab server.
That always results in an error message, and in defaulting to using the `group_descendants` API, which is not available in versions of GitLab pre `13.5`.

**Cloning a public group by name, to the current directory, using SSH protocol**
```bash
devex gitlab clone open-source-devex
```
**Cloning a public group by name, to the current directory, initializing git submodules, using SSH protocol**
```bash
devex gitlab clone --recurse-submodules open-source-devex
```
**Cloning a group by name, to a specified location, using SSH protocol**
```bash
devex gitlab clone open-source-devex ~/some-path-on-my-home
```
**Cloning a public group by id, to the current directory, using SSH protocol**
```bash
devex gitlab clone --search-mode id 2150497
```
**Cloning a public group by path, to the current directory, using SSH protocol**
```bash
devex gitlab clone --search-mode full_path open-source-devex/terraform-modules/aws
```
**Cloning a public group by name, to the current directory, using HTTPS protocol**
```bash
devex gitlab clone --clone-protocol https open-source-devex
```

### Private groups and self-hosted GitLab servers

Cloning private groups requires a gitlab token.
Cloning from GitLab servers other than [gitlab.com](https://gitlab.com) requires the url for the server.

**Cloning a private group by name, to the current directory, using SSH protocol**
```bash
GITLAB_TOKEN="..." devex gitlab clone "some private group"
```
**Cloning a private group by name, to the current directory, using HTTPS protocol**
```bash
GITLAB_TOKEN="..." devex gitlab clone --clone-protocol https --https-username miguelaferreira "some private group"
```
**Cloning a private group by name, to the current directory, using SSH protocol, from a self-hosted GitLab server**
```bash
GITLAB_URL="https://gitlab.acme.com" GITLAB_TOKEN="..." devex gitlab clone "some private group"
```

### Full usage description
```
$ devex gitlab clone -h
Usage:

Clone an entire GitLab group with all sub-groups and repositories.
While cloning initialize project git sub-modules (may require two runs due to ordering of projects).
When a project is already cloned, tries to initialize git sub-modules.

devex gitlab clone [-hrvVx] [--debug] [--trace] [-u[=<httpsUsername>]] [-c=<cloneProtocol>] [-m=<searchMode>] GROUP PATH

GitLab configuration:

The GitLab URL and private token are read from the environment, using GITLAB_URL and GITLAB_TOKEN variables.
GITLAB_URL defaults to 'https://gitlab.com'.
The GitLab token is used for both querying the GitLab API and discover the group to clone and as the password for
cloning using HTTPS.
No token is needed for public groups and repositories.

Parameters:
      GROUP                  The GitLab group to clone.
      PATH                   The local path where to create the group clone.

Options:
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.
  -r, --recurse-submodules   Initialize project submodules. If projects are already cloned try and initialize
                               sub-modules anyway.
  -c, --clone-protocol=<cloneProtocol>
                             Chose the transport protocol used clone the project repositories. Valid values: SSH, HTTPS.
  -m, --search-mode=<searchMode>
                             Chose how the group is searched for. Groups can be searched by name or full path. Valid
                               values: NAME, FULL_PATH, ID.
  -u, --https-username[=<httpsUsername>]
                             The username to authenticate with when the HTTPS clone protocol is selected. This option
                               is required when cloning private groups, in which case the GitLab token will be used as
                               the password.
  -v, --verbose              Print out extra information about what the tool is doing.
  -x, --very-verbose         Print out even more information about what the tool is doing.
      --debug                Sets all loggers to DEBUG level.
      --trace                Sets all loggers to TRACE level. WARNING: this setting will leak the GitLab token or
                               password to the logs, use with caution.

Copyright(c) 2021 - Miguel Ferreira - GitHub/GitLab: @miguelaferreira
```

### Protocols

The tool supports both SSH and HTTPS protocols for cloning projects, SSH being the default protocol.

#### SSH

There are three requirements for cloning via SSH that apply to both public and private groups:

1. A known hosts file containing and entry for the GitLab server must exist in the default
   location (`${HOME}/.ssh/known_hosts`). This entry looks something like
   this: `gitlab.com,172.65.251.78 ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBFSMqzJeV9rUzU4kWitGjeR4PWSa29SPqJ1fVkhtj3Hw9xjLVXVYrU9QlYWrOLXBpQ6KWjbjTDTdDkoohFzgbEY=`
2. A private key must exist in the default location (`${HOME}/.ssh/id_rsa`) or in a different location as long there is
   an entry for the GitLab server in the ssh client configuration that points to the correct key.
   See [GitLab documentation on configuring SSH access](https://docs.gitlab.com/ee/ssh/) for more information on how to
   set this up
3. The private key must be in the PEM format, the keyfile needs to start with the line:
   `-----BEGIN RSA PRIVATE KEY-----`
   _Note: Support for the OpenSSH key format will be available once a native binary can be produced with a Java15
   GraalVM native-image. See Issue #16 to track progress of this_

Cloning via HTTPS (cli option `--clone-protocol=HTTPS`) does not require any setup for public groups. For private
groups, the username needs to be provided (cli option `--https-username=<USERNAME>`). The GitLab token specified on the
environment will be used as the password for the HTTPS authentication.

## Development

### Setup

SDKMAN automates the process of installing and upgrading sdks, namely Java sdks. Install is via.

```bash
curl -s "https://get.sdkman.io" | bash
```

Then install GraalVM.

```bash
sdk install java 21.1.0.r11-grl
```

Load the installed GraalVM on the current terminal.

```bash
sdk use java 21.1.0.r11-grl
```

To build native images using GraalVM it is necessary to install the `native-image` tool.

```
~/.sdkman/candidates/java/21.1.0.r11-grl/bin/gu install native-image
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
