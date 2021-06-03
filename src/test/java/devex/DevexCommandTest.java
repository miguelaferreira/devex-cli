package devex;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class DevexCommandTest {

    @Test
    public void testVersion() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"-V"};
            DevexCommand.execute(ctx, args);

            assertThat(baos.toString()).startsWith(new DevexCommand.AppVersionProvider().getVersionText());
        }
    }

    @Test
    public void testHelp() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"-h"};
            DevexCommand.execute(ctx, args);

            final String output = baos.toString();
            assertThat(output).contains("Developer experience tools, saving time by automating gruntwork.")
                              .contains("Description:")
                              .contains("The devex cli it a collection of tools aimed at automating day to day tasks")
                              .contains("Options:")
                              .contains("Commands:")
                              .contains("Copyright(c) 2021 - Miguel Ferreira - GitHub/GitLab: @miguelaferreira");
        }
    }
}
