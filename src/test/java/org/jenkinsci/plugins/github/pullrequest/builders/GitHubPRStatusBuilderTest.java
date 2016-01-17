package org.jenkinsci.plugins.github.pullrequest.builders;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.ItemGroup;
import hudson.model.Project;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHRepository;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * @author Alina Karpovich
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRStatusBuilderTest {
    private static final String DEFAULT_MESSAGE = "$GITHUB_PR_COND_REF run started";
    private static final String CUSTOM_MESSAGE = "Custom run message";

    @Mock
    private AbstractBuild<?, ?> build;
    @Mock
    private Launcher launcher;
    @Mock
    private BuildListener listener;
    @Mock
    private Project project;
    @Mock
    private GitHubPRTrigger trigger;
    @Mock
    private GitHubPRCause cause;
    @Mock
    private PrintStream logger;
    @Mock
    private GitHubPRTrigger.DescriptorImpl triggerDescriptor;
    @Mock
    private GHRepository remoteRepository;
    @Mock
    private GitHubPRMessage message;
    @Mock
    private ItemGroup itemGroup;

    @Test
    public void createBuilder() {
        GitHubPRStatusBuilder builder = new GitHubPRStatusBuilder();

        assertEquals(DEFAULT_MESSAGE, builder.getStatusMessage().getContent());
    }

    @Test
    public void createBuilderWithNullMessage() {
        GitHubPRStatusBuilder builder = new GitHubPRStatusBuilder(null);

        assertEquals(DEFAULT_MESSAGE, builder.getStatusMessage().getContent());
    }

    @Test
    public void createBuilderWithNullMessageContent() {
        GitHubPRStatusBuilder builder = new GitHubPRStatusBuilder(new GitHubPRMessage(null));

        assertEquals(DEFAULT_MESSAGE, builder.getStatusMessage().getContent());
    }

    @Test
    public void createBuilderWithEmptyMessage() {
        GitHubPRStatusBuilder builder = new GitHubPRStatusBuilder(new GitHubPRMessage(""));

        assertEquals(DEFAULT_MESSAGE, builder.getStatusMessage().getContent());
    }

    @Test
    public void createBuilderWithCustomMessage() {
        GitHubPRStatusBuilder builder = new GitHubPRStatusBuilder(new GitHubPRMessage(CUSTOM_MESSAGE));

        assertEquals(CUSTOM_MESSAGE, builder.getStatusMessage().getContent());
    }

    @Test
    public void runBuilderWithNullTrigger() throws IOException, InterruptedException {
        GitHubPRStatusBuilder builder = new GitHubPRStatusBuilder(new GitHubPRMessage(CUSTOM_MESSAGE));

        triggerExpectations(null);

        assertTrue(builder.perform(build, launcher, listener));
    }

    @Test
    public void runBuilderWithNullCause() throws IOException, InterruptedException {
        GitHubPRStatusBuilder builder = new GitHubPRStatusBuilder(new GitHubPRMessage(CUSTOM_MESSAGE));

        triggerExpectations(trigger);
        when(build.getCause(GitHubPRCause.class)).thenReturn(null);

        assertTrue(builder.perform(build, launcher, listener));
    }

    @Test
    public void runBuilderWithIOExceptionOnGettingRemoteRepo() throws IOException, InterruptedException {
        GitHubPRStatusBuilder builder = new GitHubPRStatusBuilder(new GitHubPRMessage(CUSTOM_MESSAGE));

        triggerExpectations(trigger);
        when(build.getCause(GitHubPRCause.class)).thenReturn(cause);
        urlExpectations();
        when(trigger.getRemoteRepo()).thenThrow(new IllegalStateException("on getting remote repo"));
        when(listener.getLogger()).thenReturn(logger);

        assertTrue(builder.perform(build, launcher, listener));
    }

    @Test
    public void runBuilderWithIOExceptionOnSettingDescription() throws IOException, InterruptedException {
        when(message.getContent()).thenReturn(CUSTOM_MESSAGE);

        GitHubPRStatusBuilder builder = new GitHubPRStatusBuilder(message);

        triggerExpectations(trigger);
        when(build.getCause(GitHubPRCause.class)).thenReturn(cause);
        urlExpectations();
        when(trigger.getRemoteRepo()).thenReturn(remoteRepository);
        when(message.expandAll(build, listener)).thenReturn("expanded");

        when(build.getProject()).thenReturn(project);
        when(project.getParent()).thenReturn(itemGroup);
        when(itemGroup.getFullName()).thenReturn("project full name");

        doThrow(new IOException("on setting description")).when(build).setDescription(any(String.class));

        assertTrue(builder.perform(build, launcher, listener));
    }

    private void triggerExpectations(GitHubPRTrigger expectedTrigger) {
        when(build.getProject()).thenReturn(project);
        when(project.getTrigger(GitHubPRTrigger.class)).thenReturn(expectedTrigger);
    }

    private void urlExpectations() {
        when(trigger.getDescriptor()).thenReturn(triggerDescriptor);
        when(triggerDescriptor.getJenkinsURL()).thenReturn("http://www.validjenkins.url");
        when(build.getUrl()).thenReturn("http://www.validbuild.url");
    }
}
