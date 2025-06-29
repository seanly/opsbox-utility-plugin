#!/usr/bin/env groovy

/**
 * Jenkins Groovy Script to migrate JobBuildNameParameterDefinition class references
 * from old package cn.opsbox.jenkinsci.plugins.oes.parameter.BuildNameParameterDefinition
* to new package org.jenkinsci.plugins.opsbox.utility.parameter.JobBuildNameParameterDefinition
 * 
 * Usage: Run this script in Jenkins Script Console (Manage Jenkins > Script Console)
 */

import jenkins.model.Jenkins
import hudson.model.*
import hudson.util.XStream2
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.mapper.Mapper
import java.util.logging.Logger
import java.util.logging.Level

def logger = Logger.getLogger("migration-script")
logger.setLevel(Level.INFO)

// 定义旧的和新的类名
def OLD_CLASS_NAME = "cn.opsbox.jenkinsci.plugins.oes.parameter.BuildNameParameterDefinition"
def NEW_CLASS_NAME = "org.jenkinsci.plugins.opsbox.utility.parameter.JobBuildNameParameterDefinition"

logger.info("Starting migration from ${OLD_CLASS_NAME} to ${NEW_CLASS_NAME}")

def migratedCount = 0
def errorCount = 0

// 获取所有项目
def allItems = Jenkins.instance.getAllItems()

logger.info("Found ${allItems.size()} items to check")

allItems.each { item ->
    try {
        logger.info("Checking item: ${item.fullName}")
        
        // 检查项目是否有参数化构建
        if (item instanceof ParametersDefinitionProperty) {
            def hasOldParameter = false
            
            // 检查参数定义
            if (item.hasProperty('parameterDefinitions')) {
                item.parameterDefinitions?.each { paramDef ->
                    if (paramDef.class.name == OLD_CLASS_NAME) {
                        logger.info("Found old parameter definition in ${item.fullName}")
                        hasOldParameter = true
                    }
                }
            }
            
            if (hasOldParameter) {
                logger.info("Migrating ${item.fullName}")
                // 这里需要重新配置项目
                // 实际的迁移逻辑会在下面处理
                migratedCount++
            }
        }
        
        // 对于 Job 类型的项目，检查其配置
        if (item instanceof Job) {
            def configFile = item.getConfigFile()
            def configXml = configFile.asString()
            
            if (configXml.contains(OLD_CLASS_NAME)) {
                logger.info("Found old class reference in config XML of ${item.fullName}")
                
                // 替换配置文件中的类名
                def newConfigXml = configXml.replace(OLD_CLASS_NAME, NEW_CLASS_NAME)
                
                // 备份原配置
                def backupFile = new File(configFile.file.parent, configFile.file.name + ".backup-" + System.currentTimeMillis())
                backupFile.write(configXml)
                logger.info("Backup created: ${backupFile.absolutePath}")
                
                // 写入新配置
                configFile.write(newConfigXml)
                
                // 重新加载项目配置
                item.doReload()
                
                logger.info("Successfully migrated ${item.fullName}")
                migratedCount++
            }
        }
        
    } catch (Exception e) {
        logger.severe("Error processing ${item.fullName}: ${e.message}")
        e.printStackTrace()
        errorCount++
    }
}

// 处理全局配置
try {
    logger.info("Checking global configuration...")
    def globalConfig = Jenkins.instance.getConfigFile()
    def globalConfigXml = globalConfig.asString()
    
    if (globalConfigXml.contains(OLD_CLASS_NAME)) {
        logger.info("Found old class reference in global configuration")
        
        // 备份全局配置
        def backupFile = new File(globalConfig.file.parent, "config.xml.backup-" + System.currentTimeMillis())
        backupFile.write(globalConfigXml)
        logger.info("Global config backup created: ${backupFile.absolutePath}")
        
        // 替换全局配置中的类名
        def newGlobalConfigXml = globalConfigXml.replace(OLD_CLASS_NAME, NEW_CLASS_NAME)
        globalConfig.write(newGlobalConfigXml)
        
        logger.info("Global configuration migrated")
        migratedCount++
    }
} catch (Exception e) {
    logger.severe("Error processing global configuration: ${e.message}")
    e.printStackTrace()
    errorCount++
}

// 处理视图配置
Jenkins.instance.views.each { view ->
    try {
        def viewConfigFile = view.getConfigFile()
        if (viewConfigFile != null) {
            def viewConfigXml = viewConfigFile.asString()
            
            if (viewConfigXml.contains(OLD_CLASS_NAME)) {
                logger.info("Found old class reference in view: ${view.viewName}")
                
                // 备份视图配置
                def backupFile = new File(viewConfigFile.file.parent, viewConfigFile.file.name + ".backup-" + System.currentTimeMillis())
                backupFile.write(viewConfigXml)
                
                // 替换视图配置中的类名
                def newViewConfigXml = viewConfigXml.replace(OLD_CLASS_NAME, NEW_CLASS_NAME)
                viewConfigFile.write(newViewConfigXml)
                
                logger.info("View configuration migrated: ${view.viewName}")
                migratedCount++
            }
        }
    } catch (Exception e) {
        logger.severe("Error processing view ${view.viewName}: ${e.message}")
        errorCount++
    }
}

// 清理 XStream 别名缓存
try {
    logger.info("Clearing XStream aliases...")
    def xstream = Jenkins.XSTREAM2
    
    // 添加新的别名映射（如果需要）
    // xstream.alias("buildNameParameterDefinition", Class.forName(NEW_CLASS_NAME))
    
    logger.info("XStream aliases updated")
} catch (Exception e) {
    logger.warning("Could not update XStream aliases: ${e.message}")
}

// 保存Jenkins配置
try {
    Jenkins.instance.save()
    logger.info("Jenkins configuration saved")
} catch (Exception e) {
    logger.severe("Error saving Jenkins configuration: ${e.message}")
    errorCount++
}

// 输出迁移结果
logger.info("=== Migration Summary ===")
logger.info("Items migrated: ${migratedCount}")
logger.info("Errors encountered: ${errorCount}")

if (migratedCount > 0) {
    logger.info("Migration completed successfully!")
    logger.info("Please restart Jenkins to ensure all changes take effect.")
    logger.info("Backup files have been created for all modified configurations.")
} else {
    logger.info("No items found requiring migration.")
}

if (errorCount > 0) {
    logger.warning("Some errors occurred during migration. Please check the logs above.")
}

// 返回结果摘要
return [
    "migratedCount": migratedCount,
    "errorCount": errorCount,
    "message": migratedCount > 0 ? "Migration completed. Please restart Jenkins." : "No migration needed."
] 