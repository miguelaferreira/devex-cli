resource "random_string" "some_string" {
  length = 10
}

resource "random_password" "some_password" {
  length = 12
}

resource "random_id" "some_id" {
  byte_length = 16
}

module "sub_module" {
  source = "./sub-modules/sub-module"
}

data "local_file" "this" {
  filename = "${path.module}/main.tf"
}

output "foo" {
  value = "foo"
}

output "bar" {
  value = "bar"
}
