package org.jenkinsci.plugins.opsbox.utility.logsummary;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class LogAnalysisRuleTest {
    
    private LogAnalysisRule rule;
    
    @Before
    public void setUp() {
        rule = new LogAnalysisRule("test");
    }
    
    @Test
    public void testYamlFormatPatternMatching() {
        // 测试YAML格式的正则表达式（带斜杠）
        List<String> patterns = Arrays.asList(
            "/error/",
            "/ERROR/",
            "/(?i)warning/"
        );
        
        rule.setSearchPatterns(patterns);
        
        // 测试匹配
        assertTrue("Should match /error/ pattern", rule.matches("This is an error message"));
        assertTrue("Should match /ERROR/ pattern", rule.matches("ERROR: Something went wrong"));
        assertTrue("Should match /(?i)warning/ pattern", rule.matches("Warning: Deprecated method"));
        assertTrue("Should match /(?i)warning/ pattern (case insensitive)", rule.matches("WARNING: Resource not found"));
        
        // 测试不匹配
        assertFalse("Should not match unrelated text", rule.matches("This is a normal message"));
        assertFalse("Should not match null", rule.matches(null));
    }
    
    @Test
    public void testPlainPatternMatching() {
        // 测试普通正则表达式（不带斜杠）
        List<String> patterns = Arrays.asList(
            "error",
            "ERROR",
            "(?i)warning"
        );
        
        rule.setSearchPatterns(patterns);
        
        // 测试匹配
        assertTrue("Should match plain error pattern", rule.matches("This is an error message"));
        assertTrue("Should match plain ERROR pattern", rule.matches("ERROR: Something went wrong"));
        assertTrue("Should match plain warning pattern", rule.matches("Warning: Deprecated method"));
        assertTrue("Should match plain warning pattern (case insensitive)", rule.matches("WARNING: Resource not found"));
    }
    
    @Test
    public void testComplexPatternMatching() {
        // 测试复杂的正则表达式
        List<String> patterns = Arrays.asList(
            "/(?i)^error:/",
            "/(?i)warning/",
            "/Exception/",
            "/at .*\\.java:/"
        );
        
        rule.setSearchPatterns(patterns);
        
        // 测试匹配
        assertTrue("Should match start of line error:", rule.matches("error: Something went wrong"));
        assertTrue("Should match warning with case variation", rule.matches("Warning: Deprecated method"));
        assertTrue("Should match warning with case variation", rule.matches("WARNING: Resource not found"));
        assertTrue("Should match exception", rule.matches("java.lang.NullPointerException"));
        assertTrue("Should match stack trace", rule.matches("at com.example.Main.main(Main.java:10)"));
        
        // 测试不匹配
        assertFalse("Should not match error in middle of line", rule.matches("This is an error message"));
        assertFalse("Should not match unrelated text", rule.matches("This is a normal message"));
    }
    
    @Test
    public void testInvalidPatternHandling() {
        // 测试无效的正则表达式处理
        List<String> patterns = Arrays.asList(
            "/error/",  // 有效
            "/invalid[regex/",  // 无效
            "/another_valid_pattern/"  // 有效
        );
        
        rule.setSearchPatterns(patterns);
        
        // 应该能够编译有效的模式
        assertTrue("Should have compiled patterns", rule.getCompiledPatternCount() > 0);
        
        // 应该能够匹配有效的模式
        assertTrue("Should match valid pattern", rule.matches("This is an error message"));
        assertTrue("Should match another valid pattern", rule.matches("This is another_valid_pattern in log"));
    }
    
    @Test
    public void testEmptyAndNullPatterns() {
        // 测试空和null模式
        rule.setSearchPatterns(null);
        assertFalse("Should not match with null patterns", rule.matches("any text"));
        
        rule.setSearchPatterns(Arrays.asList());
        assertFalse("Should not match with empty patterns", rule.matches("any text"));
        
        rule.setSearchPatterns(Arrays.asList("", "   "));
        assertFalse("Should not match with empty string patterns", rule.matches("any text"));
    }
    
    @Test
    public void testPatternCleaning() {
        // 测试模式清理功能
        List<String> patterns = Arrays.asList(
            "/simple/",
            "/with spaces /",
            "  /trimmed/  ",
            "plain pattern",
            "/complex(?i)pattern/"
        );
        
        rule.setSearchPatterns(patterns);
        
        // 验证原始模式保持不变
        List<String> originalPatterns = rule.getOriginalPatterns();
        assertEquals("Should preserve original patterns", patterns.size(), originalPatterns.size());
        
        // 验证编译后的模式数量
        assertTrue("Should have compiled patterns", rule.getCompiledPatternCount() > 0);
        
        // 测试匹配
        assertTrue("Should match cleaned pattern", rule.matches("This is a simple message"));
        assertTrue("Should match plain pattern", rule.matches("This contains plain pattern"));
    }
    
    @Test
    public void testShowNameFallback() {
        // 测试显示名称的回退机制
        LogAnalysisRule rule1 = new LogAnalysisRule("test_rule");
        assertEquals("Should use name as showName when not set", "test_rule", rule1.getShowName());
        
        LogAnalysisRule rule2 = new LogAnalysisRule("test_rule");
        rule2.setShowName("Custom Display Name");
        assertEquals("Should use custom showName", "Custom Display Name", rule2.getShowName());
    }
    
    @Test
    public void testNullLogLineHandling() {
        // 测试null日志行处理
        rule.setSearchPatterns(Arrays.asList("/error/"));
        
        assertFalse("Should not match null log line", rule.matches(null));
        assertFalse("Should not match empty log line", rule.matches(""));
        assertFalse("Should not match whitespace log line", rule.matches("   "));
    }
} 