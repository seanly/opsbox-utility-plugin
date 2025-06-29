package org.jenkinsci.plugins.opsbox.utility.logsummary;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 日志分析器 - 使用Stream API优化性能
 */
public class LogAnalyzer {

    public List<LogAnalysisRule> rules;

    public LogAnalyzer(List<LogAnalysisRule> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
    }

    /**
     * 分析日志内容 - 使用Stream API优化性能
     */
    public List<LogAnalysisResult> analyze(String logContent) {
        if (logContent == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用ConcurrentHashMap提高并发性能
        Map<String, LogAnalysisResult> results = new ConcurrentHashMap<>();
        
        // 预初始化结果映射
        rules.forEach(rule -> 
            results.put(rule.getName(), new LogAnalysisResult(rule.getName(), rule.getShowName()))
        );

        // 如果日志内容为空，直接返回所有结果（count为0）
        if (logContent.trim().isEmpty()) {
            return results.values().stream()
                .sorted(Comparator.comparing(LogAnalysisResult::getShowName))
                .collect(Collectors.toList());
        }

        // 将日志内容分割成行并并行处理
        List<String> logLines = Arrays.asList(logContent.split("\n"));
        
        // 使用Stream API并行处理日志行
        logLines.parallelStream()
            .filter(line -> line != null && !line.trim().isEmpty())
            .forEach(line -> {
                // 对每个规则检查匹配
                rules.forEach(rule -> {
                    if (rule.matches(line)) {
                        LogAnalysisResult result = results.get(rule.getName());
                        if (result != null) {
                            result.addMatchedLog(line);
                        }
                    }
                });
            });

        // 返回所有结果，包括没有匹配的项目
        return results.values().stream()
            .sorted(Comparator.comparing(LogAnalysisResult::getShowName))
            .collect(Collectors.toList());
    }

    /**
     * 分析日志流 - 使用InputStream直接处理，避免内存溢出
     */
    public List<LogAnalysisResult> analyzeStream(InputStream logStream) throws IOException {
        if (logStream == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用ConcurrentHashMap提高并发性能
        Map<String, LogAnalysisResult> results = new ConcurrentHashMap<>();
        
        // 预初始化结果映射
        rules.forEach(rule -> 
            results.put(rule.getName(), new LogAnalysisResult(rule.getName(), rule.getShowName()))
        );

        // 使用BufferedReader逐行读取，避免将整个文件加载到内存
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(logStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // 对每个规则检查匹配
                for (LogAnalysisRule rule : rules) {
                    if (rule.matches(line)) {
                        LogAnalysisResult result = results.get(rule.getName());
                        if (result != null) {
                            result.addMatchedLog(line);
                        }
                    }
                }
            }
        }

        // 返回所有结果，包括没有匹配的项目
        return results.values().stream()
            .sorted(Comparator.comparing(LogAnalysisResult::getShowName))
            .collect(Collectors.toList());
    }

    /**
     * 分析日志流 - 并行处理版本（适用于大文件）
     */
    public List<LogAnalysisResult> analyzeStreamParallel(InputStream logStream) throws IOException {
        if (logStream == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用ConcurrentHashMap提高并发性能
        Map<String, LogAnalysisResult> results = new ConcurrentHashMap<>();
        
        // 预初始化结果映射
        rules.forEach(rule -> 
            results.put(rule.getName(), new LogAnalysisResult(rule.getName(), rule.getShowName()))
        );

        // 读取所有行到列表中（对于大文件，可能需要分批处理）
        List<String> logLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(logStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    logLines.add(line);
                }
            }
        }

        // 使用Stream API并行处理日志行
        logLines.parallelStream()
            .forEach(line -> {
                rules.forEach(rule -> {
                    if (rule.matches(line)) {
                        LogAnalysisResult result = results.get(rule.getName());
                        if (result != null) {
                            result.addMatchedLog(line);
                        }
                    }
                });
            });

        // 返回所有结果，包括没有匹配的项目
        return results.values().stream()
            .sorted(Comparator.comparing(LogAnalysisResult::getShowName))
            .collect(Collectors.toList());
    }

    /**
     * 分析日志内容 - 单线程版本（适用于小文件或需要顺序处理的情况）
     */
    public List<LogAnalysisResult> analyzeSequential(String logContent) {
        if (logContent == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, LogAnalysisResult> results = new HashMap<>();
        
        // 预初始化结果映射
        rules.forEach(rule -> 
            results.put(rule.getName(), new LogAnalysisResult(rule.getName(), rule.getShowName()))
        );

        // 如果日志内容为空，直接返回所有结果（count为0）
        if (logContent.trim().isEmpty()) {
            return results.values().stream()
                .sorted(Comparator.comparing(LogAnalysisResult::getShowName))
                .collect(Collectors.toList());
        }

        // 使用Stream API顺序处理
        logContent.lines()
            .filter(line -> !line.trim().isEmpty())
            .forEach(line -> {
                rules.forEach(rule -> {
                    if (rule.matches(line)) {
                        LogAnalysisResult result = results.get(rule.getName());
                        if (result != null) {
                            result.addMatchedLog(line);
                        }
                    }
                });
            });

        // 返回所有结果，包括没有匹配的项目
        return results.values().stream()
            .sorted(Comparator.comparing(LogAnalysisResult::getShowName))
            .collect(Collectors.toList());
    }

    /**
     * 从YAML配置创建分析器 - 使用Stream API优化
     */
    public static LogAnalyzer fromYamlConfig(String yamlConfig) {
        if (yamlConfig == null || yamlConfig.trim().isEmpty()) {
            return createDefaultAnalyzer();
        }

        try {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
            Map<String, Object> data = yaml.load(yamlConfig);

            if (data != null) {
                List<LogAnalysisRule> rules = data.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof Map)
                    .map(entry -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> ruleData = (Map<String, Object>) entry.getValue();
                        return parseRule(entry.getKey(), ruleData);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                return new LogAnalyzer(rules);
            }
        } catch (Exception e) {
            // 如果YAML解析失败，返回默认配置
            return createDefaultAnalyzer();
        }

        return createDefaultAnalyzer();
    }

    /**
     * 解析单个规则 - 使用Stream API优化
     */
    private static LogAnalysisRule parseRule(String ruleName, Map<String, Object> ruleData) {
        String showName = Optional.ofNullable((String) ruleData.get("showName"))
            .orElse(ruleName);

        LogAnalysisRule rule = new LogAnalysisRule(ruleName);
        rule.setShowName(showName);

        Object searchObj = ruleData.get("search");
        List<String> patterns = new ArrayList<>();

        if (searchObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> searchList = (List<String>) searchObj;
            patterns.addAll(searchList);
        } else if (searchObj instanceof String) {
            patterns.add((String) searchObj);
        }

        if (!patterns.isEmpty()) {
            rule.setSearchPatterns(patterns);
            return rule;
        }

        return null;
    }

    /**
     * 创建默认分析器 - 使用Stream API优化
     */
    private static LogAnalyzer createDefaultAnalyzer() {
        List<LogAnalysisRule> rules = Stream.of(
            createErrorRule(),
            createWarningRule(),
            createInfoRule()
        ).collect(Collectors.toList());

        return new LogAnalyzer(rules);
    }

    /**
     * 创建错误规则
     */
    private static LogAnalysisRule createErrorRule() {
        LogAnalysisRule errorRule = new LogAnalysisRule("error");
        errorRule.setShowName("Error");
        errorRule.setSearchPatterns(List.of(
            "/(?i)^error ",
            "/(?i)error:",
            "/(?i)fatal error",
            "/(?i)build failed",
            "/(?i)compilation failed"
        ));
        return errorRule;
    }

    /**
     * 创建警告规则
     */
    private static LogAnalysisRule createWarningRule() {
        LogAnalysisRule warningRule = new LogAnalysisRule("warning");
        warningRule.setShowName("Warning");
        warningRule.setSearchPatterns(List.of(
            "/[Ww]arning",
            "/WARNING",
            "/(?i)warning:",
            "/(?i)deprecated"
        ));
        return warningRule;
    }

    /**
     * 创建信息规则
     */
    private static LogAnalysisRule createInfoRule() {
        LogAnalysisRule infoRule = new LogAnalysisRule("info");
        infoRule.setShowName("Information");
        infoRule.setSearchPatterns(List.of(
            "/(?i)^info ",
            "/(?i)information:",
            "/(?i)note:"
        ));
        return infoRule;
    }

    /**
     * 获取规则数量
     */
    public int getRuleCount() {
        return rules.size();
    }

    /**
     * 检查是否有规则配置
     */
    public boolean hasRules() {
        return !rules.isEmpty();
    }
}