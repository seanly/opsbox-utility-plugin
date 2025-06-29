# Log Summary 功能说明

LogSummary 模块通过按行分析日志内容，将匹配的信息整理，用表格的方式展示在构建页面。

## 功能特性

- 自动分析构建日志
- 支持正则表达式匹配规则
- 可配置的匹配规则（YAML格式）
- 表格形式展示匹配结果
- 在构建页面侧边栏显示链接
- 全局配置管理（系统管理页面）
- 高性能流式处理（避免内存溢出）

## 配置方式

### 1. 全局配置（推荐）

在Jenkins系统管理页面配置YAML规则：

1. 进入 **Manage Jenkins** > **Configure System**
2. 找到 **Log Summary Configuration** 部分
3. 在YAML配置文本框中输入规则
4. 点击 **Save** 保存配置

### 2. 配置文件

在插件资源目录中放置 `log-summary-config.yml` 文件（作为默认配置）。

## YAML配置格式

配置规则采用YAML格式，支持以下结构：

```yaml
# 规则名称
rule_name:
  search:
    - /正则表达式1/
    - /正则表达式2/
  showName: 显示名称
```

### 配置示例

```yaml
# 错误模式匹配
error:
  search:
    - /(?i)^error /
    - /(?i)error:/
    - /(?i)fatal error/
    - /(?i)build failed/
    - /(?i)compilation failed/
  showName: Error

# 警告模式匹配
warning:
  search:
    - /[Ww]arning/
    - /WARNING/
    - /(?i)warning:/
    - /(?i)deprecated/
  showName: Warning

# 信息模式匹配
info:
  search:
    - /(?i)^info /
    - /(?i)information:/
    - /(?i)note:/
  showName: Information

# 异常模式匹配
exception:
  search:
    - /Exception/
    - /Error/
    - /Stack trace/
    - /at .*\.java:/
  showName: Exception
```

## 使用方法

1. **自动集成**: 插件会自动为每个构建添加LogSummary功能
2. **查看摘要**: 在构建页面侧边栏点击"Log Summary"链接
3. **表格展示**: 匹配的内容会按规则分组显示在表格中

## 表格格式

| Name | Count | Logs |
|------|-------|------|
| Error | 2 | 匹配的错误信息列表 |
| Warning | 1 | 匹配的警告信息列表 |
| Information | 0 | 匹配的信息列表 |

## 默认配置

如果未提供配置文件，插件会使用以下默认规则：

- **Error**: 匹配以"error"开头的行（不区分大小写）
- **Warning**: 匹配包含"warning"的行（不区分大小写）
- **Information**: 匹配以"info"开头的行（不区分大小写）

## 正则表达式说明

- 使用标准的Java正则表达式语法
- 支持不区分大小写匹配：`(?i)`
- 支持行首匹配：`^`
- 支持行尾匹配：`$`
- 支持任意字符匹配：`.*`
- **YAML格式支持**：支持`/pattern/`格式，会自动去除前后的斜杠

### 正则表达式格式

#### 1. YAML格式（推荐）
```yaml
error:
  search:
    - /error/          # 匹配包含"error"的行
    - /ERROR/          # 匹配包含"ERROR"的行
    - /(?i)error/      # 不区分大小写匹配"error"
    - /(?i)^error /    # 以"error "开头的行（不区分大小写）
  showName: Error
```

#### 2. 普通格式
```yaml
error:
  search:
    - error            # 匹配包含"error"的行
    - ERROR            # 匹配包含"ERROR"的行
    - (?i)error        # 不区分大小写匹配"error"
    - (?i)^error       # 以"error"开头的行（不区分大小写）
  showName: Error
```

### 常用模式示例

| 模式 | 说明 | 示例 | 匹配结果 |
|------|------|------|----------|
| `/error/` | 匹配包含"error"的行 | `This is an error message` | ✅ |
| `/ERROR/` | 匹配包含"ERROR"的行 | `ERROR: Something went wrong` | ✅ |
| `/(?i)error/` | 不区分大小写匹配"error" | `Error`, `ERROR`, `error` | ✅ |
| `/(?i)^error /` | 以"error "开头的行（不区分大小写） | `ERROR: Compilation failed` | ✅ |
| `/(?i)^error /` | 以"error "开头的行（不区分大小写） | `This is an error message` | ❌ |
| `/[Ww]arning/` | 匹配"Warning"或"warning" | `Warning: Deprecated method` | ✅ |
| `/Exception/` | 匹配包含"Exception"的行 | `java.lang.NullPointerException` | ✅ |
| `/at .*\.java:/` | Java堆栈跟踪行 | `at com.example.Main.main(Main.java:10)` | ✅ |
| `/\\d+/` | 匹配数字 | `Build #123 completed` | ✅ |

### 正则表达式优化建议

#### 1. 使用YAML格式
```yaml
# 推荐：使用YAML格式，更清晰
error:
  search:
    - /(?i)^error /
    - /(?i)error:/
  showName: Error

# 不推荐：混合格式
error:
  search:
    - /(?i)^error /
    - error:
  showName: Error
```

#### 2. 避免过度复杂的正则表达式
```yaml
# 推荐：简单明确
error:
  search:
    - /(?i)^error /
    - /(?i)fatal error/
  showName: Error

# 不推荐：过于复杂
error:
  search:
    - /(?i)(error|fatal|critical|fail)/  # 过于复杂，难以维护
  showName: Error
```

#### 3. 使用适当的修饰符
```yaml
# 推荐：使用(?i)进行不区分大小写匹配
error:
  search:
    - /(?i)error/      # 匹配 error, ERROR, Error
  showName: Error

# 不推荐：重复定义大小写变体
error:
  search:
    - /error/
    - /ERROR/
    - /Error/
  showName: Error
```

## 高级配置

### 多模式匹配

一个规则可以包含多个匹配模式：

```yaml
error:
  search:
    - /(?i)^error /
    - /(?i)fatal error/
    - /(?i)build failed/
    - /(?i)compilation failed/
  showName: Error
```

### 自定义显示名称

可以为规则指定自定义的显示名称：

```yaml
my_custom_rule:
  search:
    - /MyApp:/
  showName: My Application Logs
```

## 性能优化

### 流式处理

插件使用高效的流式处理方式：

1. **优先使用InputStream**: 使用`run.getLogInputStream()`直接处理日志流
2. **回退机制**: 如果InputStream不可用，使用`run.getLog(maxLength)`方法
3. **内存限制**: 设置1MB的最大日志长度限制，避免内存溢出
4. **智能处理**: 根据构建状态自动选择并行或顺序处理

### 处理策略

```java
// 推荐的处理流程
InputStream logStream = run.getLogInputStream();
if (logStream != null) {
    if (shouldUseParallelProcessing(run, rules.size())) {
        results = analyzer.analyzeStreamParallel(logStream);
    } else {
        results = analyzer.analyzeStream(logStream);
    }
}
```

## 故障排除

### 配置不生效

1. 检查YAML语法是否正确
2. 确保正则表达式格式正确
3. 重启Jenkins服务
4. 检查Jenkins日志中的错误信息

### 匹配不到内容

1. 检查正则表达式是否正确
2. 使用在线正则表达式测试工具验证
3. 确保日志内容确实包含匹配的文本

### 性能问题

1. 检查日志文件大小
2. 优化正则表达式
3. 减少规则数量
4. 监控系统资源使用

### 内存问题

1. 使用InputStream处理方式
2. 增加JVM堆内存
3. 减少并行度
4. 分批处理大文件

## 注意事项

- 配置文件使用SnakeYAML库解析
- 匹配规则按顺序执行
- 一行日志可能匹配多个规则
- 空匹配结果不会显示在表格中
- 全局配置优先级高于默认配置
- 使用`run.getLog(maxLength)`替代已弃用的`run.getLog()`方法
- 设置1MB的最大日志长度限制，避免内存溢出
- 支持流式处理，适合处理大型日志文件 