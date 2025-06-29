package org.jenkinsci.plugins.opsbox.utility.logsummary;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

public class LogAnalyzerStreamTest {
    
    private LogAnalyzer analyzer;
    
    @Before
    public void setUp() {
        // 创建测试规则
        List<LogAnalysisRule> rules = List.of(
            createTestRule("error", "Error", List.of("/(?i)^error ", "/(?i)error:")),
            createTestRule("warning", "Warning", List.of("/[Ww]arning", "/WARNING")),
            createTestRule("info", "Info", List.of("/(?i)^info ", "/(?i)information:"))
        );
        
        analyzer = new LogAnalyzer(rules);
    }
    
    @Test
    public void testAnalyzeStream() throws Exception {
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
        String log = "ERROR: fail\nwarning: be careful\nnormal line\nerror: again\n";
        InputStream in = new ByteArrayInputStream(log.getBytes(StandardCharsets.UTF_8));
        List<LogAnalysisResult> results = analyzer.analyzeStream(in);
        assertEquals(2, results.size());
        LogAnalysisResult errorResult = results.stream().filter(r -> r.getRuleName().equals("error")).findFirst().orElse(null);
        assertNotNull(errorResult);
        assertEquals(2, errorResult.getCount());
        LogAnalysisResult warningResult = results.stream().filter(r -> r.getRuleName().equals("warning")).findFirst().orElse(null);
        assertNotNull(warningResult);
        assertEquals(1, warningResult.getCount());
    }
    
    @Test
    public void testAnalyzeStreamParallel() throws Exception {
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
        for (int i = 0; i < 1000; i++) {
            sb.append("ERROR: fail\n");
            sb.append("warning: be careful\n");
            sb.append("normal line\n");
        }
        InputStream in = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
        List<LogAnalysisResult> results = analyzer.analyzeStreamParallel(in);
        assertEquals(2, results.size());
        LogAnalysisResult errorResult = results.stream().filter(r -> r.getRuleName().equals("error")).findFirst().orElse(null);
        assertNotNull(errorResult);
        assertTrue(errorResult.getCount() > 0);
        LogAnalysisResult warningResult = results.stream().filter(r -> r.getRuleName().equals("warning")).findFirst().orElse(null);
        assertNotNull(warningResult);
        assertTrue(warningResult.getCount() > 0);
    }
    
    @Test
    public void testAnalyzeStreamWithEmptyLog() throws IOException {
        InputStream logStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        List<LogAnalysisResult> results = analyzer.analyzeStream(logStream);
        
        assertFalse("Empty log should return results with count 0", results.isEmpty());
        results.forEach(result -> assertEquals("All results should have count 0", 0, result.getCount()));
    }
    
    @Test
    public void testAnalyzeStreamWithNullStream() throws IOException {
        List<LogAnalysisResult> results = analyzer.analyzeStream(null);
        
        assertTrue("Null stream should return empty results", results.isEmpty());
    }
    
    @Test
    public void testStreamVsStringComparison() throws IOException {
        String logContent = """
            Starting build...
            ERROR: Compilation failed
            Warning: Deprecated method used
            INFO: Build started
            ERROR: Another error occurred
            Normal log line
            INFO: Build completed
            """;
        
        // 使用String方法
        List<LogAnalysisResult> stringResults = analyzer.analyze(logContent);
        
        // 使用Stream方法
        InputStream logStream = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
        List<LogAnalysisResult> streamResults = analyzer.analyzeStream(logStream);
        
        // 验证结果一致性
        assertEquals("Results should be consistent between methods", 
                    stringResults.size(), streamResults.size());
        
        // 验证每个结果的内容
        for (int i = 0; i < stringResults.size(); i++) {
            LogAnalysisResult stringResult = stringResults.get(i);
            LogAnalysisResult streamResult = streamResults.get(i);
            
            assertEquals("Result names should match", 
                        stringResult.getRuleName(), streamResult.getRuleName());
            assertEquals("Result counts should match", 
                        stringResult.getCount(), streamResult.getCount());
        }
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
     * 生成大日志内容用于性能测试
     */
    private String generateLargeLogContent(int lineCount) {
        StringBuilder log = new StringBuilder();
        java.util.Random random = new java.util.Random(42); // 固定种子以确保可重复性
        
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