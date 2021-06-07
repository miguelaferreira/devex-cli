#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

say() {
  what="$@"
  echo "==> ${what}"
}

cmd() {
  config_output_dir="${1}"
  args="${*:2}"
  java -agentlib:native-image-agent=config-output-dir="${config_output_dir}" -jar build/libs/devex-*-all.jar gitlab clone ${args}
}

test_clones_dir="test-clones"
native_image_config_dir="native-image-config"

rm -rf "${native_image_config_dir}"
mkdir "${native_image_config_dir}"

say "Asking for tool version"
cmd "${native_image_config_dir}/no-clone/version" -V

say "Asking for tool help"
cmd "${native_image_config_dir}/no-clone/help" -h

local_path="${test_clones_dir}/public-without-token-ssh-no-submodules-trace"
say "Asking to clone a public group without using a token, via ssh, without submodules"
GITLAB_TOKEN="" cmd "${native_image_config_dir}/${local_path}-1" --trace gitlab-clone-example "${local_path}"

say "Asking to clone the same public group without using a token, via ssh, with submodules (effectively only initializing submodules)"
GITLAB_TOKEN="" cmd "${native_image_config_dir}/${local_path}-2" --trace gitlab-clone-example -r "${local_path}"

local_path="${test_clones_dir}/public-with-token-ssh-with-submodules-debug"
say "Asking to clone a public group using a token, via ssh, with submodules"
cmd "${native_image_config_dir}/${local_path}" --debug -r gitlab-clone-example "${local_path}"

local_path="${test_clones_dir}/public-with-token-https-with-submodules-verbose"
say "Asking to clone a public group using a token, via https, with submodules"
cmd "${native_image_config_dir}/${local_path}" -v -r -c HTTPS gitlab-clone-example "${local_path}"

local_path="${test_clones_dir}/private-without-token-ssh-with-submodules-verbose"
say "Asking to clone a private group without using a token, via ssh, with submodules"
GITLAB_TOKEN="" cmd "${native_image_config_dir}/${local_path}" -v -r gitlab-clone-example-private "${local_path}" || true

local_path="${test_clones_dir}/public-without-token-https-with-submodules-verbose"
say "Asking to clone a private group without using a token, via https, with submodules"
GITLAB_TOKEN="" cmd "${native_image_config_dir}/${local_path}" -v -r -c HTTPS gitlab-clone-example-private "${local_path}" || true

local_path="${test_clones_dir}/private-with-token-ssh-with-submodules-very-verbose"
say "Asking to clone a private group using a token, via ssh, with submodules"
cmd "${native_image_config_dir}/${local_path}" -x -r gitlab-clone-example-private "${local_path}"

local_path="${test_clones_dir}/public-with-token-https-with-submodules-very-verbose"
say "Asking to clone a private group using a token, via https, with submodules"
cmd "${native_image_config_dir}/${local_path}" -x -r -c HTTPS gitlab-clone-example-private "${local_path}"

local_path="${test_clones_dir}/public-by-id-with-token-very-verbose"
say "Asking to clone a public group by id using a token, via https, with submodules"
cmd "${native_image_config_dir}/${local_path}" -x -m id -r -c HTTPS 11961707 "${local_path}"

local_path="${test_clones_dir}/public-sub-group-by-full-path-with-token-very-verbose"
say "Asking to clone a public subgroup by full path using a token, via https, with submodules"
cmd "${native_image_config_dir}/${local_path}" -x -m full_path -r -c HTTPS gitlab-clone-example/sub-group-2/sub-group-3 "${local_path}"
