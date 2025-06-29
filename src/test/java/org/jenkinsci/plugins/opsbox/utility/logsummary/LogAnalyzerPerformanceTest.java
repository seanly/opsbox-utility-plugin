package org.jenkinsci.plugins.opsbox.utility.logsummary;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class LogAnalyzerPerformanceTest {

    private LogAnalyzer analyzer;
    private String largeLogContent;
    private String smallLogContent;

    @Before
    public void setUp() {
        // 创建测试规则
        List<LogAnalysisRule> rules = List.of(
            createTestRule("error", "Error", List.of("/(?i)^error ", "/(?i)error:")),
            createTestRule("warning", "Warning", List.of("/[Ww]arning", "/WARNING")),
            createTestRule("info", "Info", List.of("/(?i)^info ", "/(?i)information:"))
        );

        analyzer = new LogAnalyzer(rules);

        // 生成测试日志内容
        smallLogContent = generateTestLog(100);
        largeLogContent = generateTestLog(10000);
    }

    @Test
    public void testPerformanceComparison() {
        // 测试小文件性能
        long startTime = System.currentTimeMillis();
        List<LogAnalysisResult> sequentialResults = analyzer.analyzeSequential(smallLogContent);
        long sequentialTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        List<LogAnalysisResult> parallelResults = analyzer.analyze(largeLogContent);
        long parallelTime = System.currentTimeMillis() - startTime;

        // 验证结果一致性
        assertEquals(sequentialResults.size(), parallelResults.size());

        System.out.println("Small log (100 lines) - Sequential: " + sequentialTime + "ms");
        System.out.println("Large log (10000 lines) - Parallel: " + parallelTime + "ms");

        // 验证性能提升（大文件并行处理应该更快）
        assertTrue("Parallel processing should be efficient for large files",
                  parallelTime < 5000); // 5秒内应该完成
    }

    @Test
    public void testLargeLogPerformance() {
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("ERROR: fail\n");
            sb.append("warning: be careful\n");
            sb.append("normal line\n");
        }
        String log = sb.toString();
        long start = System.currentTimeMillis();
        List<LogAnalysisResult> results = analyzer.analyze(log);
        long duration = System.currentTimeMillis() - start;
        assertFalse("Should find some results in large log", results.isEmpty());
        LogAnalysisResult errorResult = results.stream().filter(r -> r.getRuleName().equals("error")).findFirst().orElse(null);
        assertNotNull(errorResult);
        assertTrue(errorResult.getCount() > 0);
        LogAnalysisResult warningResult = results.stream().filter(r -> r.getRuleName().equals("warning")).findFirst().orElse(null);
        assertNotNull(warningResult);
        assertTrue(warningResult.getCount() > 0);
        System.out.println("Large log analysis duration: " + duration + "ms");
    }

    @Test
    public void testEmptyLogPerformance() {
        // 测试空日志处理性能
        long startTime = System.currentTimeMillis();
        List<LogAnalysisResult> results = analyzer.analyze("");
        long processingTime = System.currentTimeMillis() - startTime;

        System.out.println("Empty log processing time: " + processingTime + "ms");

        // 空日志应该快速返回
        assertTrue("Empty log should be processed quickly", processingTime < 100);
        assertFalse("Empty log should return results with count 0", results.isEmpty());
        results.forEach(result -> assertEquals("All results should have count 0", 0, result.getCount()));
    }

    @Test
    public void testNullLogPerformance() {
        // 测试null日志处理性能
        long startTime = System.currentTimeMillis();
        List<LogAnalysisResult> results = analyzer.analyze(null);
        long processingTime = System.currentTimeMillis() - startTime;

        System.out.println("Null log processing time: " + processingTime + "ms");

        // null日志应该快速返回
        assertTrue("Null log should be processed quickly", processingTime < 100);
        assertTrue("Null log should return empty results", results.isEmpty());
    }

    /**
     * 创建测试规则
     */
    private LogAnalysisRule createTestRule(String name, String showName, List<String> patterns) {
        LogAnalysisRule rule = new LogAnalysisRule(name);
        rule.setShowName(showName);
        rule.setSearchPatterns(patterns);
        return rule;
    }

    /**
     * 生成测试日志内容
     */
    private String generateTestLog(int lineCount) {
        StringBuilder log = new StringBuilder();
        Random random = new Random(42); // 固定种子以确保可重复性

        String[] logTypes = {
            "Starting build...",
            "ERROR: Compilation failed",
            "Warning: Deprecated method used",
            "INFO: Build started",
            "ERROR: Another error occurred",
            "Normal log line",
            "INFO: Build completed",
            "WARNING: Resource not found",
            "error: Something went wrong",
            "Information: Process completed"
        };

        for (int i = 0; i < lineCount; i++) {
            if (i > 0) {
                log.append('\n');
            }
            log.append(logTypes[random.nextInt(logTypes.length)]);
        }

        return log.toString();
    }
}