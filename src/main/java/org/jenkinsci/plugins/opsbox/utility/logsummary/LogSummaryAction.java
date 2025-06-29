package org.jenkinsci.plugins.opsbox.utility.logsummary;

import hudson.model.Action;
import hudson.model.Run;
import lombok.Getter;

import java.util.List;

/**
 * 日志摘要Action，用于在构建页面展示分析结果
 */
@Getter
public class LogSummaryAction implements Action {

    private final Run<?, ?> run;
    private final List<LogAnalysisResult> results;

    public LogSummaryAction(Run<?, ?> run, List<LogAnalysisResult> results) {
        this.run = run;
        this.results = results;
    }

    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @Override
    public String getDisplayName() {
        return "Log Summary";
    }

    @Override
    public String getUrlName() {
        return null;
    }

    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    public int getTotalErrors() {
        if (results == null) {
            return 0;
        }
        return results.stream().mapToInt(LogAnalysisResult::getCount).sum();
    }

}