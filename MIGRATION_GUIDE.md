# Jenkins 插件迁移指南

## 概述
本指南将帮助您将 `oes-pipeline-plugin` 中的 `BuildNameParameterDefinition` 类迁移到新的 `oes-utils-plugin` 中，包名从 `cn.opsbox.jenkinsci.plugins.oes.parameter.BuildNameParameterDefinition` 更改为 `dev.opsbox.jenkinsci.plugins.oes.parameter.BuildNameParameterDefinition`。

## 迁移步骤

### 第一步：准备工作

1. **备份 Jenkins 数据**
   ```bash
   # 停止 Jenkins
   sudo systemctl stop jenkins
   
   # 备份 Jenkins_HOME 目录
   sudo cp -r /var/lib/jenkins /var/lib/jenkins.backup.$(date +%Y%m%d)
   
   # 重启 Jenkins
   sudo systemctl start jenkins
   ```

2. **确认当前状态**
   - 确认旧插件 `oes-pipeline-plugin` 已安装
   - 记录哪些作业使用了 `BuildNameParameterDefinition` 参数

### 第二步：安装新插件

1. **构建新插件**
   ```bash
   cd oes-utils-plugin
   ~/Programming/apps/apache-maven-3.9.9/bin/mvn clean package
   ```

2. **安装新插件**
   - 进入 Jenkins 管理界面 → 插件管理 → 高级设置
   - 上传生成的 `target/oes-utils-plugin.hpi` 文件
   - 重启 Jenkins

### 第三步：执行迁移脚本

1. **打开 Jenkins 脚本控制台**
   - 登录 Jenkins
   - 进入 "Manage Jenkins" → "Script Console"

2. **运行预检查脚本**（可选）
   ```groovy
   // 预检查脚本 - 只查看不修改
   import jenkins.model.Jenkins
   import hudson.model.*
   
   def OLD_CLASS_NAME = "cn.opsbox.jenkinsci.plugins.oes.parameter.JobBuildNameParameterDefinition"
   
   def affectedJobs = []
   
   Jenkins.instance.getAllItems(Job.class).each { job ->
       def configXml = job.getConfigFile().asString()
       if (configXml.contains(OLD_CLASS_NAME)) {
           affectedJobs << job.fullName
       }
   }
   
   println "需要迁移的作业数量: ${affectedJobs.size()}"
   affectedJobs.each { jobName ->
       println "  - ${jobName}"
   }
   
   return affectedJobs
   ```

3. **执行完整迁移脚本**
   - 将 `parameter-migration-script.groovy` 的内容复制到脚本控制台
   - 点击 "运行" 按钮
   - 查看输出结果

### 第四步：验证迁移结果

1. **检查迁移日志**
   - 确认所有作业都已成功迁移
   - 检查是否有错误信息

2. **验证作业配置**
   ```groovy
   // 验证脚本
   import jenkins.model.Jenkins
   import hudson.model.*
   
   def NEW_CLASS_NAME = "dev.opsbox.jenkinsci.plugins.oes.parameter.JobBuildNameParameterDefinition"
   def OLD_CLASS_NAME = "cn.opsbox.jenkinsci.plugins.oes.parameter.JobBuildNameParameterDefinition"
   
   def newClassJobs = []
   def oldClassJobs = []
   
   Jenkins.instance.getAllItems(Job.class).each { job ->
       def configXml = job.getConfigFile().asString()
       if (configXml.contains(NEW_CLASS_NAME)) {
           newClassJobs << job.fullName
       }
       if (configXml.contains(OLD_CLASS_NAME)) {
           oldClassJobs << job.fullName
       }
   }
   
   println "使用新类的作业: ${newClassJobs.size()}"
   println "仍使用旧类的作业: ${oldClassJobs.size()}"
   
   if (oldClassJobs.size() > 0) {
       println "⚠️ 仍有作业使用旧类，需要重新迁移:"
       oldClassJobs.each { println "  - ${it}" }
   }
   
   return [newClass: newClassJobs.size(), oldClass: oldClassJobs.size()]
   ```

3. **测试作业功能**
   - 手动运行几个迁移后的作业
   - 确认参数定义正常工作

### 第五步：清理工作

1. **重启 Jenkins**
   ```bash
   sudo systemctl restart jenkins
   ```

2. **卸载旧插件**（确认一切正常后）
   - 进入插件管理界面
   - 卸载 `oes-pipeline-plugin`
   - 再次重启 Jenkins

## 脚本说明

### migration-script.groovy
- **功能**: 完整的迁移脚本，处理所有配置文件
- **适用场景**: 复杂环境，需要全面迁移
- **特点**: 包含详细日志和错误处理

### parameter-migration-script.groovy
- **功能**: 简化版迁移脚本，专注于参数定义
- **适用场景**: 只需要迁移参数定义的简单场景
- **特点**: 更轻量，输出简洁

## 故障排除

### 常见问题

1. **类找不到错误**
   ```
   ClassNotFoundException: dev.opsbox.jenkinsci.plugins.oes.parameter.BuildNameParameterDefinition
   ```
   **解决方案**: 确认新插件已正确安装并重启 Jenkins

2. **权限错误**
   ```
   Access denied
   ```
   **解决方案**: 确保有管理员权限运行脚本

3. **配置文件锁定**
   ```
   IOException: Could not write to config file
   ```
   **解决方案**: 确保没有其他进程在修改配置文件

### 回滚步骤

如果迁移出现问题，可以按以下步骤回滚：

1. **恢复备份的配置文件**
   ```bash
   # 找到备份文件
   find /var/lib/jenkins -name "*.backup-*" -type f
   
   # 恢复特定作业的配置
   sudo cp /var/lib/jenkins/jobs/YOUR_JOB/config.xml.backup-20240628-123456 \
          /var/lib/jenkins/jobs/YOUR_JOB/config.xml
   ```

2. **使用 Jenkins 备份恢复**
   ```bash
   sudo systemctl stop jenkins
   sudo rm -rf /var/lib/jenkins
   sudo cp -r /var/lib/jenkins.backup.20240628 /var/lib/jenkins
   sudo chown -R jenkins:jenkins /var/lib/jenkins
   sudo systemctl start jenkins
   ```

## 迁移检查清单

- [ ] 已备份 Jenkins 数据
- [ ] 新插件已构建并安装
- [ ] 已运行预检查脚本
- [ ] 已执行迁移脚本
- [ ] 已验证迁移结果
- [ ] 已测试作业功能
- [ ] 已重启 Jenkins
- [ ] 已卸载旧插件（可选）

## 注意事项

1. **在生产环境执行前，建议先在测试环境验证**
2. **迁移过程中，建议暂停所有构建任务**
3. **保留备份文件至少一周，确认无问题后再删除**
4. **如有自定义配置，可能需要手动调整** 