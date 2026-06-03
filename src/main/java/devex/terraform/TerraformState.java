package devex.terraform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;
import lombok.ToString;

import java.util.List;

@ToString
@Serdeable
@JsonIgnoreProperties(ignoreUnknown = true)
public class TerraformState {

    @JsonProperty("terraform_version")
    private String terraformVersion;
    private List<TerraformResource> resources;

    public String getTerraformVersion() {
        return terraformVersion;
    }

    public void setTerraformVersion(String terraformVersion) {
        this.terraformVersion = terraformVersion;
    }

    public List<TerraformResource> getResources() {
        return resources;
    }

    public void setResources(List<TerraformResource> resources) {
        this.resources = resources;
    }

    @ToString
    @Serdeable
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TerraformResource {

        @JsonIgnore
        private String module = "";
        private String mode;
        private String type;
        private String name;

        public TerraformResource() {
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
