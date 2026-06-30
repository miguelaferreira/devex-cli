package devex.git;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vavr.control.Either;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class GitCloneResultsTest {

    private static final String REPOSITORY_NAME = "some-group / a-project";
    private static final String SUCCESS_MESSAGE = "Repository 'some-group/a-project' updated.";
    private static final String GENERIC_FAILURE_MESSAGE = "Git operation failed";

    private final Logger logger = (Logger) LoggerFactory.getLogger(GitCloneResultsTest.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void log_repositoryAlreadyCloned_logsInfoSayingAlreadyCloned() {
        final Either<Throwable, Git> result = Either.left(new RepositoryAlreadyClonedException(REPOSITORY_NAME));

        GitCloneResults.log(logger, result, () -> SUCCESS_MESSAGE);

        assertThat(appender.list).singleElement()
                                 .satisfies(event -> {
                                     assertThat(event.getLevel()).isEqualTo(Level.INFO);
                                     assertThat(event.getFormattedMessage()).contains(REPOSITORY_NAME)
                                                                            .contains("already cloned");
                                 });
    }

    @Test
    void log_genericFailure_logsWarnGitOperationFailed() {
        final Either<Throwable, Git> result = Either.left(new IllegalStateException("boom"));

        GitCloneResults.log(logger, result, () -> SUCCESS_MESSAGE);

        assertThat(appender.list).singleElement()
                                 .satisfies(event -> {
                                     assertThat(event.getLevel()).isEqualTo(Level.WARN);
                                     assertThat(event.getFormattedMessage()).isEqualTo(GENERIC_FAILURE_MESSAGE);
                                 });
    }

    @Test
    void log_success_logsInfoWithSuccessMessage() {
        final Either<Throwable, Git> result = Either.right(null);

        GitCloneResults.log(logger, result, () -> SUCCESS_MESSAGE);

        assertThat(appender.list).singleElement()
                                 .satisfies(event -> {
                                     assertThat(event.getLevel()).isEqualTo(Level.INFO);
                                     assertThat(event.getFormattedMessage()).isEqualTo(SUCCESS_MESSAGE);
                                 });
    }
}
