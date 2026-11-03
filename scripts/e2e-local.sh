#!/usr/bin/env bash
#
# Local end-to-end validation of a devex-cli upgrade branch, meant to be run by the
# maintainer before cutting a release. Run it from anywhere inside the checkout.
#
#   scripts/e2e-local.sh [options]
#
# Options:
#   --only STAGES         comma separated subset of: build,native,smoke,jar,brew
#                         (default: all, in that order). "unit" is accepted as an alias
#                         of "build", because `gradlew build` runs all the tests.
#   --skip-native-build   native/smoke/brew reuse an existing native binary instead of
#                         running `gradlew nativeCompile`
#   --binary PATH         native binary under test (default build/native/nativeCompile/devex)
#   --tap-repo URL        homebrew tap to test with
#                         (default https://github.com/miguelaferreira/homebrew-tools)
#   --tap-branch BRANCH   tap branch to test with (default claude/tender-maxwell-wrnoym)
#   --force-brew          let the brew stage uninstall devex-cli / devex-cli-jdk and
#                         replace an already tapped miguelaferreira/tools (restored after)
#   -h, --help            this text
#
# Stages:
#   build   ./gradlew --no-daemon clean build (all unit + integration tests, with
#           GITHUB_TOKEN / GITLAB_TOKEN and DEVEX_SSH_INTEGRATION_TESTS=true like CI),
#           then a test count summary from build/test-results/test/*.xml.
#           NOTE: `clean` wipes build/, including a previously built native binary
#           (with --skip-native-build the binary is stashed and restored around it).
#   native  ./gradlew --no-daemon nativeCompile; shows `devex -V`, size and architecture.
#   smoke   .github/scripts/run-native-binary.sh against the native binary. That script
#           hardcodes `build/native/nativeCompile/devex` relative to the cwd and writes
#           clones next to it, so it is run from a scratch directory that has the binary
#           under test at that path and a copy of the terraform test module. Nothing
#           is written into the checkout.
#   jar     `java -jar build/libs/devex-*-all.jar`: version, help, one GitLab HTTPS
#           clone and one GitHub HTTPS clone (covers the devex-cli-jdk code path).
#   brew    simulates a release locally and installs through the tap's formulas:
#           - a fake GitHub release tree (real binary for this platform, placeholders
#             for the others, source archive from `git archive HEAD`) is served by
#             python3 http.server on a free localhost port
#           - the tap is cloned into a temp dir; ONLY that copy of update-formula.sh is
#             pointed at the local server and run for the VERSION file's version
#           - brew style and brew audit --strict run on the updated, pristine formulas
#           - the copy is tapped as miguelaferreira/tools (the real name, because both
#             formulas use `conflicts_with "miguelaferreira/tools/..."`, which a
#             throwaway tap name would not match). The brew stage therefore refuses to
#             run if that tap or either formula is already present, unless --force-brew;
#             with --force-brew an existing tap is untapped and re-tapped from its
#             original remote and branch at the end. Uninstalled formulae are NOT
#             reinstalled, reinstall them yourself.
#           - the formula URLs in the tapped copy are then rewritten to the local
#             server; each formula is installed, `brew test`ed, `devex -V` and a GitHub
#             HTTPS clone are run with the installed binary, the jdk launcher is checked
#             with JAVA_HOME=/nonexistent, and installing one formula while the other is
#             installed must fail with a conflict. Everything is untapped on exit.
#             On macOS Intel devex-cli must refuse to install (disabled formula).
#           - side effect that is left behind on purpose: openjdk@25 (dependency of the
#             jdk formula) stays installed.
#
# Needs: java 25 + native-image (GraalVM 25), git, terraform, python3, curl, brew (brew stage)
# Wanted: GITHUB_TOKEN, GITLAB_TOKEN, and SSH keys (agent or ~/.ssh) for GitHub and GitLab.
# Only a temp dir created by this script is written to; it is removed on exit.
# Exit status: 0 if nothing FAILed, 1 otherwise, 2 for usage / preflight errors.

# shellcheck disable=SC2329,SC2016  # functions are invoked indirectly via check / run_stage; sed patterns are literal
set -euo pipefail

ALL_STAGES="build native smoke jar brew"
STAGES=""
SKIP_NATIVE_BUILD=0
BINARY=""
TAP_REPO="https://github.com/miguelaferreira/homebrew-tools"
TAP_BRANCH="claude/tender-maxwell-wrnoym"
FORCE_BREW=0
TAP_NAME="miguelaferreira/tools"

usage() { sed -n '3,/^$/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }
die() { echo "error: $*" >&2; exit 2; }

while [ $# -gt 0 ]; do
  case "$1" in
    --only)
      [ $# -ge 2 ] || die "--only needs a value"
      STAGES="$(echo "$2" | tr ',' ' ')"; shift 2 ;;
    --skip-native-build) SKIP_NATIVE_BUILD=1; shift ;;
    --binary) [ $# -ge 2 ] || die "--binary needs a value"; BINARY="$2"; shift 2 ;;
    --tap-repo) [ $# -ge 2 ] || die "--tap-repo needs a value"; TAP_REPO="$2"; shift 2 ;;
    --tap-branch) [ $# -ge 2 ] || die "--tap-branch needs a value"; TAP_BRANCH="$2"; shift 2 ;;
    --force-brew) FORCE_BREW=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) usage >&2; die "unknown option $1" ;;
  esac
done

# Normalise the stage list (order is always the canonical one).
if [ -z "${STAGES}" ]; then STAGES="${ALL_STAGES}"; fi
SELECTED=" "
for s in ${STAGES}; do
  case "$s" in
    unit) s="build" ;;
    build|native|smoke|jar|brew) ;;
    *) die "unknown stage '$s' (valid: build,native,smoke,jar,brew)" ;;
  esac
  SELECTED="${SELECTED}${s} "
done
want() { case "${SELECTED}" in *" $1 "*) return 0 ;; *) return 1 ;; esac; }

if [ -n "${BINARY}" ]; then
  case "${BINARY}" in /*) ;; *) BINARY="${PWD}/${BINARY}" ;; esac
fi
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "${ROOT}"
[ -f VERSION ] && [ -x ./gradlew ] || die "run this from a devex-cli checkout (VERSION / gradlew not found in ${ROOT})"
VER="$(tr -d '[:space:]' < VERSION)"
[ -n "${BINARY}" ] || BINARY="${ROOT}/build/native/nativeCompile/devex"

case "$(uname -s)" in
  Darwin) OS_LABEL="macOS" ;;
  Linux) OS_LABEL="Linux" ;;
  *) OS_LABEL="$(uname -s)" ;;
esac
case "$(uname -m)" in
  x86_64|amd64) ARCH_LABEL="amd64" ;;
  arm64|aarch64) ARCH_LABEL="arm64" ;;
  *) ARCH_LABEL="$(uname -m)" ;;
esac
MAC_INTEL=0
if [ "${OS_LABEL}" = "macOS" ] && [ "${ARCH_LABEL}" != "arm64" ]; then MAC_INTEL=1; fi

# ---------------------------------------------------------------------------------------
# Plumbing: temp dir, cleanup, result bookkeeping
# ---------------------------------------------------------------------------------------

WORK="$(mktemp -d "${TMPDIR:-/tmp}/devex-e2e.XXXXXX")"
mkdir -p "${WORK}/state"
RESULTS="${WORK}/results.tsv"
: > "${RESULTS}"
FINAL_RC=0
LAST_CHECK=""

section() { printf '\n==============================================================================\n%s\n==============================================================================\n' "$*"; }
say() { echo "==> $*"; }
warn() { echo "WARNING: $*" >&2; }

sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d ' ' -f 1
  else
    shasum -a 256 "$1" | cut -d ' ' -f 1
  fi
}

# BSD and GNU sed compatible in-place edit.
sed_inplace() {
  local f="$1" expr="$2"
  sed "${expr}" "${f}" > "${f}.tmp"
  cat "${f}.tmp" > "${f}"
  rm -f "${f}.tmp"
}

cleanup() {
  local rc=$?
  trap - EXIT INT TERM
  set +e
  local pid names f
  if [ -f "${WORK}/state/server.pid" ]; then
    pid="$(cat "${WORK}/state/server.pid")"
    kill "${pid}" >/dev/null 2>&1
    wait "${pid}" >/dev/null 2>&1
  fi
  if [ -f "${WORK}/state/tap_added" ] && command -v brew >/dev/null 2>&1; then
    section "Cleanup: homebrew"
    for f in devex-cli devex-cli-jdk; do
      brew uninstall --force "${TAP_NAME}/${f}" >/dev/null 2>&1
      rm -f "$(brew --cache --formula "${TAP_NAME}/${f}" 2>/dev/null)" 2>/dev/null
    done
    if brew trust --help >/dev/null 2>&1; then brew untrust "${TAP_NAME}" >/dev/null 2>&1; fi
    brew untap "${TAP_NAME}" >/dev/null 2>&1 || brew untap --force "${TAP_NAME}"
    if [ -f "${WORK}/state/orig_tap_url" ]; then
      say "Restoring original tap ${TAP_NAME}"
      brew tap "${TAP_NAME}" "$(cat "${WORK}/state/orig_tap_url")"
      names="$(cat "${WORK}/state/orig_tap_branch" 2>/dev/null)"
      if [ -n "${names}" ] && [ "${names}" != "HEAD" ]; then
        git -C "$(brew --repository "${TAP_NAME}")" checkout "${names}"
      fi
    fi
  fi
  if [ -n "${WORK:-}" ] && [ -d "${WORK}" ]; then rm -rf "${WORK}"; fi
  exit "${rc}"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

record() { printf '%s\t%s\t%s\n' "$1" "$2" "${3:-}" >> "${RESULTS}"; }
note() { echo "$*" > "${WORK}/.note"; }

# check NAME CMD...  Runs CMD (a command or shell function) in a subshell with `set -e`,
# records PASS / FAIL (exit 77 means SKIP) and never aborts the caller. The result is
# also left in LAST_CHECK. Must be called as a plain command (never in an if / || / &&
# list), otherwise bash ignores `set -e` inside the checked function.
check() {
  local name="$1" rc st n=""
  shift
  echo
  echo "--- ${name}"
  rm -f "${WORK}/.note"
  set +e
  ( set -e; "$@" )
  rc=$?
  set -e
  case "${rc}" in
    0) st="PASS" ;;
    77) st="SKIP" ;;
    *) st="FAIL"; n="exit ${rc}" ;;
  esac
  if [ -f "${WORK}/.note" ]; then n="$(cat "${WORK}/.note")"; fi
  echo "--- ${st}: ${name}${n:+ (${n})}"
  record "${st}" "${name}" "${n}"
  LAST_CHECK="${st}"
}

# run_stage NAME FUNCTION: header + isolated execution; an unexpected abort is a FAIL.
run_stage() {
  local name="$1" fn="$2" rc
  section "Stage: ${name}"
  set +e
  ( set -e; "${fn}" )
  rc=$?
  set -e
  if [ "${rc}" -ne 0 ]; then
    record "FAIL" "${name}: stage" "aborted unexpectedly (exit ${rc})"
  fi
}

# ---------------------------------------------------------------------------------------
# Preflight
# ---------------------------------------------------------------------------------------

JAVA_BIN=""
NATIVE_IMAGE_BIN=""
MISSING=""

need() { if ! command -v "$1" >/dev/null 2>&1; then MISSING="${MISSING} $1"; fi; }

preflight() {
  section "Preflight"
  echo "Repository : ${ROOT}"
  echo "Branch     : $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?') ($(git rev-parse --short HEAD 2>/dev/null || echo '?'))"
  echo "Version    : ${VER} (VERSION file)"
  echo "OS / arch  : $(uname -s) $(uname -m)  -> release asset label ${OS_LABEL}-${ARCH_LABEL}"
  echo "Stages     : ${SELECTED}"

  need git
  need python3
  need curl

  if want build || want jar || { want native && [ "${SKIP_NATIVE_BUILD}" -eq 0 ]; }; then
    if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
      JAVA_BIN="${JAVA_HOME}/bin/java"
    else
      JAVA_BIN="$(command -v java || true)"
    fi
    if [ -z "${JAVA_BIN}" ]; then
      MISSING="${MISSING} java(25)"
    else
      local first major
      first="$("${JAVA_BIN}" -version 2>&1 | grep -m 1 ' version "' || true)"
      major="$(echo "${first}" | sed -n 's/.*"\([0-9][0-9]*\)[^"]*".*/\1/p')"
      echo "Java       : ${JAVA_BIN} -> ${first}"
      if [ "${major}" != "25" ]; then MISSING="${MISSING} java-25(found:${major:-unknown})"; fi
    fi
  fi
  if want native && [ "${SKIP_NATIVE_BUILD}" -eq 0 ]; then
    if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/native-image" ]; then
      NATIVE_IMAGE_BIN="${JAVA_HOME}/bin/native-image"
    else
      NATIVE_IMAGE_BIN="$(command -v native-image || true)"
    fi
    if [ -z "${NATIVE_IMAGE_BIN}" ]; then
      MISSING="${MISSING} native-image(GraalVM-25)"
    else
      echo "native-image: ${NATIVE_IMAGE_BIN}"
    fi
  fi
  if want build || want smoke; then need terraform; fi
  if want brew; then need brew; fi
  if want smoke || want brew || want jar; then
    if [ "${SKIP_NATIVE_BUILD}" -eq 1 ] || ! want native; then
      if want smoke || want brew; then
        if [ ! -x "${BINARY}" ]; then
          if want smoke; then
            MISSING="${MISSING} native-binary(${BINARY})"
          else
            warn "no native binary at ${BINARY}: the brew stage will skip devex-cli and test the jdk formula only"
          fi
        fi
      fi
    fi
  fi
  if [ -n "${MISSING}" ]; then
    echo >&2
    echo "Missing required tools / versions:${MISSING}" >&2
    die "preflight failed (install the above, or select fewer stages with --only)"
  fi
  command -v terraform >/dev/null 2>&1 && echo "Terraform  : $(terraform version | sed -n 1p)"
  command -v git >/dev/null 2>&1 && echo "Git        : $(git --version)"
  command -v brew >/dev/null 2>&1 && echo "Homebrew   : $(brew --version | sed -n 1p)"

  local tokens_missing=""
  if [ -z "${GITHUB_TOKEN:-}" ]; then tokens_missing="${tokens_missing} GITHUB_TOKEN"; fi
  if [ -z "${GITLAB_TOKEN:-}" ]; then tokens_missing="${tokens_missing} GITLAB_TOKEN"; fi
  if [ -n "${tokens_missing}" ]; then
    warn "not set:${tokens_missing}. The stages still run, but every test that talks to the GitHub / GitLab APIs (build, smoke, jar, brew clones) is expected to FAIL."
  fi

  local agent_rc=2
  if command -v ssh-add >/dev/null 2>&1; then
    set +e; ssh-add -l >/dev/null 2>&1; agent_rc=$?; set -e
  fi
  local keyfiles=0 k
  for k in "${HOME}"/.ssh/id_*; do
    case "${k}" in *.pub) continue ;; esac
    [ -f "${k}" ] && keyfiles=1
  done
  if [ "${agent_rc}" -ne 0 ] && [ "${keyfiles}" -eq 0 ]; then
    warn "no SSH agent identities and no ~/.ssh/id_* key files. SSH clones (build integration tests, smoke) are expected to FAIL."
  else
    echo "SSH keys   : agent identities: $([ "${agent_rc}" -eq 0 ] && echo yes || echo no), key files in ~/.ssh: $([ "${keyfiles}" -eq 1 ] && echo yes || echo no)"
  fi

  if want build && [ "${SKIP_NATIVE_BUILD}" -eq 0 ] && ! want native; then
    warn "'build' runs gradle clean, which deletes build/native; the native binary will need rebuilding."
  fi
  if want brew && [ -n "$(git status --porcelain 2>/dev/null)" ]; then
    warn "working tree is dirty. The brew stage builds the jdk formula from 'git archive HEAD', so uncommitted changes are NOT covered there."
  fi
}

# ---------------------------------------------------------------------------------------
# Shared helpers
# ---------------------------------------------------------------------------------------

# version_is CMD...: `CMD -V` must succeed and mention the version from the VERSION file.
version_is() {
  local out
  out="$("$@" -V 2>&1)" || { echo "${out}"; return 1; }
  echo "${out}"
  case "${out}" in
    *"${VER}"*) ;;
    *) echo "expected version ${VER} in the output above" >&2; return 1 ;;
  esac
}

help_works() {
  local out
  out="$("$@" -h 2>&1)" || { echo "${out}"; return 1; }
  printf '%s\n' "${out}" | sed -n '1,15p'
  [ -n "${out}" ]
}

# clone helpers: first arg is the destination, rest is the devex command.
gitlab_https_clone() {
  local dest="$1"
  shift
  "$@" gitlab clone -x -r -c HTTPS gitlab-clone-example "${dest}"
  [ -f "${dest}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]
  case "$(git -C "${dest}/gitlab-clone-example/a-project" remote get-url origin)" in
    *"https://"*) ;;
    *) echo "remote is not https" >&2; return 1 ;;
  esac
}

github_https_clone() {
  local dest="$1"
  shift
  "$@" github clone -x -c HTTPS devex-cli-example "${dest}"
  [ -f "${dest}/devex-cli-example/a-public-repository/README.md" ]
  [ -f "${dest}/devex-cli-example/a-private-repository/README.md" ]
}

# expect_fail PATTERN CMD...: CMD must fail and its output must match PATTERN (grep -Ei).
expect_fail() {
  local pattern="$1" out rc
  shift
  set +e
  out="$("$@" 2>&1)"
  rc=$?
  set -e
  echo "${out}"
  if [ "${rc}" -eq 0 ]; then echo "expected the command to fail, but it succeeded" >&2; return 1; fi
  grep -Eiq "${pattern}" <<<"${out}" || { echo "failed, but the output does not match /${pattern}/" >&2; return 1; }
}

# ---------------------------------------------------------------------------------------
# Stage: build
# ---------------------------------------------------------------------------------------

gradle_build() {
  local rc=0 stash="${WORK}/stash"
  # `clean` deletes build/, including a native binary the user asked us to reuse.
  case "${BINARY}" in
    "${ROOT}/build/"*)
      if [ "${SKIP_NATIVE_BUILD}" -eq 1 ] && [ -x "${BINARY}" ]; then
        mkdir -p "${stash}"
        cp -p "${BINARY}" "${stash}/binary"
      fi ;;
  esac
  DEVEX_SSH_INTEGRATION_TESTS=true ./gradlew --no-daemon clean build || rc=$?
  if [ -f "${stash}/binary" ]; then
    mkdir -p "$(dirname "${BINARY}")"
    cp -p "${stash}/binary" "${BINARY}"
    echo "(restored the native binary that 'clean' deleted: ${BINARY})"
  fi
  return "${rc}"
}

test_summary() {
  local dir="${ROOT}/build/test-results/test"
  python3 -I - "${dir}" <<'PY'
import glob, os, sys
import xml.etree.ElementTree as ET

d = sys.argv[1]
files = sorted(glob.glob(os.path.join(d, "*.xml")))
if not files:
    print("no test result files in %s" % d)
    sys.exit(1)
tests = failures = errors = skipped = 0
bad = []
skipped_names = []
for f in files:
    root = ET.parse(f).getroot()
    suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
    for s in suites:
        tests += int(s.get("tests", 0))
        failures += int(s.get("failures", 0))
        errors += int(s.get("errors", 0))
        skipped += int(s.get("skipped", 0))
        for c in s.findall("testcase"):
            name = "%s.%s" % (c.get("classname"), c.get("name"))
            if c.find("failure") is not None or c.find("error") is not None:
                bad.append(name)
            if c.find("skipped") is not None:
                skipped_names.append(name)
passed = tests - failures - errors - skipped
print("test classes: %d" % len(files))
print("tests: %d  passed: %d  failed: %d  errors: %d  skipped: %d" % (tests, passed, failures, errors, skipped))
for n in bad[:30]:
    print("  FAILED  " + n)
if skipped:
    print("skipped tests (SSH integration tests or missing tokens are typical):")
    for n in skipped_names[:15]:
        print("  skipped " + n)
    if len(skipped_names) > 15:
        print("  ... and %d more" % (len(skipped_names) - 15))
sys.exit(1 if (failures or errors or tests == 0) else 0)
PY
}

stage_build() {
  check "build: gradlew clean build" gradle_build
  check "build: test results summary" test_summary
}

# ---------------------------------------------------------------------------------------
# Stage: native
# ---------------------------------------------------------------------------------------

native_compile() { ./gradlew --no-daemon nativeCompile; }

native_present() {
  [ -x "${BINARY}" ] || { echo "no executable binary at ${BINARY}" >&2; return 1; }
}

native_info() {
  ls -l "${BINARY}"
  echo "size: $(du -h "${BINARY}" | cut -f 1)"
  if command -v file >/dev/null 2>&1; then
    local desc
    desc="$(file "${BINARY}")"
    echo "${desc}"
    case "${ARCH_LABEL}" in
      arm64) echo "${desc}" | grep -Eiq 'arm64|aarch64' || { echo "binary is not arm64" >&2; return 1; } ;;
      amd64) echo "${desc}" | grep -Eiq 'x86[-_]64' || { echo "binary is not x86-64" >&2; return 1; } ;;
    esac
    case "${OS_LABEL}" in
      macOS) echo "${desc}" | grep -qi 'Mach-O' || { echo "binary is not Mach-O" >&2; return 1; } ;;
      Linux) echo "${desc}" | grep -qi 'ELF' || { echo "binary is not ELF" >&2; return 1; } ;;
    esac
  else
    note "file(1) not available"
    return 77
  fi
}

stage_native() {
  if [ "${SKIP_NATIVE_BUILD}" -eq 1 ]; then
    check "native: reuse existing binary" native_present
  else
    check "native: gradlew nativeCompile" native_compile
    check "native: binary exists" native_present
  fi
  [ "${LAST_CHECK}" = "PASS" ] || return 0
  check "native: devex -V shows ${VER}" version_is "${BINARY}"
  check "native: size / architecture match this host" native_info
}

# ---------------------------------------------------------------------------------------
# Stage: smoke
# ---------------------------------------------------------------------------------------

smoke_run() {
  local stage="${WORK}/smoke" bin_dir
  rm -rf "${stage}"
  mkdir -p "${stage}/build/native" "${stage}/src/test/resources"
  # The smoke script runs `build/native/nativeCompile/devex` relative to its cwd.
  bin_dir="$(dirname "${BINARY}")"
  if [ "$(basename "${BINARY}")" = "devex" ]; then
    ln -s "${bin_dir}" "${stage}/build/native/nativeCompile"
  else
    mkdir -p "${stage}/build/native/nativeCompile"
    ln -s "${BINARY}" "${stage}/build/native/nativeCompile/devex"
  fi
  # Scratch copy of the terraform module (without any state from previous local runs).
  cp -R "${ROOT}/src/test/resources/terraform" "${stage}/src/test/resources/terraform"
  find "${stage}/src" -name .terraform -type d -prune -exec rm -rf {} + 2>/dev/null || true
  find "${stage}/src" -name 'terraform.tfstate*' -type f -exec rm -f {} + 2>/dev/null || true
  say "Running .github/scripts/run-native-binary.sh in ${stage}"
  say "(binary under test: ${BINARY}; SSH_AUTH_SOCK is kept, CI unsets it to exercise key files)"
  ( cd "${stage}" && bash "${ROOT}/.github/scripts/run-native-binary.sh" )
}

stage_smoke() {
  check "smoke: binary present" native_present
  [ "${LAST_CHECK}" = "PASS" ] || return 0
  check "smoke: run-native-binary.sh (gitlab, github, terraform)" smoke_run
}

# ---------------------------------------------------------------------------------------
# Stage: jar
# ---------------------------------------------------------------------------------------

JAR=""
devex_jar() { "${JAVA_BIN}" -jar "${JAR}" "$@"; }

jar_present() {
  [ -n "${JAR}" ] && [ -f "${JAR}" ] || { echo "no build/libs/devex-*-all.jar (run the build stage)" >&2; return 1; }
  ls -l "${JAR}"
}

stage_jar() {
  local f
  for f in "${ROOT}"/build/libs/devex-*-all.jar; do
    if [ -f "${f}" ]; then JAR="${f}"; fi
  done
  check "jar: shadow jar present" jar_present
  [ "${LAST_CHECK}" = "PASS" ] || return 0
  check "jar: devex -V shows ${VER}" version_is devex_jar
  check "jar: devex -h" help_works devex_jar
  check "jar: gitlab clone over https (with submodules)" gitlab_https_clone "${WORK}/jar-gitlab" devex_jar
  check "jar: github clone over https (devex-cli-example)" github_https_clone "${WORK}/jar-github" devex_jar
}

# ---------------------------------------------------------------------------------------
# Stage: brew
# ---------------------------------------------------------------------------------------

TAP_SRC="${WORK}/tap-src"
RELEASE_DIR="${WORK}/release"
FORMULAS="devex-cli devex-cli-jdk"
PORT=""
REPO_SLUG=""
HAVE_REAL_BINARY=0

brew_preconditions() {
  local f installed taps
  for f in ${FORMULAS}; do
    installed="$(brew list --formula --versions "${f}" 2>/dev/null || true)"
    if [ -n "${installed}" ]; then
      if [ "${FORCE_BREW}" -eq 1 ]; then
        warn "--force-brew: uninstalling ${f} (${installed}); it will NOT be reinstalled"
        brew uninstall --force "${f}"
      else
        echo "${f} is already installed; refusing to touch it (use --force-brew to uninstall it)" >&2
        return 1
      fi
    fi
  done
  taps="$(brew tap)"
  if grep -qx "${TAP_NAME}" <<<"${taps}"; then
    if [ "${FORCE_BREW}" -ne 1 ]; then
      echo "${TAP_NAME} is already tapped; refusing to replace it (use --force-brew)" >&2
      return 1
    fi
    local dir
    dir="$(brew --repository "${TAP_NAME}")"
    git -C "${dir}" remote get-url origin > "${WORK}/state/orig_tap_url"
    git -C "${dir}" rev-parse --abbrev-ref HEAD > "${WORK}/state/orig_tap_branch"
    warn "--force-brew: replacing tap ${TAP_NAME} ($(cat "${WORK}/state/orig_tap_url") @ $(cat "${WORK}/state/orig_tap_branch")); it is restored at the end"
    brew untap --force "${TAP_NAME}"
  fi
}

brew_clone_tap() {
  git clone --quiet --branch "${TAP_BRANCH}" "${TAP_REPO}" "${TAP_SRC}"
  [ -f "${TAP_SRC}/scripts/update-formula.sh" ]
  [ -f "${TAP_SRC}/Formula/devex-cli.rb" ] && [ -f "${TAP_SRC}/Formula/devex-cli-jdk.rb" ]
  git -C "${TAP_SRC}" log -1 --oneline
}

brew_make_release() {
  local asset name dir real="${OS_LABEL}-${ARCH_LABEL}" tag="v${VER}"
  dir="${RELEASE_DIR}/${REPO_SLUG}/releases/download/${tag}"
  mkdir -p "${dir}"
  for asset in Linux-amd64 Linux-arm64 macOS-arm64; do
    name="devex-${asset}-${tag}"
    if [ "${asset}" = "${real}" ] && [ "${HAVE_REAL_BINARY}" -eq 1 ]; then
      cp "${BINARY}" "${dir}/${name}"
    else
      echo "placeholder for ${asset}, not a real binary" > "${dir}/${name}"
    fi
    (cd "${dir}" && printf '%s  %s\n' "$(sha256_of "${name}")" "${name}" > "${name}.sha256sum")
  done
  mkdir -p "${RELEASE_DIR}/${REPO_SLUG}/archive/refs/tags"
  git -C "${ROOT}" archive --format=tar --prefix="devex-cli-${VER}/" HEAD | gzip -n > "${RELEASE_DIR}/${REPO_SLUG}/archive/refs/tags/${tag}.tar.gz"
  (cd "${RELEASE_DIR}" && find . -type f | sort)
}

free_port() {
  python3 -I -c 'import socket; s = socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1])'
}

start_server() {
  python3 -I -m http.server "${PORT}" --bind 127.0.0.1 --directory "${RELEASE_DIR}" > "${WORK}/http.log" 2>&1 &
  echo $! > "${WORK}/state/server.pid"
  local i=0
  until curl -fsS -o /dev/null "http://127.0.0.1:${PORT}/" 2>/dev/null; do
    i=$((i + 1))
    if [ "${i}" -ge 50 ]; then cat "${WORK}/http.log" >&2; echo "http server did not start" >&2; return 1; fi
    sleep 0.2
  done
  echo "serving ${RELEASE_DIR} on http://127.0.0.1:${PORT}/"
}

brew_update_formulas() {
  local script="${TAP_SRC}/scripts/update-formula.sh"
  # Only this temp copy is edited: point both URL bases at the local server.
  sed_inplace "${script}" 's|https://github.com/${repo}|http://127.0.0.1:'"${PORT}"'/${repo}|'
  grep -F "http://127.0.0.1:${PORT}/\${repo}/releases/download" "${script}"
  grep -F "http://127.0.0.1:${PORT}/\${repo}/archive/refs/tags" "${script}"
  (cd "${TAP_SRC}" && bash scripts/update-formula.sh "${VER}")
  git -C "${TAP_SRC}" diff --stat
  git -C "${TAP_SRC}" add Formula
  git -C "${TAP_SRC}" -c user.name=devex-e2e -c user.email=devex-e2e@localhost -c commit.gpgsign=false commit -q -m "e2e: update formulas to ${VER}"
}

brew_tap_local() {
  local taps
  brew tap "${TAP_NAME}" "${TAP_SRC}"
  # Newer brew versions only load formulae from trusted taps.
  if brew trust --help >/dev/null 2>&1; then
    brew trust "${TAP_NAME}" || echo "warning: 'brew trust ${TAP_NAME}' failed; continuing" >&2
  fi
  taps="$(brew tap)"
  grep -qx "${TAP_NAME}" <<<"${taps}"
}

brew_localise_urls() {
  local dir f
  dir="$(brew --repository "${TAP_NAME}")/Formula"
  for f in ${FORMULAS}; do
    sed_inplace "${dir}/${f}.rb" "s|https://github.com/${REPO_SLUG}/|http://127.0.0.1:${PORT}/${REPO_SLUG}/|g"
    grep -F "http://127.0.0.1:${PORT}/${REPO_SLUG}/" "${dir}/${f}.rb"
    if grep -E 'https://github.com/[^"]*/(releases|archive)/' "${dir}/${f}.rb"; then
      echo "${f}.rb still has github release / archive URLs" >&2
      return 1
    fi
  done
}

# brew_do ACTION FORMULA
brew_do() {
  local action="$1" f="$2"
  case "${action}" in
    style) brew style "${TAP_NAME}/${f}" ;;
    audit) brew audit --strict "${TAP_NAME}/${f}" ;;
    install)
      # The jdk formula is a source build; never let a stray bottle hide a broken build.
      if [ "${f}" = "devex-cli-jdk" ]; then
        brew install --build-from-source "${TAP_NAME}/${f}"
      else
        brew install "${TAP_NAME}/${f}"
      fi ;;
    test) brew test "${TAP_NAME}/${f}" ;;
    uninstall) brew uninstall "${TAP_NAME}/${f}" ;;
  esac
}

brew_devex() { "$(brew --prefix)/bin/devex" "$@"; }
brew_devex_no_java_home() { JAVA_HOME=/nonexistent "$(brew --prefix)/bin/devex" "$@"; }

brew_formula_cycle() {
  local f="$1" other
  if [ "${f}" = "devex-cli" ]; then other="devex-cli-jdk"; else other="devex-cli"; fi

  if [ "${f}" = "devex-cli" ] && [ "${MAC_INTEL}" -eq 1 ]; then
    check "brew ${f}: install is refused on macOS Intel (formula disabled)" expect_fail 'disabled' brew install "${TAP_NAME}/${f}"
    return 0
  fi
  if [ "${f}" = "devex-cli" ] && [ "${HAVE_REAL_BINARY}" -eq 0 ]; then
    record "SKIP" "brew ${f}: install, test and run" "no native binary for ${OS_LABEL}-${ARCH_LABEL}"
    return 0
  fi

  check "brew ${f}: install" brew_do install "${f}"
  if [ "${LAST_CHECK}" = "PASS" ]; then
    check "brew ${f}: brew test" brew_do test "${f}"
    check "brew ${f}: devex -V shows ${VER}" version_is brew_devex
    check "brew ${f}: github https clone with installed binary" github_https_clone "${WORK}/brew-${f}-github" brew_devex
    if [ "${f}" = "devex-cli-jdk" ]; then
      check "brew ${f}: works with JAVA_HOME=/nonexistent (launcher pins openjdk@25)" version_is brew_devex_no_java_home
    fi
    if [ "${MAC_INTEL}" -eq 1 ]; then
      record "SKIP" "brew ${f}: installing ${other} while installed conflicts" "devex-cli cannot be installed on macOS Intel"
    else
      check "brew ${f}: installing ${other} while installed fails with a conflict" expect_fail 'conflict' brew install "${TAP_NAME}/${other}"
      # In case brew did not stop at the conflict, never leave both around.
      brew uninstall --force "${TAP_NAME}/${other}" >/dev/null 2>&1 || true
    fi
    check "brew ${f}: uninstall" brew_do uninstall "${f}"
  fi
}

stage_brew() {
  export HOMEBREW_NO_AUTO_UPDATE=1 HOMEBREW_NO_INSTALL_CLEANUP=1 HOMEBREW_NO_ANALYTICS=1 HOMEBREW_NO_ENV_HINTS=1
  local f

  if [ -x "${BINARY}" ] && [ "${MAC_INTEL}" -eq 0 ]; then HAVE_REAL_BINARY=1; fi
  if [ "${HAVE_REAL_BINARY}" -eq 1 ] && [[ "$("${BINARY}" -V 2>&1 || true)" != *"${VER}"* ]]; then
    warn "${BINARY} does not report version ${VER}; it was probably not built from this tree"
  fi

  check "brew: nothing installed / tapped that we would clobber" brew_preconditions
  [ "${LAST_CHECK}" = "PASS" ] || return 0

  check "brew: clone tap ${TAP_REPO} @ ${TAP_BRANCH}" brew_clone_tap
  [ "${LAST_CHECK}" = "PASS" ] || return 0
  REPO_SLUG="$(sed -n 's/^repo="\(.*\)"$/\1/p' "${TAP_SRC}/scripts/update-formula.sh")"
  [ -n "${REPO_SLUG}" ] || { record "FAIL" "brew: read repo from update-formula.sh" "no repo= line"; return 0; }

  check "brew: build fake GitHub release tree for v${VER}" brew_make_release
  [ "${LAST_CHECK}" = "PASS" ] || return 0

  PORT="$(free_port)"
  check "brew: serve release tree on a local port" start_server
  [ "${LAST_CHECK}" = "PASS" ] || return 0

  check "brew: update-formula.sh ${VER} against the local release" brew_update_formulas
  [ "${LAST_CHECK}" = "PASS" ] || return 0

  echo "${TAP_NAME}" > "${WORK}/state/tap_added"
  check "brew: tap the local copy as ${TAP_NAME}" brew_tap_local
  [ "${LAST_CHECK}" = "PASS" ] || return 0

  # Lint the pristine formulas (brew audit rejects the http://127.0.0.1 URLs used below).
  for f in ${FORMULAS}; do
    check "brew ${f}: brew style" brew_do style "${f}"
    check "brew ${f}: brew audit --strict (offline)" brew_do audit "${f}"
  done

  check "brew: point formula URLs at the local server" brew_localise_urls
  [ "${LAST_CHECK}" = "PASS" ] || return 0

  for f in ${FORMULAS}; do
    brew_formula_cycle "${f}"
  done
}

# ---------------------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------------------

summary() {
  section "Summary"
  local st name note pass=0 fail=0 skip=0
  printf '%-6s  %-72s  %s\n' "STATUS" "CHECK" "NOTE"
  printf '%-6s  %-72s  %s\n' "------" "-----" "----"
  while IFS=$'\t' read -r st name note; do
    printf '%-6s  %-72s  %s\n' "${st}" "${name}" "${note}"
    case "${st}" in
      PASS) pass=$((pass + 1)) ;;
      FAIL) fail=$((fail + 1)) ;;
      SKIP) skip=$((skip + 1)) ;;
    esac
  done < "${RESULTS}"
  echo
  echo "PASS: ${pass}   FAIL: ${fail}   SKIP: ${skip}"
  if [ "${fail}" -gt 0 ]; then FINAL_RC=1; fi
  if [ "${fail}" -eq 0 ]; then echo "RESULT: OK"; else echo "RESULT: FAILED"; fi
}

# ---------------------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------------------

preflight

if want build; then run_stage build stage_build; fi
if want native; then run_stage native stage_native; fi
if want smoke; then run_stage smoke stage_smoke; fi
if want jar; then run_stage jar stage_jar; fi
if want brew; then run_stage brew stage_brew; fi

summary
exit "${FINAL_RC}"
