package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelAddedEvent;

def f = namespace(lib.FormTagLib);

f.entry() {
    f.property(field:"label")
}
