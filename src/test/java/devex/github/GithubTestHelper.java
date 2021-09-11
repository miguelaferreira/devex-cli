package devex.github;

import io.micronaut.http.client.exceptions.HttpClientResponseException;

import static org.junit.jupiter.api.Assertions.fail;

public class GithubTestHelper {

    static void handleException(HttpClientResponseException e) {
        if (e.getMessage().contains("API rate limit exceeded")) {
            System.out.println("Test skipped due to API rate limit exception");
        } else {
            fail(e);
        }
    }
}
