package org.jenkinsci.plugins.opsbox.utility.logsummary;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 日志分析规则配置
 */
public class LogAnalysisRule {

    @Setter
    @Getter
    private String name;
    @Getter
    private List<String> searchPatterns;
    private String showName;
    private final List<Pattern> compiledPatterns;

    @DataBoundConstructor
    public LogAnalysisRule(String name) {
        this.name = name;
        this.searchPatterns = new ArrayList<>();
        this.compiledPatterns = new ArrayList<>();
    }

    @DataBoundSetter
    public void setSearchPatterns(List<String> searchPatterns) {
        this.searchPatterns = searchPatterns != null ? searchPatterns : new ArrayList<>();
        compilePatterns();
    }

    public String getShowName() {
        return showName != null ? showName : name;
    }

    @DataBoundSetter
    public void setShowName(String showName) {
        this.showName = showName;
    }

    /**
     * 编译正则表达式模式
     */
    private void compilePatterns() {
        compiledPatterns.clear();
        if (searchPatterns != null) {
            for (String pattern : searchPatterns) {
                String cleanPattern = cleanPattern(pattern);
                if (cleanPattern.isEmpty()) {
                    continue; // 跳过空模式
                }
                try {
                    compiledPatterns.add(Pattern.compile(cleanPattern));
                } catch (Exception e) {
                    // 如果正则表达式无效，记录错误但继续处理其他模式
                    System.err.println("Invalid regex pattern: " + pattern + " for rule: " + name);
                }
            }
        }
    }

    /**
     * 清理正则表达式模式，去除YAML格式的斜杠
     */
    private String cleanPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return "";
        }

        String trimmed = pattern.trim();

        // 如果模式以/开头和结尾，去除它们
        if (trimmed.startsWith("/") && trimmed.endsWith("/")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }

        return trimmed;
    }

    /**
     * 检查日志行是否匹配当前规则
     */
    public boolean matches(String logLine) {
        if (logLine == null || compiledPatterns.isEmpty()) {
            return false;
        }

        for (Pattern pattern : compiledPatterns) {
            try {
                if (pattern.matcher(logLine).find()) {
                    return true;
                }
            } catch (Exception e) {
                // 忽略匹配异常，继续检查其他模式
            }
        }
        return false;
    }

    /**
     * 获取原始搜索模式（用于调试）
     */
    public List<String> getOriginalPatterns() {
        return new ArrayList<>(searchPatterns);
    }

    /**
     * 获取编译后的模式数量
     */
    public int getCompiledPatternCount() {
        return compiledPatterns.size();
    }
}