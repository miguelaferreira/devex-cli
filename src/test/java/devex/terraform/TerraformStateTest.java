package devex.terraform;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.jackson.datatype.VavrModule;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TerraformStateTest {

    @Test
    void parse() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new VavrModule());
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        final TerraformState terraformState = mapper.readValue(new File("src/test/resources/terraform/terraform.tfstate"), TerraformState.class);

        assertThat(terraformState).isNotNull();
        assertThat(terraformState.getTerraformVersion()).isEqualTo("1.0.8");
        VavrAssertions.assertThat(terraformState.getResources()).isNotEmpty().hasSizeGreaterThan(1);
    }
}
