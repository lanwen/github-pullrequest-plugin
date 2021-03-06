package org.jenkinsci.plugins.github.pullrequest;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a comment for GitHub that can contain token macros.
 *
 * @author Kanstantsin Shautsou
 * @author Alina Karpovich
 */
public class GitHubPRMessage extends AbstractDescribableImpl<GitHubPRMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRMessage.class);

    private String content;

    @DataBoundConstructor
    public GitHubPRMessage(String content) {
        this.content = content;
    }

    public String expandAll(AbstractBuild<?, ?> build, TaskListener listener) {
        String content = getContent();
        if (content == null || content.length() == 0) {
            return content; // Do nothing for an empty String
        }
//        // Expand environment variables
//        String s = build.getEnvironment(listener).expand(content);
        // Expand build variables
        content = Util.replaceMacro(content, build.getBuildVariableResolver());

        Jenkins jenkins = Jenkins.getInstance();
        try {
            ClassLoader uberClassLoader = Jenkins.getInstance().pluginManager.uberClassLoader;

            if (jenkins.getPlugin("token-macro") != null) {
                List macros = null;

                //get private macroses like groovy template ${SCRIPT} if available
                if (jenkins.getPlugin("email-ext") != null) {
                    Class<?> contentBuilderClazz = uberClassLoader.loadClass("hudson.plugins.emailext.plugins.ContentBuilder");
                    Method getPrivateMacrosMethod = contentBuilderClazz.getDeclaredMethod("getPrivateMacros");
                    macros = new ArrayList((Collection) getPrivateMacrosMethod.invoke(null));
                }

                //calls TokenMacro.expand(build, listener, content, false, macros)
                Class<?> tokenMacroClazz = uberClassLoader.loadClass("org.jenkinsci.plugins.tokenmacro.TokenMacro");
                Method tokenMacroExpand = tokenMacroClazz.getDeclaredMethod("expand", AbstractBuild.class,
                        TaskListener.class, String.class, boolean.class, List.class);

                content = (String) tokenMacroExpand.invoke(null, build, listener, content, false, macros);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.error("Can't find class", e);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Can't evaluate macro", e);
        } catch (InvocationTargetException e) {
            LOGGER.error("Can't evaluate macro", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Can't evaluate macro", e);
        }

        return content;
    }

    public String getContent() {
        return content;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubPRMessage> {
        @Override
        public String getDisplayName() {
            return "Expandable comment field";
        }
    }
}
