package nl.bjovin.enforcer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

@Named("requireFilesContain")
public class RequireFilesContain extends AbstractEnforcerRule {
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RESET = "\u001B[0m";

    private boolean fail;
    @Inject
    private MavenProject project;
    @Inject
    private List<String> directories;
    @Inject
    private String pattern;

    @Override
    public void execute() throws EnforcerRuleException {
        if (directories.isEmpty()) return;

        List<String> results = new ArrayList<>();
        directories.forEach(dir -> {
            try (Stream<Path> paths = Files.walk(Paths.get(project.getBasedir().getAbsolutePath(), dir))) {
                paths.forEach(path -> {
                    if (Files.isDirectory(path)) return;

                    try {
                        boolean match = Files.readAllLines(path).stream()
                                .anyMatch(line -> line.contains(pattern));
                        if (!match) results.add(path.toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        if (!results.isEmpty()) {
            System.out.println("["+ ANSI_YELLOW + "WARNING" + ANSI_RESET + "] Not all files contain pattern: " + pattern);
            results.stream().sorted().forEach(System.out::println);
            if (fail) throw new EnforcerRuleException("Not all files contain pattern: " + pattern);
        }
    }

    @Override
    public String toString() {
        return "RequireFilesContain[" +
                "directories=" + directories +
                ", pattern='" + pattern + '\'' +
                ']';
    }
}
