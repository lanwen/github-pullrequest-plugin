package org.jenkinsci.plugins.github.pullrequest;

import hudson.Functions;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Maintains state about a Pull Request for a particular Jenkins job.  This is what understands the current state
 * of a PR for a particular job. Instances of this class are immutable.
 * Used from {@link GitHubPRRepository}
 */
public class GitHubPRPullRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRPullRequest.class);

    private final int number;
    private final Date issueUpdatedAt;
    private String title;
    private Date prUpdatedAt;
    private String headSha;
    private String headRef;
    private Boolean mergeable;
    private String baseRef;
    private String userEmail;
    private String userLogin;
    private URL htmlUrl;
    private Set<String> labels;
    @CheckForNull
    private Date lastCommentCreatedAt;
    private String sourceRepoOwner;

    /**
     * Save only what we need for next comparison
     */
    public GitHubPRPullRequest(GHPullRequest pr) throws IOException {
        userLogin = pr.getUser().getLogin();
        number = pr.getNumber();
        prUpdatedAt = pr.getUpdatedAt();
        // https://github.com/kohsuke/github-api/issues/178
        issueUpdatedAt = pr.getIssueUpdatedAt();
        headSha = pr.getHead().getSha();
        headRef = pr.getHead().getRef();
        title = pr.getTitle();
        baseRef = pr.getBase().getRef();
        htmlUrl = pr.getHtmlUrl();

        try {
            Date maxDate = new Date(0);
            for (GHIssueComment comment : pr.getComments()) {
                if (comment.getCreatedAt().compareTo(maxDate) > 0) {
                    maxDate = comment.getCreatedAt();
                }
            }
            lastCommentCreatedAt = maxDate.getTime() == 0 ? null : new Date(maxDate.getTime());
        } catch (IOException e) {
            LOGGER.warn("Can't get comments for PR: {}", e.getMessage());
            lastCommentCreatedAt = null;
        }

        try {
            userEmail = pr.getUser().getEmail();
        } catch (Exception e) {
            LOGGER.warn("Can't get GitHub user email: {}", e.getMessage());
            userEmail = "";
        }

        GHRepository remoteRepo = pr.getRepository();

        try {
            updateLabels(remoteRepo.getIssue(number).getLabels());
        } catch (IOException e) {
            LOGGER.warn("Can't retrieve label list: {}", e.getMessage());
        }

        // see https://github.com/kohsuke/github-api/issues/111
        try {
            mergeable = pr.getMergeable();
        } catch (IOException e) {
            LOGGER.warn("Can't get mergeable status: {}", e.getMessage());
            mergeable = false;
        }
        sourceRepoOwner = remoteRepo.getOwnerName();
    }

    public int getNumber() {
        return number;
    }

    public String getHeadSha() {
        return headSha;
    }

    public boolean isMergeable() {
        return mergeable == null ? false : mergeable;
    }

    public String getBaseRef() {
        return baseRef;
    }

    public String getHeadRef() {
        return headRef;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getTitle() {
        return title;
    }

    public Set<String> getLabels() {
        return labels;
    }

    @CheckForNull
    public Date getLastCommentCreatedAt() {
        return lastCommentCreatedAt;
    }

    /**
     * URL to the Github Pull Request.
     */
    public URL getHtmlUrl() {
        return htmlUrl;
    }

    public Date getPrUpdatedAt() {
        return new Date(prUpdatedAt.getTime());
    }

    public Date getIssueUpdatedAt() {
        return issueUpdatedAt;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getSourceRepoOwner() {
        return sourceRepoOwner;
    }

    private void updateLabels(Collection<GHLabel> labels) {
        this.labels = new HashSet<>();
        for (GHLabel label : labels) {
            this.labels.add(label.getName());
        }
    }

    public String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-pull-request.svg";
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
                .append("number", number)
                .append("issueUpdatedAt", issueUpdatedAt)
                .append("title", title)
                .append("prUpdatedAt", prUpdatedAt)
                .append("headSha", headSha)
                .append("headRef", headRef)
                .append("mergeable", mergeable)
                .append("baseRef", baseRef)
                .append("userEmail", userEmail)
                .append("userLogin", userLogin)
                .append("htmlUrl", htmlUrl)
                .append("labels", labels)
                .append("lastCommentCreatedAt", lastCommentCreatedAt)
                .append("sourceRepoOwner", sourceRepoOwner).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitHubPRPullRequest that = (GitHubPRPullRequest) o;

        if (number != that.number) return false;
        if (baseRef != null ? !baseRef.equals(that.baseRef) : that.baseRef != null) return false;
        if (headRef != null ? !headRef.equals(that.headRef) : that.headRef != null) return false;
        if (headSha != null ? !headSha.equals(that.headSha) : that.headSha != null) return false;
        if (issueUpdatedAt != null ? !issueUpdatedAt.equals(that.issueUpdatedAt) : that.issueUpdatedAt != null)
            return false;
        if (labels != null ? !labels.equals(that.labels) : that.labels != null) return false;
        if (lastCommentCreatedAt != null ? !lastCommentCreatedAt.equals(that.lastCommentCreatedAt) : that.lastCommentCreatedAt != null)
            return false;
        if (mergeable != null ? !mergeable.equals(that.mergeable) : that.mergeable != null) return false;
        if (prUpdatedAt != null ? !prUpdatedAt.equals(that.prUpdatedAt) : that.prUpdatedAt != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (htmlUrl != null ? !htmlUrl.equals(that.htmlUrl) : that.htmlUrl != null) return false;
        if (userEmail != null ? !userEmail.equals(that.userEmail) : that.userEmail != null) return false;
        if (userLogin != null ? !userLogin.equals(that.userLogin) : that.userLogin != null) return false;
        if (sourceRepoOwner != null ? !sourceRepoOwner.equals(that.sourceRepoOwner) : that.sourceRepoOwner != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = number;
        result = 31 * result + (issueUpdatedAt != null ? issueUpdatedAt.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (prUpdatedAt != null ? prUpdatedAt.hashCode() : 0);
        result = 31 * result + (headSha != null ? headSha.hashCode() : 0);
        result = 31 * result + (headRef != null ? headRef.hashCode() : 0);
        result = 31 * result + (mergeable != null ? mergeable.hashCode() : 0);
        result = 31 * result + (baseRef != null ? baseRef.hashCode() : 0);
        result = 31 * result + (userEmail != null ? userEmail.hashCode() : 0);
        result = 31 * result + (userLogin != null ? userLogin.hashCode() : 0);
        result = 31 * result + (htmlUrl != null ? htmlUrl.hashCode() : 0);
        result = 31 * result + (labels != null ? labels.hashCode() : 0);
        result = 31 * result + (lastCommentCreatedAt != null ? lastCommentCreatedAt.hashCode() : 0);
        result = 31 * result + (sourceRepoOwner != null ? sourceRepoOwner.hashCode() : 0);
        return result;
    }

}
