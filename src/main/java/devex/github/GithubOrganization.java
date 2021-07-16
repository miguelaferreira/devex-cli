package devex.github;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import lombok.ToString;

@Introspected
@ToString
public class GithubOrganization {
    @JsonProperty("login")
    private String name;
    private String id;
    @JsonProperty("repos_url")
    private String reposUrl;

    @JsonCreator
    public GithubOrganization() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReposUrl() {
        return reposUrl;
    }

    public void setReposUrl(String reposUrl) {
        this.reposUrl = reposUrl;
    }
}
