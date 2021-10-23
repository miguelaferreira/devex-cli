package devex.terraform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.vavr.collection.List;
import lombok.ToString;

@ToString
@Introspected
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
    @Introspected
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TerraformResource {

        @JsonIgnore
        private String module = "";
        private ResourceMode mode;
        private String type;
        private String name;

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public ResourceMode getMode() {
            return mode;
        }

        public void setMode(ResourceMode mode) {
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

    private static enum ResourceMode {DATA, MANAGED}
}
