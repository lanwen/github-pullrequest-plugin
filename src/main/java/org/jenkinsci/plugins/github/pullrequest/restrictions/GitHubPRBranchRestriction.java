package org.jenkinsci.plugins.github.pullrequest.restrictions;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Restriction by target branch (one or many).
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRBranchRestriction implements Describable<GitHubPRBranchRestriction> {
    private static final Logger LOGGER = Logger.getLogger(GitHubPRBranchRestriction.class.getName());

    private final String targetBranch;
    private final Set<String> targetBranchList;

    @DataBoundConstructor
    public GitHubPRBranchRestriction(String targetBranch) {
        this.targetBranch = targetBranch.trim();
        //TODO check if System.lineSeparator() is correct separator
        this.targetBranchList = new HashSet<String>(Arrays.asList(targetBranch.split(System.lineSeparator())));
        targetBranchList.remove("");
    }

    public boolean isBranchBuildAllowed(GHPullRequest remotePR) {
        String branchName = remotePR.getBase().getRef();
        //if allowed branch list is empty, it's allowed to build any branch
        boolean isAllowed = targetBranchList.isEmpty();

        for (String targetBranch : targetBranchList) {
            //if branch name matches to pattern, allow build
            isAllowed = Pattern.compile(targetBranch).matcher(branchName).matches();
            if (isAllowed) {
                break;
            }
        }

        LOGGER.log(Level.FINEST, "Target branch {0} is {1} in our whitelist of target branches",
                new Object[]{branchName, (isAllowed ? "" : "not ")});
        return isAllowed;
    }

    public Set<String> getTargetBranchList() {
        return targetBranchList;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public Descriptor<GitHubPRBranchRestriction> getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(GitHubPRBranchRestriction.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubPRBranchRestriction> {
        @Override
        public String getDisplayName() {
            return "Target branch";
        }
    }
}