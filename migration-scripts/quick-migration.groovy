// ==================================================================
// Jenkins 插件迁移脚本 - 快速版本
// 从 cn.opsbox.jenkinsci.plugins.oes.parameter.BuildNameParameterDefinition
// 到 org.jenkinsci.plugins.opsbox.utility.parameter.JobBuildNameParameterDefinition
// ==================================================================

import jenkins.model.Jenkins
import hudson.model.*

// 配置
def OLD_CLASS = "cn.opsbox.jenkinsci.plugins.oes.parameter.BuildNameParameterDefinition"
def NEW_CLASS = "org.jenkinsci.plugins.opsbox.utility.parameter.JobBuildNameParameterDefinition"

println "🔄 开始迁移 JobBuildNameParameterDefinition..."
println "   从: ${OLD_CLASS}"
println "   到: ${NEW_CLASS}"
println "-" * 60

def results = [migrated: 0, errors: 0, jobs: []]

// 迁移所有作业
Jenkins.instance.getAllItems(Job.class).each { job ->
    try {
        def configFile = job.getConfigFile()
        def configXml = configFile.asString()
        
        if (configXml.contains(OLD_CLASS)) {
            // 创建备份
            def backup = new File(configFile.file.absolutePath + ".bak")
            backup.text = configXml
            
            // 执行替换
            configFile.write(configXml.replace(OLD_CLASS, NEW_CLASS))
            job.doReload()
            
            results.migrated++
            results.jobs << job.fullName
            println "✅ ${job.fullName}"
        }
    } catch (Exception e) {
        results.errors++
        println "❌ ${job.fullName}: ${e.message}"
    }
}

// 迁移全局配置
try {
    def globalConfig = Jenkins.instance.getConfigFile()
    def globalXml = globalConfig.asString()
    
    if (globalXml.contains(OLD_CLASS)) {
        new File(Jenkins.instance.getRootDir(), "config.xml.bak").text = globalXml
        globalConfig.write(globalXml.replace(OLD_CLASS, NEW_CLASS))
        println "✅ 全局配置已迁移"
        results.migrated++
    }
} catch (Exception e) {
    println "❌ 全局配置迁移失败: ${e.message}"
    results.errors++
}

// 保存配置
Jenkins.instance.save()

// 输出结果
println "-" * 60
println "📊 迁移完成!"
println "   成功: ${results.migrated} 项"
println "   错误: ${results.errors} 项"

if (results.migrated > 0) {
    println "\n📋 迁移的作业:"
    results.jobs.each { println "   • ${it}" }
    
    println "\n⚠️  下一步操作:"
    println "   1. 重启 Jenkins"
    println "   2. 验证作业功能正常"
    println "   3. 卸载旧插件 (oes-pipeline-plugin)"
}

println "\n💾 备份文件: 所有配置都已备份为 .bak 文件"

return results 