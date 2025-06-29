#!/usr/bin/env groovy

/**
 * 简化版本：专门用于迁移 JobBuildNameParameterDefinition 参数定义
 * 从 cn.opsbox.jenkinsci.plugins.oes.parameter.BuildNameParameterDefinition
* 到 org.jenkinsci.plugins.opsbox.utility.parameter.JobBuildNameParameterDefinition
 */

import jenkins.model.Jenkins
import hudson.model.*

// 配置
def OLD_CLASS_NAME = "cn.opsbox.jenkinsci.plugins.oes.parameter.BuildNameParameterDefinition"
def NEW_CLASS_NAME = "org.jenkinsci.plugins.opsbox.utility.parameter.JobBuildNameParameterDefinition"

println "开始迁移参数定义..."
println "从: ${OLD_CLASS_NAME}"
println "到: ${NEW_CLASS_NAME}"
println "=" * 50

def migratedJobs = []
def errors = []

// 遍历所有作业
Jenkins.instance.getAllItems(Job.class).each { job ->
    try {
        def modified = false
        def configFile = job.getConfigFile()
        def configXml = configFile.asString()
        
        // 检查是否包含旧的类名
        if (configXml.contains(OLD_CLASS_NAME)) {
            println "发现需要迁移的作业: ${job.fullName}"
            
            // 创建备份
            def backupFile = new File(configFile.file.absolutePath + ".backup-" + new Date().format("yyyyMMdd-HHmmss"))
            backupFile.text = configXml
            println "  - 备份文件: ${backupFile.name}"
            
            // 执行替换
            def newConfigXml = configXml.replaceAll(OLD_CLASS_NAME, NEW_CLASS_NAME)
            
            // 写入新配置
            configFile.write(newConfigXml)
            
            // 重新加载配置
            job.doReload()
            
            migratedJobs << job.fullName
            println "  - ✓ 迁移完成"
        }
        
    } catch (Exception e) {
        def error = "错误处理作业 ${job.fullName}: ${e.message}"
        errors << error
        println "  - ✗ ${error}"
    }
}

// 检查并迁移全局配置中的参数模板
try {
    def globalConfigFile = Jenkins.instance.getConfigFile()
    def globalConfigXml = globalConfigFile.asString()
    
    if (globalConfigXml.contains(OLD_CLASS_NAME)) {
        println "\n发现全局配置需要迁移"
        
        // 备份全局配置
        def backupFile = new File(Jenkins.instance.getRootDir(), "config.xml.backup-" + new Date().format("yyyyMMdd-HHmmss"))
        backupFile.text = globalConfigXml
        println "  - 全局配置备份: ${backupFile.name}"
        
        // 执行替换
        def newGlobalConfigXml = globalConfigXml.replaceAll(OLD_CLASS_NAME, NEW_CLASS_NAME)
        globalConfigFile.write(newGlobalConfigXml)
        
        println "  - ✓ 全局配置迁移完成"
    }
} catch (Exception e) {
    errors << "迁移全局配置时出错: ${e.message}"
}

// 保存所有更改
try {
    Jenkins.instance.save()
    println "\n✓ Jenkins 配置已保存"
} catch (Exception e) {
    errors << "保存 Jenkins 配置时出错: ${e.message}"
}

// 输出结果摘要
println "\n" + "=" * 50
println "迁移摘要:"
println "=" * 50
println "成功迁移的作业数量: ${migratedJobs.size()}"

if (migratedJobs.size() > 0) {
    println "\n迁移的作业列表:"
    migratedJobs.each { jobName ->
        println "  - ${jobName}"
    }
}

if (errors.size() > 0) {
    println "\n遇到的错误:"
    errors.each { error ->
        println "  - ${error}"
    }
}

println "\n" + "=" * 50
if (migratedJobs.size() > 0) {
    println "⚠️  重要提示："
    println "1. 请重启 Jenkins 以确保所有更改生效"
    println "2. 确保新插件 (oes-utils-plugin) 已正确安装"
    println "3. 可以安全卸载旧插件 (oes-pipeline-plugin)"
    println "4. 所有配置文件都已备份，如有问题可以恢复"
} else {
    println "没有发现需要迁移的配置"
}

// 返回结果
return [
    migratedJobs: migratedJobs,
    errors: errors,
    success: errors.size() == 0
] 