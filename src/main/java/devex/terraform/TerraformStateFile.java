package devex.terraform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import lombok.ToString;

@ToString
public class TerraformStateFile {

    private final File stateFile;

    public TerraformStateFile(final String content) throws IOException {
        stateFile = File.createTempFile("terraform-state", ".tfstate");
        stateFile.deleteOnExit();
        Files.writeString(stateFile.toPath(), content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
