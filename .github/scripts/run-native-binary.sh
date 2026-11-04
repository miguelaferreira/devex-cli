#!/bin/bash

set -euo pipefail
IFS=$'\n\t'

say() {
  local what="$*"
  echo "==> ${what}"
}

# Assertions use `[[ ... ]] || fail ...` instead of relying on `set -e`: macOS ships
# bash 3.2 as /bin/bash, and before bash 4.1 a failing `[[ ]]` does not trigger errexit,
# so bare `[[ ]]` checks silently passed on the macOS release runner.
fail() {
  echo "ASSERTION FAILED (line ${1}): ${2}" >&2
  exit 1
}

say "Asking for tool version"
build/native/nativeCompile/devex -V

say "Asking for tool help"
build/native/nativeCompile/devex -h

################################################
# Gitlab
################################################
local_path="gitlab-ssh-no-submodules"
say "Asking to clone group, via ssh, without submodules"
build/native/nativeCompile/devex gitlab clone -x gitlab-clone-example "${local_path}"
[[ ! -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]] || fail "${LINENO}" "! -f \"\${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md\""

say "Asking to clone the same group, via ssh, with submodules (effectively only initializing submodules)"
build/native/nativeCompile/devex gitlab clone -x -r gitlab-clone-example "${local_path}"
[[ -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]] || fail "${LINENO}" "-f \"\${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md\""
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"git@"* ]] || fail "${LINENO}" "\"\$(git remote -v | head -n 1)\" == *\"git@\"*"
cd -

local_path="gitlab-ssh-with-submodules"
say "Asking to clone group, via ssh, with submodules"
build/native/nativeCompile/devex gitlab clone -x -r gitlab-clone-example "${local_path}"
[[ -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]] || fail "${LINENO}" "-f \"\${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md\""
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"git@"* ]] || fail "${LINENO}" "\"\$(git remote -v | head -n 1)\" == *\"git@\"*"
cd -

local_path="gitlab-https-with-submodules"
say "Asking to clone group, via https, with submodules"
build/native/nativeCompile/devex gitlab clone -x -r -c HTTPS gitlab-clone-example "${local_path}"
[[ -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]] || fail "${LINENO}" "-f \"\${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md\""
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"https://"* ]] || fail "${LINENO}" "\"\$(git remote -v | head -n 1)\" == *\"https://\"*"
cd -

local_path="gitlab-https-by-id"
say "Asking to clone group by id"
build/native/nativeCompile/devex gitlab clone -x -r -c HTTPS -m id 11961707 "${local_path}"
[[ -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]] || fail "${LINENO}" "-f \"\${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md\""
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"https://"* ]] || fail "${LINENO}" "\"\$(git remote -v | head -n 1)\" == *\"https://\"*"
cd -

local_path="gitlab-ssh-by-full-path"
say "Asking to clone group by full path"
build/native/nativeCompile/devex gitlab clone -x -m full_path gitlab-clone-example/sub-group-2/sub-group-3 "${local_path}"
[[ -f "${local_path}/gitlab-clone-example/sub-group-2/sub-group-3/another-project/README.md" ]] || fail "${LINENO}" "-f \"\${local_path}/gitlab-clone-example/sub-group-2/sub-group-3/another-project/README.md\""

################################################
# Github
################################################
local_path="github-ssh-no-submodules"
say "Asking to clone organization, via ssh, without submodules"
build/native/nativeCompile/devex github clone -x devex-cli-example "${local_path}"
[[ -f "${local_path}/devex-cli-example/a-private-repository/README.md" ]] || fail "${LINENO}" "-f \"\${local_path}/devex-cli-example/a-private-repository/README.md\""
[[ -f "${local_path}/devex-cli-example/a-public-repository/README.md" ]] || fail "${LINENO}" "-f \"\${local_path}/devex-cli-example/a-public-repository/README.md\""

local_path="github-https-with-submodules"
say "Asking to clone organization, via https, with submodules"
build/native/nativeCompile/devex github clone -x -r -c HTTPS devex-cli-example "${local_path}"
[[ -f "${local_path}/devex-cli-example/a-private-repository/README.md" ]] || fail "${LINENO}" "-f \"\${local_path}/devex-cli-example/a-private-repository/README.md\""
[[ -f "${local_path}/devex-cli-example/a-public-repository/README.md" ]] || fail "${LINENO}" "-f \"\${local_path}/devex-cli-example/a-public-repository/README.md\""
cd "${local_path}/devex-cli-example/a-private-repository"
[[ "$(git remote -v | head -n 1)" == *"https://"* ]] || fail "${LINENO}" "\"\$(git remote -v | head -n 1)\" == *\"https://\"*"
cd -

################################################
# Terraform
################################################
terraform_module_dir="src/test/resources/terraform/module1"
repo_root="$(pwd)"

# Make sure the applied resources are destroyed even if a later step fails.
# Registered before `terraform apply` so a partially applied state is cleaned up too.
destroy_terraform() {
  local exit_code=$?
  trap - EXIT
  say "[TERRAFORM] Destroying resources"
  (cd "${repo_root}/${terraform_module_dir}" && terraform destroy -auto-approve) || { [[ ${exit_code} -ne 0 ]] || exit_code=1; }
  exit "${exit_code}"
}
trap destroy_terraform EXIT

cd "${terraform_module_dir}"
terraform init -input=false
terraform apply -auto-approve
cd -

say "[TERRAFORM] Asking to taint secrets"
TERRAFORM_SECRETS="random_string" build/native/nativeCompile/devex terraform taint-secrets "${terraform_module_dir}"
say "[TERRAFORM] Asking to untaint secrets"
TERRAFORM_SECRETS="random_string" build/native/nativeCompile/devex terraform taint-secrets -u "${terraform_module_dir}"

