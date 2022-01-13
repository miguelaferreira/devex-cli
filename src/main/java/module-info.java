module devex {
    requires static lombok;
    requires logback.classic;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires io.vavr;
    requires io.vavr.jackson;
    requires io.micronaut.http_client;
    requires io.micronaut.runtime;
    requires io.micronaut.inject;
    requires io.micronaut.http;
    requires io.micronaut.http_client_core;
    requires reactor.core;
    requires jakarta.inject;
    requires info.picocli;
    requires org.eclipse.jgit;
    // merged with org.eclipse.jgit to prevent split package
    // requires org.eclipse.jgit.ssh.jsch;
    requires io.micronaut.core;
    requires org.reactivestreams;
    requires io.micronaut.picocli.picocli;
    requires logback.core;
    requires commons.exec;

    exports devex;
    exports devex.git;
    exports devex.gitlab;
    exports devex.github;
    exports devex.terraform;
}
