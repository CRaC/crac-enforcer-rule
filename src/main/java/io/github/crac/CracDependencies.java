package io.github.crac;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
import org.apache.maven.enforcer.rules.dependency.BannedDependencies;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Named("cracDependencies")
public class CracDependencies extends AbstractStandardEnforcerRule {

    private static final String LATEST_REPLACEMENTS = "https://raw.githubusercontent.com/CRaC/crac-enforcer-rule/main/src/main/resources/replacements.txt";
    private static final String BANNED = "banned via the exclude/include list";
    private static final Pattern ARTIFACT_EXTRACTOR = Pattern.compile("\\s*(\\S*)");

    boolean offline;

    List<String> allowedArtifacts;

    @Inject
    private BannedDependencies innerRule;

    private final Map<String, String> replacements = new HashMap<>();

    private void init() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("replacements.txt")) {
            parseReplacements(stream);
        } catch (IOException e) {
            getLog().error("Cannot read replacements information!");
        }
        if (!offline) {
            try (InputStream stream = new BufferedInputStream(new URL(LATEST_REPLACEMENTS).openStream())) {
                parseReplacements(stream);
            } catch (IOException e) {
                getLog().warn("Cannot fetch latest replacements information: " + e.getMessage());
            }
        }
    }

    private void parseReplacements(InputStream stream) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("--->", 2);
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    replacements.put(parts[0].trim(), parts[1].trim());
                } else {
                    replacements.put(parts[0].trim(), "NO REPLACEMENT AVAILABLE");
                }
            }
        }
    }

    @Override
    public void execute() throws EnforcerRuleException {
        if (replacements.isEmpty()) {
            init();
        }
        innerRule.setExcludes(new ArrayList<>(replacements.keySet()));
        innerRule.setIncludes(allowedArtifacts);
        try {
            innerRule.execute();
        } catch (EnforcerRuleException e) {
            String newMessage = e.getMessage().lines().map(line -> {
                int index = line.indexOf(BANNED);
                if (index < 0) {
                    return line;
                }
                Matcher matcher = ARTIFACT_EXTRACTOR.matcher(line);
                if (!matcher.find()) {
                    getLog().error("Cannot parse artifact from: " + line);
                    return line;
                }
                String artifact = matcher.group(1);
                if (artifact == null) {
                    getLog().error("Cannot parse artifact from: " + line);
                    return line;
                }
                String key = replacements.keySet().stream().reduce((a1, a2) -> {
                  int l1 = findCommon(a1, artifact);
                  int l2 = findCommon(a2, artifact);
                  return l1 < l2 ? a2 : a1;
                }).orElseThrow();
                return line.substring(0, index) + "replace with " + replacements.get(key);
            }).collect(Collectors.joining("\n"));
            throw new EnforcerRuleException(newMessage);
        }
    }

    private int findCommon(String s1, String s2) {
        for (int i = 0; i < s1.length() && i < s2.length(); ++i) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }
        return 0;
    }
}
