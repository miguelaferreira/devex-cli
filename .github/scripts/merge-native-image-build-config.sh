#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

say() {
  what="$@"
  echo "==> ${what}"
}

os="${1:-darwin}"

native_image_config_dir="native-image-config"
output_dir="src/main/resources/META-INF/native-image"

rm -rf "${output_dir}"
mkdir -p "${output_dir}"

[[ -d "${native_image_config_dir}" ]] || exit 1


say "Merging native-image build config"
input_dirs=""
for config_dir in "${native_image_config_dir}"/*/*; do
  input_dirs+="--input-dir=${config_dir} "
done

eval "graalvm/bin/native-image-configure-${os}" generate "${input_dirs}" --output-dir="${output_dir}"
