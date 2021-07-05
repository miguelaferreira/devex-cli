#!/bin/bash

set -euo pipefail
IFS=$'\n\t'

say() {
  local what="$@"
  echo "==> ${what}"
}

cmd() {
  local config_output_dir="${1}"
  local args="${*:2}"
  java -agentlib:native-image-agent=config-output-dir="${config_output_dir}" -jar build/libs/devex-*-all.jar ${args}
}

cmd_gitlab() {
  local config_output_dir="${1}"
  local args="${*:2}"
  GITHUB_TOKEN="" cmd ${config_output_dir} gitlab clone ${args}
}

cmd_github() {
  local config_output_dir="${1}"
  local args="${*:2}"
  GITLAB_TOKEN="" cmd ${config_output_dir} github clone ${args}
}

test_clones_dir="test-clones"
native_image_config_dir="native-image-config"

rm -rf "${native_image_config_dir}"
mkdir "${native_image_config_dir}"

say "Asking for tool version"
cmd "${native_image_config_dir}/no-clone/version" -V

say "Asking for tool help"
cmd "${native_image_config_dir}/no-clone/help" -h

################################################
# Gitlab
################################################
local_path="${test_clones_dir}/gitlab-public-without-token-ssh-no-submodules-trace"
say "[GITLAB] Asking to clone a public group without using a token, via ssh, without submodules"
GITLAB_TOKEN="" cmd_gitlab "${native_image_config_dir}/${local_path}-1" --trace gitlab-clone-example "${local_path}"

say "[GITLAB] Asking to clone the same public group without using a token, via ssh, with submodules (effectively only initializing submodules)"
GITLAB_TOKEN="" cmd_gitlab "${native_image_config_dir}/${local_path}-2" --trace gitlab-clone-example -r "${local_path}"

local_path="${test_clones_dir}/gitlab-public-with-token-ssh-with-submodules-debug"
say "[GITLAB] Asking to clone a public group using a token, via ssh, with submodules"
cmd_gitlab "${native_image_config_dir}/${local_path}" --debug -r gitlab-clone-example "${local_path}"

local_path="${test_clones_dir}/gitlab-public-with-token-https-with-submodules-verbose"
say "[GITLAB] Asking to clone a public group using a token, via https, with submodules"
cmd_gitlab "${native_image_config_dir}/${local_path}" -v -r -c HTTPS gitlab-clone-example "${local_path}"

local_path="${test_clones_dir}/gitlab-private-without-token-ssh-with-submodules-verbose"
say "[GITLAB] Asking to clone a private group without using a token, via ssh, with submodules"
GITLAB_TOKEN="" cmd_gitlab "${native_image_config_dir}/${local_path}" -v -r gitlab-clone-example-private "${local_path}" || true

local_path="${test_clones_dir}/gitlab-public-without-token-https-with-submodules-verbose"
say "[GITLAB] Asking to clone a private group without using a token, via https, with submodules"
GITLAB_TOKEN="" cmd_gitlab "${native_image_config_dir}/${local_path}" -v -r -c HTTPS gitlab-clone-example-private "${local_path}" || true

local_path="${test_clones_dir}/gitlab-private-with-token-ssh-with-submodules-very-verbose"
say "[GITLAB] Asking to clone a private group using a token, via ssh, with submodules"
cmd_gitlab "${native_image_config_dir}/${local_path}" -x -r gitlab-clone-example-private "${local_path}"

local_path="${test_clones_dir}/gitlab-public-with-token-https-with-submodules-very-verbose"
say "[GITLAB] Asking to clone a private group using a token, via https, with submodules"
cmd_gitlab "${native_image_config_dir}/${local_path}" -x -r -c HTTPS gitlab-clone-example-private "${local_path}"

local_path="${test_clones_dir}/gitlab-public-by-id-with-token-very-verbose"
say "[GITLAB] Asking to clone a public group by id using a token, via https, with submodules"
cmd_gitlab "${native_image_config_dir}/${local_path}" -x -m id -r -c HTTPS 11961707 "${local_path}"

local_path="${test_clones_dir}/gitlab-public-sub-group-by-full-path-with-token-very-verbose"
say "[GITLAB] Asking to clone a public subgroup by full path using a token, via https, with submodules"
cmd_gitlab "${native_image_config_dir}/${local_path}" -x -m full_path -r -c HTTPS gitlab-clone-example/sub-group-2/sub-group-3 "${local_path}"

################################################
# Github
################################################
local_path="${test_clones_dir}/github-org-without-token-ssh-no-submodules-trace"
say "[GITHUB] Asking to clone an organization without using a token, via ssh, without submodules"
GITHUB_TOKEN="" cmd_github "${native_image_config_dir}/${local_path}-1" --trace devex-cli-example "${local_path}"

local_path="${test_clones_dir}/github-org-with-token-ssh-with-submodules-trace"
say "[GITHUB] Asking to clone an organization using a token, via ssh, with submodules"
cmd_github "${native_image_config_dir}/${local_path}-1" --trace -r devex-cli-example "${local_path}"

local_path="${test_clones_dir}/github-org-without-token-https-without-submodules-debug"
say "[GITLAB] Asking to clone an organization using a token, via https, with submodules"
GITHUB_TOKEN="" cmd_github "${native_image_config_dir}/${local_path}" --debug -c HTTPS devex-cli-example "${local_path}"

local_path="${test_clones_dir}/github-org-with-token-https-with-submodules-debug"
say "[GITLAB] Asking to clone an organization using a token, via https, with submodules"
cmd_github "${native_image_config_dir}/${local_path}" --debug -c HTTPS -r devex-cli-example "${local_path}"
