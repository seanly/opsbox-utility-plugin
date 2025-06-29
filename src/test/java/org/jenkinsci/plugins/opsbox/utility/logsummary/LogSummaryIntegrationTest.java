package org.jenkinsci.plugins.opsbox.utility.logsummary;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import jenkins.model.GlobalConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;

import static org.junit.Assert.*;

public class LogSummaryIntegrationTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testLogSummaryWithGlobalConfig() throws Exception {
        // 配置全局规则
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
        LogSummaryGlobalConfiguration.get().setRulesConfig(yamlConfig);
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new hudson.tasks.Shell("""
echo "INFO: start"
echo "error: something failed"
echo "WARNING: be careful"
echo "done"
"""));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);

        // 检查Action
        LogSummaryAction action = build.getAction(LogSummaryAction.class);
        assertNotNull(action);
        List<LogAnalysisResult> results = action.getResults();
        LogAnalysisResult errorResult = results.stream().filter(r -> r.getRuleName().equals("error")).findFirst().orElse(null);
        assertNotNull(errorResult);
        assertTrue(errorResult.getMatchedLogs().stream().anyMatch(log -> log.contains("error: something failed")));
        LogAnalysisResult warningResult = results.stream().filter(r -> r.getRuleName().equals("warning")).findFirst().orElse(null);
        assertNotNull(warningResult);
        assertTrue(warningResult.getMatchedLogs().stream().anyMatch(log -> log.contains("WARNING: be careful")));
    }

    @Test
    public void testNoGlobalConfigNoAction() throws Exception {
        // 清空全局配置
        LogSummaryGlobalConfiguration.get().setRulesConfig("");
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new hudson.tasks.Shell("echo 'error: should not match'"));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);
        assertNull(build.getAction(LogSummaryAction.class));
    }

    @Test
    public void testLogSummaryRuleAccuracy() throws Exception {
        String yamlConfig = """
error:
  search:
  - /(?i)error/
  showName: Error
""";
        LogSummaryGlobalConfiguration.get().setRulesConfig(yamlConfig);
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new hudson.tasks.Shell("""
echo "error: one"
echo "error: two"
echo "not an error"
"""));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);
        LogSummaryAction action = build.getAction(LogSummaryAction.class);
        assertNotNull(action);
        LogAnalysisResult errorResult = action.getResults().stream().filter(r -> r.getRuleName().equals("error")).findFirst().orElse(null);
        assertNotNull(errorResult);
        assertTrue(errorResult.getMatchedLogs().stream().anyMatch(log -> log.contains("error: one")));
        assertTrue(errorResult.getMatchedLogs().stream().anyMatch(log -> log.contains("error: two")));
    }

    @Test
    public void testCompleteYamlWorkflow() {
        // 1. 测试YAML配置解析
        String yamlConfig = """
            error:
              search:
                - /(?i)error:/
                - /(?i)error/
              showName: Error
            
            warning:
              search:
                - /(?i)warning/
              showName: Warning
            
            info:
              search:
                - /(?i)info/
              showName: Information
            """;
        
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig(yamlConfig);
        List<LogAnalysisRule> rules = analyzer.rules;
        
        assertEquals(3, rules.size());
        
        // 2. 测试日志分析
        String logContent = """
            Starting build...
            ERROR: Compilation failed
            Warning: Deprecated method used
            INFO: Build started
            error: Another error occurred
            Normal log line
            INFO: Build completed
            """;
        
        List<LogAnalysisResult> results = analyzer.analyze(logContent);
        
        assertEquals(3, results.size());
        
        // 3. 验证错误结果
        LogAnalysisResult errorResult = results.stream()
            .filter(r -> r.getRuleName().equals("error"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(errorResult);
        assertEquals("Error", errorResult.getShowName());
        assertEquals(2, errorResult.getCount());
        
        // 4. 验证警告结果
        LogAnalysisResult warningResult = results.stream()
            .filter(r -> r.getRuleName().equals("warning"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(warningResult);
        assertEquals("Warning", warningResult.getShowName());
        assertEquals(1, warningResult.getCount());
        
        // 5. 验证信息结果
        LogAnalysisResult infoResult = results.stream()
            .filter(r -> r.getRuleName().equals("info"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(infoResult);
        assertEquals("Information", infoResult.getShowName());
        assertEquals(2, infoResult.getCount());
    }
    
    @Test
    public void testYamlConfigWithComplexPatterns() {
        String yamlConfig = """
            exception:
              search:
                - /Exception/
                - /at .*\\.java:/
                - /Caused by:/
              showName: Exception
            
            security:
              search:
                - /(?i)security/
                - /(?i)authentication/
                - /(?i)authorization/
              showName: Security
            """;
        
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig(yamlConfig);
        
        String logContent = """
            java.lang.NullPointerException
            at com.example.Main.main(Main.java:10)
            Caused by: java.lang.ArrayIndexOutOfBoundsException
            Security warning: Authentication failed
            Authorization denied for user admin
            """;
        
        List<LogAnalysisResult> results = analyzer.analyze(logContent);
        
        assertEquals(2, results.size());
        
        // 验证异常结果
        LogAnalysisResult exceptionResult = results.stream()
            .filter(r -> r.getRuleName().equals("exception"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(exceptionResult);
        assertEquals(3, exceptionResult.getCount());
        
        // 验证安全结果
        LogAnalysisResult securityResult = results.stream()
            .filter(r -> r.getRuleName().equals("security"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(securityResult);
        assertEquals(2, securityResult.getCount());
    }
    
    @Test
    public void testYamlConfigValidation() {
        // 测试无效的YAML配置
        String invalidYaml = "invalid: yaml: content:";
        LogAnalyzer analyzer = LogAnalyzer.fromYamlConfig(invalidYaml);
        
        // 应该返回默认配置
        List<LogAnalysisRule> rules = analyzer.rules;
        assertTrue(rules.size() > 0);
        
        // 测试空配置
        LogAnalyzer emptyAnalyzer = LogAnalyzer.fromYamlConfig("");
        List<LogAnalysisRule> emptyRules = emptyAnalyzer.rules;
        assertTrue(emptyRules.size() > 0);
        
        // 测试null配置
        LogAnalyzer nullAnalyzer = LogAnalyzer.fromYamlConfig(null);
        List<LogAnalysisRule> nullRules = nullAnalyzer.rules;
        assertTrue(nullRules.size() > 0);
    }
} 