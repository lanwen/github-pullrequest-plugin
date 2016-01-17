package org.jenkinsci.plugins.github.pullrequest;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.asFullRepoName;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubPRRepositoryFactory extends TransientProjectActionFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRRepositoryFactory.class);

    @Override
    public Collection<? extends Action> createFor(AbstractProject project) {
        try {
            if (project.getTrigger(GitHubPRTrigger.class) != null) {
                return Collections.singleton(forProject(project));
            }
        } catch (Exception e) {
            LOGGER.warn("Bad configured project {} - {}", project.getFullName(), e.getMessage());
        }

        return Collections.emptyList();
    }

    @Nonnull
    private static GitHubPRRepository forProject(AbstractProject<?, ?> job) {
        XmlFile configFile = new XmlFile(new File(job.getRootDir(), GitHubPRRepository.FILE));

        GitHubPRTrigger trigger = job.getTrigger(GitHubPRTrigger.class);
        GitHubRepositoryName repo = trigger.getRepoFullName(job);

        GithubProjectProperty property = job.getProperty(GithubProjectProperty.class);
        String githubUrl = property.getProjectUrl().toString();
        GitHubPRRepository localRepository;
        if (configFile.exists()) {
            try {
                localRepository = (GitHubPRRepository) configFile.read();
            } catch (IOException e) {
                LOGGER.info("Can't read saved repository, creating new one", e);
                localRepository = new GitHubPRRepository(asFullRepoName(repo), githubUrl, 
                        new HashMap<Integer, GitHubPRPullRequest>());
            }
        } else {
            localRepository = new GitHubPRRepository(asFullRepoName(repo), githubUrl, 
                    new HashMap<Integer, GitHubPRPullRequest>());
        }

        localRepository.setProject(job);
        localRepository.setConfigFile(configFile);
        return localRepository;
    }

}
