package org.jenkinsci.plugins.opsbox.utility.logsummary;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest2;

import java.util.List;

/**
 * Log Summary 全局配置（系统管理页面配置规则）
 */
@Getter
@Extension
public class LogSummaryGlobalConfiguration extends GlobalConfiguration {
    private String rulesConfig;

    public LogSummaryGlobalConfiguration() {
        load();
    }

    public static LogSummaryGlobalConfiguration get() {
        return GlobalConfiguration.all().get(LogSummaryGlobalConfiguration.class);
    }

    @DataBoundSetter
    public void setRulesConfig(String rulesConfig) {
        this.rulesConfig = rulesConfig;
        save();
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    /**
     * 获取配置的规则列表
     */
    public List<LogAnalysisRule> getConfiguredRules() {
        if (rulesConfig != null && !rulesConfig.trim().isEmpty()) {
            return LogAnalyzer.fromYamlConfig(rulesConfig).rules;
        }
        return null;
    }
}