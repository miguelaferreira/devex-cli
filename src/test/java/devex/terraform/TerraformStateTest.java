package devex.terraform;

import io.micronaut.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TerraformStateTest {

    private static final Path FIXTURE = Path.of("src/test/resources/terraform/terraform.tfstate");

    @Test
    void parse() throws IOException {
        final JsonMapper mapper = JsonMapper.createDefault();

        final TerraformState terraformState = mapper.readValue(Files.readAllBytes(FIXTURE), TerraformState.class);

        assertThat(terraformState).isNotNull();
        assertThat(terraformState.getTerraformVersion()).isEqualTo("1.0.8");
        assertThat(terraformState.getResources()).isNotEmpty().hasSizeGreaterThan(1);
    }
}
