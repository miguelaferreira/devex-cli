package devex.terraform;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
public record TerraformState(
        @JsonProperty("terraform_version") String terraformVersion,
        List<TerraformResource> resources
) {

    @Serdeable
    record TerraformResource(String mode, String type, String name) {
    }
}
