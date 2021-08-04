package devex.git;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.OpenSSHConfig;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Objects;

@Slf4j
public class OverrideJschConfigSessionFactory extends JschConfigSessionFactory {

    static {
        JSch.setConfig("signature.rsa", JSch.getConfig("ssh-rsa"));
        JSch.setLogger(new JSCHLogger());
    }

    @Override
    protected JSch getJSch(OpenSshConfig.Host hc, FS fs) throws JSchException {
        final JSch jSch = super.getJSch(hc, fs);
        final String separator = FileSystems.getDefault().getSeparator();
        String configFilePath = Objects.requireNonNullElse(System.getProperty("user.home"), ".") + separator + ".ssh" + separator + "config";
        try {
            // jGit uses a patched config repository (in super.getJSch(hc, fs)) that prevents proper loading of the ssh config,
            // so, we re-set it to an implementation that works
            // patched repository: org.eclipse.jgit.transport.JschBugFixingConfigRepository
            jSch.setConfigRepository(OpenSSHConfig.parseFile(configFilePath));
        } catch (IOException e) {
            log.warn("Could not load SSH config file at " + configFilePath, e);
        }
        return jSch;
    }

    @Override
    protected void configure(OpenSshConfig.Host hc, Session session) {
        session.setConfig("PreferredAuthentications", "publickey");
    }
}
