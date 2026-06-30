package devex.github;

import io.micronaut.http.client.exceptions.HttpClientResponseException;

import static org.junit.jupiter.api.Assertions.fail;

public class GithubTestHelper {

    static final String RATE_LIMIT_EXCEEDED_MESSAGE = "API rate limit exceeded";
    static final String RATE_LIMIT_SKIP_MESSAGE = "Test skipped due to API rate limit exception";

    static void handleException(HttpClientResponseException e) {
        if (isRateLimitExceeded(e.getMessage())) {
            System.out.println(RATE_LIMIT_SKIP_MESSAGE);
        } else {
            fail(e);
        }
    }

    static boolean isRateLimitExceeded(String message) {
        return message != null && message.contains(RATE_LIMIT_EXCEEDED_MESSAGE);
    }
}
