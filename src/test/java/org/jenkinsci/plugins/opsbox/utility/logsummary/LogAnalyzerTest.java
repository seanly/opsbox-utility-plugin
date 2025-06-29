package org.jenkinsci.plugins.opsbox.utility.logsummary;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class LogAnalyzerTest {
    
    @Test
    public void testAnalyzeWithRules() {
        // 创建测试规则
        List<LogAnalysisRule> rules = new ArrayList<>();
        
        LogAnalysisRule errorRule = new LogAnalysisRule("error");
        errorRule.setSearchPatterns(List.of("error:", "ERROR:"));
        errorRule.setShowName("Error");
        rules.add(errorRule);
        
        LogAnalysisRule warningRule = new LogAnalysisRule("warning");
        warningRule.setSearchPatterns(List.of("warning:", "WARN:"));
        warningRule.setShowName("Warning");
        rules.add(warningRule);
        
        LogAnalyzer analyzer = new LogAnalyzer(rules);
        
        // 测试日志内容
        String logContent = 
            "2024-01-01 10:00:00 INFO: Application started\n" +
            "2024-01-01 10:00:01 WARN: Deprecated feature used\n" +
            "2024-01-01 10:00:02 error: Something went wrong\n" +
            "2024-01-01 10:00:03 INFO: Processing data\n" +
            "2024-01-01 10:00:04 ERROR: Critical failure\n" +
            "2024-01-01 10:00:05 warning: Minor issue detected";
        
        List<LogAnalysisResult> results = analyzer.analyze(logContent);
        
        assertEquals(2, results.size());
        
        // 检查错误结果
        LogAnalysisResult errorResult = results.stream()
            .filter(r -> r.getRuleName().equals("error"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(errorResult);
        assertEquals("Error", errorResult.getShowName());
        assertEquals(2, errorResult.getCount());
        assertTrue(errorResult.getMatchedLogs().contains("2024-01-01 10:00:02 error: Something went wrong"));
        assertTrue(errorResult.getMatchedLogs().contains("2024-01-01 10:00:04 ERROR: Critical failure"));
        
        // 检查警告结果
        LogAnalysisResult warningResult = results.stream()
            .filter(r -> r.getRuleName().equals("warning"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(warningResult);
        assertEquals("Warning", warningResult.getShowName());
        assertEquals(2, warningResult.getCount());
        assertTrue(warningResult.getMatchedLogs().contains("2024-01-01 10:00:01 WARN: Deprecated feature used"));
        assertTrue(warningResult.getMatchedLogs().contains("2024-01-01 10:00:05 warning: Minor issue detected"));
    }
    
    @Test
    public void testAnalyzeWithYamlConfig() {
        String yamlConfig = 
            "error:\n" +
            "  search:\n" +
            "  - \"error:\"\n" +
            "  - \"ERROR:\"\n" +
            "  showName: Error\n" +
            "\n" +
            "warning:\n" +
            "  search:\n" +
            "  - \"warning:\"\n" +
            "  - \"WARN:\"\n" +
            "  showName: Warning";
        
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig(yamlConfig);
        
        String logContent = 
            "2024-01-01 10:00:00 INFO: Application started\n" +
            "2024-01-01 10:00:01 WARN: Deprecated feature used\n" +
            "2024-01-01 10:00:02 error: Something went wrong";
        
        List<LogAnalysisResult> results = analyzer.analyze(logContent);
        
        assertEquals(2, results.size());
        
        // 检查错误结果
        LogAnalysisResult errorResult = results.stream()
            .filter(r -> r.getRuleName().equals("error"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(errorResult);
        assertEquals("Error", errorResult.getShowName());
        assertEquals(1, errorResult.getCount());
        
        // 检查警告结果
        LogAnalysisResult warningResult = results.stream()
            .filter(r -> r.getRuleName().equals("warning"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(warningResult);
        assertEquals("Warning", warningResult.getShowName());
        assertEquals(1, warningResult.getCount());
    }
    
    @Test
    public void testEmptyLogContent() {
        List<LogAnalysisRule> rules = new ArrayList<>();
        LogAnalysisRule rule = new LogAnalysisRule("test");
        rule.setSearchPatterns(List.of("test"));
        rules.add(rule);
        
        LogAnalyzer analyzer = new LogAnalyzer(rules);
        List<LogAnalysisResult> results = analyzer.analyze("");
        
        assertEquals(1, results.size());
        assertEquals(0, results.get(0).getCount());
    }
    
    @Test
    public void testNoMatchingLogs() {
        List<LogAnalysisRule> rules = new ArrayList<>();
        LogAnalysisRule rule = new LogAnalysisRule("test");
        rule.setSearchPatterns(List.of("nonexistent"));
        rules.add(rule);
        
        LogAnalyzer analyzer = new LogAnalyzer(rules);
        List<LogAnalysisResult> results = analyzer.analyze("This is a test log line");
        
        assertEquals(1, results.size());
        assertEquals(0, results.get(0).getCount());
    }
    
    @Test
    public void testYamlConfigParsing() {
        String yamlConfig = """
            error:
              search:
                - /(?i)error/
                - /(?i)error:/
                - /(?i)fatal error/
              showName: Error
            
            warning:
              search:
                - /(?i)warning/
                - /(?i)WARNING/
              showName: Warning
            """;
        
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig(yamlConfig);
        List<LogAnalysisRule> rules = analyzer.rules;
        
        assertEquals(2, rules.size());
        
        // 检查错误规则
        LogAnalysisRule errorRule = rules.stream()
            .filter(r -> r.getName().equals("error"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(errorRule);
        assertEquals("Error", errorRule.getShowName());
        assertEquals(3, errorRule.getSearchPatterns().size());
        assertTrue(errorRule.getSearchPatterns().contains("/(?i)error/"));
        
        // 检查警告规则
        LogAnalysisRule warningRule = rules.stream()
            .filter(r -> r.getName().equals("warning"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(warningRule);
        assertEquals("Warning", warningRule.getShowName());
        assertEquals(2, warningRule.getSearchPatterns().size());
    }
    
    @Test
    public void testYamlFormatPatternMatching() {
        String yamlConfig = """
            error:
              search:
                - /(?i)error/
              showName: Error
            
            warning:
              search:
                - /(?i)warning/
              showName: Warning
            """;
        
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig(yamlConfig);
        
        String logContent = """
            Starting build...
            ERROR: Compilation failed
            Warning: Deprecated method used
            INFO: Build completed
            error: Another error occurred
            Normal log line
            WARNING: Resource not found
            """;
        
        List<LogAnalysisResult> results = analyzer.analyze(logContent);
        
        assertEquals(2, results.size());
        
        // 检查错误结果
        LogAnalysisResult errorResult = results.stream()
            .filter(r -> r.getRuleName().equals("error"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(errorResult);
        assertEquals(2, errorResult.getCount()); // ERROR, error
        assertTrue(errorResult.getMatchedLogs().contains("ERROR: Compilation failed"));
        assertTrue(errorResult.getMatchedLogs().contains("error: Another error occurred"));
        
        // 检查警告结果
        LogAnalysisResult warningResult = results.stream()
            .filter(r -> r.getRuleName().equals("warning"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(warningResult);
        assertEquals(2, warningResult.getCount());
        assertTrue(warningResult.getMatchedLogs().contains("Warning: Deprecated method used"));
        assertTrue(warningResult.getMatchedLogs().contains("WARNING: Resource not found"));
    }
    
    @Test
    public void testComplexYamlPatternMatching() {
        String yamlConfig = """
            error:
              search:
                - /(?i)^error:/
                - /(?i)error:/
              showName: Error
            
            exception:
              search:
                - /Exception/
                - /at .*\\.java:/
              showName: Exception
            """;
        
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig(yamlConfig);
        
        String logContent = """
            Starting build...
            ERROR: Compilation failed
            error: Something went wrong
            java.lang.NullPointerException
            at com.example.Main.main(Main.java:10)
            INFO: Build completed
            """;
        
        List<LogAnalysisResult> results = analyzer.analyze(logContent);
        
        assertEquals(2, results.size());
        
        // 检查错误结果
        LogAnalysisResult errorResult = results.stream()
            .filter(r -> r.getRuleName().equals("error"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(errorResult);
        assertEquals(2, errorResult.getCount());
        assertTrue(errorResult.getMatchedLogs().contains("ERROR: Compilation failed"));
        assertTrue(errorResult.getMatchedLogs().contains("error: Something went wrong"));
        
        // 检查异常结果
        LogAnalysisResult exceptionResult = results.stream()
            .filter(r -> r.getRuleName().equals("exception"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(exceptionResult);
        assertEquals(2, exceptionResult.getCount());
        assertTrue(exceptionResult.getMatchedLogs().contains("java.lang.NullPointerException"));
        assertTrue(exceptionResult.getMatchedLogs().contains("at com.example.Main.main(Main.java:10)"));
    }
    
    @Test
    public void testEmptyYamlConfig() {
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig("");
        List<LogAnalysisRule> rules = analyzer.rules;
        
        // 应该返回默认规则
        assertTrue(rules.size() > 0);
        
        // 检查是否有默认的错误和警告规则
        boolean hasErrorRule = rules.stream().anyMatch(r -> r.getName().equals("error"));
        boolean hasWarningRule = rules.stream().anyMatch(r -> r.getName().equals("warning"));
        
        assertTrue(hasErrorRule);
        assertTrue(hasWarningRule);
    }
    
    @Test
    public void testInvalidYamlConfig() {
        String invalidYaml = "invalid: yaml: content:";
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig(invalidYaml);
        List<LogAnalysisRule> rules = analyzer.rules;
        
        // 应该返回默认规则
        assertTrue(rules.size() > 0);
    }
    
    @Test
    public void testLogAnalysis() {
        String yamlConfig = """
            error:
              search:
                - /(?i)error/
              showName: Error
            
            warning:
              search:
                - /(?i)warning/
              showName: Warning
            """;
        
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig(yamlConfig);
        
        String logContent = """
            Starting build...
            ERROR: Compilation failed
            Warning: Deprecated method used
            INFO: Build completed
            ERROR: Another error occurred
            Normal log line
            """;
        
        List<LogAnalysisResult> results = analyzer.analyze(logContent);
        
        assertEquals(2, results.size());
        
        // 检查错误结果
        LogAnalysisResult errorResult = results.stream()
            .filter(r -> r.getRuleName().equals("error"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(errorResult);
        assertEquals(2, errorResult.getCount());
        assertTrue(errorResult.getMatchedLogs().contains("ERROR: Compilation failed"));
        assertTrue(errorResult.getMatchedLogs().contains("ERROR: Another error occurred"));
        
        // 检查警告结果
        LogAnalysisResult warningResult = results.stream()
            .filter(r -> r.getRuleName().equals("warning"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(warningResult);
        assertEquals(1, warningResult.getCount());
        assertTrue(warningResult.getMatchedLogs().contains("Warning: Deprecated method used"));
    }
    
    @Test
    public void testSequentialAnalysis() {
        String yamlConfig = """
            error:
              search:
                - /(?i)error/
              showName: Error
            
            warning:
              search:
                - /(?i)warning/
              showName: Warning
            """;
        
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig(yamlConfig);
        
        String logContent = """
            Starting build...
            ERROR: Compilation failed
            Warning: Deprecated method used
            INFO: Build completed
            ERROR: Another error occurred
            Normal log line
            """;
        
        List<LogAnalysisResult> results = analyzer.analyzeSequential(logContent);
        
        assertEquals(2, results.size());
        
        // 验证结果排序
        assertTrue("Results should be sorted by showName", 
                  results.get(0).getShowName().compareTo(results.get(1).getShowName()) <= 0);
        
        // 验证至少有一个结果有匹配
        assertTrue("At least one result should have count > 0", 
                  results.stream().anyMatch(result -> result.getCount() > 0));
    }
    
    @Test
    public void testEmptyLogAnalysis() {
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig("");
        
        // 测试空日志
        List<LogAnalysisResult> emptyResults = analyzer.analyze("");
        assertFalse("Empty log should return results with count 0", emptyResults.isEmpty());
        emptyResults.forEach(result -> assertEquals("All results should have count 0", 0, result.getCount()));
        
        // 测试null日志
        List<LogAnalysisResult> nullResults = analyzer.analyze(null);
        assertTrue("Null log should return empty results", nullResults.isEmpty());
        
        // 测试只有空行的日志
        List<LogAnalysisResult> whitespaceResults = analyzer.analyze("   \n  \n  ");
        assertFalse("Whitespace-only log should return results with count 0", whitespaceResults.isEmpty());
        whitespaceResults.forEach(result -> assertEquals("All results should have count 0", 0, result.getCount()));
    }
    
    @Test
    public void testAnalyzerProperties() {
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig("");
        
        assertTrue("Analyzer should have rules", analyzer.hasRules());
        assertTrue("Analyzer should have rule count > 0", analyzer.getRuleCount() > 0);
        
        LogAnalyzer emptyAnalyzer = new LogAnalyzer(null);
        assertFalse("Empty analyzer should not have rules", emptyAnalyzer.hasRules());
        assertEquals("Empty analyzer should have 0 rules", 0, emptyAnalyzer.getRuleCount());
    }
} 