package dev.springreview.engine;

import dev.springreview.parser.ParsedBundle;
import dev.springreview.rules.Rule;
import dev.springreview.scope.SourceBundle;
import dev.springreview.tools.IssueCandidate;

import java.util.List;

public interface Detector {

    boolean supports(Rule rule);

    List<IssueCandidate> detect(Rule rule, SourceBundle bundle, ParsedBundle parsed);
}
