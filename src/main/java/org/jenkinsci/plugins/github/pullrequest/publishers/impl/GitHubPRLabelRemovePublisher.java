package org.jenkinsci.plugins.github.pullrequest.publishers.impl;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.publishers.GitHubPRAbstractPublisher;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.kohsuke.github.GHLabel;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements removing of labels (one or many) from GitHub.
 *
 * @author Alina Karpovich
 * @author Kanstantsin Shautsou
 */
public class GitHubPRLabelRemovePublisher extends GitHubPRAbstractPublisher {
    private static final Logger LOGGER = Logger.getLogger(GitHubPRLabelRemovePublisher.class.getName());

    private GitHubPRLabel labelProperty;

    @DataBoundConstructor
    public GitHubPRLabelRemovePublisher(GitHubPRLabel labelProperty, StatusVerifier statusVerifier, PublisherErrorHandler errorHandler) {
        super(statusVerifier, errorHandler);
        setLabelProperty(labelProperty);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        if (getStatusVerifier() != null && !getStatusVerifier().isRunAllowed(build)) {
            return true;
        }
        populate(build, launcher, listener);
        try {
            HashSet<String> remoteLabels = new HashSet<String>();
            for (GHLabel label : getGhIssue().getLabels()) { //remote labels List -> Set
                remoteLabels.add(label.getName());
            }
            remoteLabels.removeAll(getLabelProperty().getLabelsSet());
            // TODO print only really removing
            listener.getLogger().println("Removing labels: " + getLabelProperty().getLabelsSet());
            getGhIssue().setLabels(remoteLabels.toArray(new String[remoteLabels.size()]));
        } catch (IOException ex) {
            listener.getLogger().println("Couldn't remove label for PR #" + getNumber() + " " + ex.getMessage());
            LOGGER.log(Level.SEVERE, "Couldn't remove label for PR #" + getNumber(), ex);
            handlePublisherError(build);
        }
        return true;
    }

    public GitHubPRLabel getLabelProperty() {
        return labelProperty;
    }

    public void setLabelProperty(GitHubPRLabel labelProperty) {
        this.labelProperty = labelProperty;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPRAbstractPublisher.DescriptorImpl {
        @Override
        public String getDisplayName() {
            return "GitHub PR: remove labels";
        }
    }
}