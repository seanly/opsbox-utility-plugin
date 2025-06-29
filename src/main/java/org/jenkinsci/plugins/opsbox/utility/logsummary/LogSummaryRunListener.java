package org.jenkinsci.plugins.opsbox.utility.logsummary;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 构建完成后自动分析日志并添加LogSummaryAction（使用InputStream优化性能）
 */
@Extension
public class LogSummaryRunListener extends RunListener<Run<?, ?>> {

    // 设置最大日志长度，避免处理过大的日志文件
    private static final int MAX_LOG_LENGTH = 1000000; // 1MB

    @Override
    public void onCompleted(Run<?, ?> run, TaskListener listener) {
        try {
            List<LogAnalysisRule> rules = LogSummaryGlobalConfiguration.get().getConfiguredRules();
            if (rules == null || rules.isEmpty()) {
                return; // 无配置时不做分析
            }

            // 获取日志输入流
            InputStream logStream = getLogInputStream(run);
            if (logStream == null) {
                return; // 无日志内容时不做分析
            }

            // 创建分析器并分析日志
            LogAnalyzer analyzer = new LogAnalyzer(rules);

            // 根据构建状态和规则数量选择处理方式
            List<LogAnalysisResult> results;
            if (shouldUseParallelProcessing(run, rules.size())) {
                // 大文件或复杂规则使用并行处理
                results = analyzer.analyzeStreamParallel(logStream);
                listener.getLogger().println("[LogSummary] Using parallel stream processing");
            } else {
                // 小文件使用顺序处理
                results = analyzer.analyzeStream(logStream);
                listener.getLogger().println("[LogSummary] Using sequential stream processing");
            }

            // 只有当有匹配结果时才添加Action
            if (results.stream().anyMatch(r -> r.getCount() > 0)) {
                run.addAction(new LogSummaryAction(run, results));
                int totalMatches = results.stream().mapToInt(LogAnalysisResult::getCount).sum();
                listener.getLogger().println("[LogSummary] Analysis completed: " + totalMatches + " matches found across " + results.size() + " categories");
            } else {
                listener.getLogger().println("[LogSummary] No matches found in log analysis");
            }

        } catch (IOException e) {
            listener.getLogger().println("[LogSummary] Failed to analyze build log: " + e.getMessage());
        } catch (Exception e) {
            listener.getLogger().println("[LogSummary] Unexpected error during log analysis: " + e.getMessage());
        }
    }

    /**
     * 获取构建日志输入流
     */
    private InputStream getLogInputStream(Run<?, ?> run) {
        if (run == null) {
            return null;
        }

        try {
            // 优先使用getLogInputStream()方法获取日志流
            return run.getLogInputStream();
        } catch (IOException e) {
            // 如果getLogInputStream()失败，尝试使用getLog(maxLength)方法
            try {
                List<String> logLines = run.getLog(MAX_LOG_LENGTH);
                if (!logLines.isEmpty()) {
                    // 将List<String>转换为InputStream
                    String logContent = String.join("\n", logLines);
                    return new java.io.ByteArrayInputStream(logContent.getBytes("UTF-8"));
                }
            } catch (Exception ex) {
                // 忽略异常，返回null
            }
            return null;
        }
    }

    /**
     * 判断是否应该使用并行处理
     */
    private boolean shouldUseParallelProcessing(Run<?, ?> run, int ruleCount) {
        // 根据构建持续时间判断日志大小
        long buildDuration = run.getDuration();
        if (buildDuration > 300000) { // 超过5分钟
            return true;
        }

        // 根据规则数量判断
        if (ruleCount > 5) {
            return true;
        }

        // 根据构建结果判断
        if (run.getResult() != null && run.getResult().isWorseThan(hudson.model.Result.SUCCESS)) {
            // 失败的构建通常有更多日志
            return true;
        }

        return false;
    }
}