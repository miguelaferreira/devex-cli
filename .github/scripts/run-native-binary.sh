#!/bin/bash

set -euo pipefail
IFS=$'\n\t'

say() {
  local what="$@"
  echo "==> ${what}"
}

say "Asking for tool version"
build/native-image/application -V

say "Asking for tool help"
build/native-image/application -h

################################################
# Gitlab
################################################
local_path="gitlab-ssh-no-submodules"
say "Asking to clone group, via ssh, without submodules"
build/native-image/application gitlab clone -x gitlab-clone-example "${local_path}"
[[ ! -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]]

say "Asking to clone the same group, via ssh, with submodules (effectively only initializing submodules)"
build/native-image/application gitlab clone -x -r gitlab-clone-example "${local_path}"
[[ -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]]
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"git@"* ]]
cd -

local_path="gitlab-ssh-with-submodules"
say "Asking to clone group, via ssh, with submodules"
build/native-image/application gitlab clone -x -r gitlab-clone-example "${local_path}"
[[ -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]]
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"git@"* ]]
cd -

local_path="gitlab-https-with-submodules"
say "Asking to clone group, via https, with submodules"
build/native-image/application gitlab clone -x -r -c HTTPS gitlab-clone-example "${local_path}"
[[ -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]]
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"https://"* ]]
cd -

local_path="gitlab-https-by-id"
say "Asking to clone group by id"
build/native-image/application gitlab clone -x -r -c HTTPS -m id 11961707 "${local_path}"
[[ -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]]
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"https://"* ]]
cd -

local_path="gitlab-ssh-by-full-path"
say "Asking to clone group by full path"
build/native-image/application gitlab clone -x -m full_path gitlab-clone-example/sub-group-2/sub-group-3 "${local_path}"
[[ -f "${local_path}/gitlab-clone-example/sub-group-2/sub-group-3/another-project/README.md" ]]

################################################
# Github
################################################
local_path="github-ssh-no-submodules"
say "Asking to clone organization, via ssh, without submodules"
build/native-image/application github clone -x devex-cli-example "${local_path}"
[[ -f "${local_path}/devex-cli-example/a-private-repository/README.md" ]]
[[ -f "${local_path}/devex-cli-example/a-public-repository/README.md" ]]

local_path="github-https-with-submodules"
say "Asking to clone organization, via https, with submodules"
build/native-image/application github clone -x -r -c HTTPS devex-cli-example "${local_path}"
[[ -f "${local_path}/devex-cli-example/a-private-repository/README.md" ]]
[[ -f "${local_path}/devex-cli-example/a-public-repository/README.md" ]]
cd "${local_path}/devex-cli-example/a-private-repository"
[[ "$(git remote -v | head -n 1)" == *"https://"* ]]
cd -
