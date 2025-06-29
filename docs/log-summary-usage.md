# Log Summary 功能使用指南

## 概述

Log Summary 模块提供了强大的日志分析功能，可以根据配置的规则对日志内容进行分析，并以表格形式展示结果。

## 功能特性

- 支持YAML配置格式定义分析规则
- 支持正则表达式和简单字符串匹配
- 表格形式展示分析结果
- 可展开查看完整的匹配日志
- 支持Pipeline集成

## 配置格式

### YAML配置示例

```yaml
error:
  search:
  - error:
  - ERROR:
  - /error: .*/
  showName: Error

warning:
  search:
  - warning:
  - WARN:
  - /warning: .*/
  showName: Warning

info:
  search:
  - INFO:
  - /INFO: .*/
  showName: Information
```

### 配置说明

- `error`, `warning`, `info`: 规则名称
- `search`: 搜索模式列表，支持正则表达式
- `showName`: 在表格中显示的名称

## Pipeline使用

### 基本用法

```groovy
def results = logSummary(
    logContent: logContent,
    yamlConfig: yamlConfig
)
```

### 完整示例

```groovy
pipeline {
    agent any
    
    stages {
        stage('Analyze Logs') {
            steps {
                script {
                    // 读取日志文件
                    def logContent = readFile 'build.log'
                    
                    // 定义分析规则
                    def yamlConfig = """
error:
  search:
  - error:
  - ERROR:
  showName: Error

warning:
  search:
  - warning:
  - WARN:
  showName: Warning
"""
                    
                    // 执行分析
                    def results = logSummary(
                        logContent: logContent,
                        yamlConfig: yamlConfig
                    )
                    
                    // 处理结果
                    for (result in results) {
                        echo "${result.showName}: ${result.count} entries"
                    }
                }
            }
        }
    }
}
```

## 结果格式

分析结果包含以下信息：

- `ruleName`: 规则名称
- `showName`: 显示名称
- `count`: 匹配的日志条数
- `matchedLogs`: 匹配的日志列表

## 界面展示

分析结果会在Jenkins构建页面中以表格形式展示：

| Name | Count | Logs |
|------|-------|------|
| Error | 2 | 2024-01-01 10:00:02 error: Something went wrong<br>2024-01-01 10:00:04 ERROR: Critical failure |
| Warning | 1 | 2024-01-01 10:00:01 WARN: Deprecated feature used |

## 高级功能

### 正则表达式支持

```yaml
error:
  search:
  - /error: .*/
  - /ERROR: .*/
  - /.*Exception.*/
  showName: Error
```

### 多模式匹配

```yaml
build_issues:
  search:
  - /BUILD FAILED/
  - /Compilation failed/
  - /Test failed/
  showName: Build Issues
```

## 最佳实践

1. **规则命名**: 使用有意义的规则名称
2. **模式设计**: 从简单模式开始，逐步优化
3. **测试验证**: 在测试环境中验证规则配置
4. **性能考虑**: 避免过于复杂的正则表达式
5. **错误处理**: 在Pipeline中添加适当的错误处理

## 故障排除

### 常见问题

1. **规则不匹配**: 检查正则表达式语法
2. **性能问题**: 简化复杂的正则表达式
3. **编码问题**: 确保日志文件编码正确

### 调试技巧

1. 使用简单的字符串匹配进行测试
2. 逐步添加正则表达式
3. 检查日志格式是否一致 