package org.jenkinsci.plugins.opsbox.utility.logsummary;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志分析结果
 */
@Getter
public class LogAnalysisResult {

    private final String ruleName;
    private final String showName;
    private final List<String> matchedLogs;

    public LogAnalysisResult(String ruleName, String showName) {
        this.ruleName = ruleName;
        this.showName = showName;
        this.matchedLogs = new ArrayList<>();
    }

    public void addMatchedLog(String logLine) {
        this.matchedLogs.add(logLine);
    }

    public int getCount() {
        return matchedLogs.size();
    }

    public String getLogsAsString() {
        return String.join("\n", matchedLogs);
    }

}