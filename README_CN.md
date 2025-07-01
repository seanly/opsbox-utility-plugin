# Opsbox 实用工具插件

[![许可证: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Jenkins 插件](https://img.shields.io/jenkins/plugin/v/opsbox-utility-plugin.svg)](https://plugins.jenkins.io/opsbox-utility-plugin/)
[![Jenkins 版本](https://img.shields.io/badge/Jenkins-2.414+-blue.svg)](https://jenkins.io/)

一个为 Jenkins 流水线提供各种辅助功能的实用插件，包括任务构建名称参数定义和 Git 分支环境变量增强。

## 功能特性

### 🏗️ 任务构建名称参数定义
- **从其他任务选择构建名称**：允许用户从其他 Jenkins 任务的成功构建中选择构建名称作为参数
- **可配置数量限制**：设置显示构建名称的最大数量
- **智能过滤**：仅显示成功的构建，排除失败或正在构建的任务
- **文件夹支持**：支持文件夹中的任务，完整路径支持

### 🌿 Git 分支环境变量
- **增强环境变量**：自动添加 Git 仓库信息到环境变量
- **清理分支名称**：移除分支名称中的 `refs/heads/` 和 `refs/tags/` 前缀
- **凭据支持**：处理私有仓库的 Git 凭据
- **多参数支持**：支持同一任务中的多个 Git 分支参数

## 安装

### 从 Jenkins 插件管理器安装
1. 进入 Jenkins → 系统管理 → 插件管理
2. 搜索 "Opsbox Utility Plugin"
3. 安装并重启 Jenkins

### 手动安装
1. 从 [发布页面](https://github.com/jenkinsci/opsbox-utility-plugin/releases) 下载 `.hpi` 文件
2. 进入 Jenkins → 系统管理 → 插件管理 → 高级设置
3. 上传 `.hpi` 文件
4. 重启 Jenkins

### 从源码构建
```bash
git clone https://github.com/jenkinsci/opsbox-utility-plugin.git
cd opsbox-utility-plugin
mvn clean package
```

## 使用方法

### 任务构建名称参数

1. **添加参数定义**：
   - 在任务配置中，进入"参数化构建过程"
   - 添加"任务构建名称参数"
   - 配置源任务名称和数量限制

2. **配置选项**：
   - **名称**：参数名称（在流水线脚本中使用）
   - **任务名称**：源任务名称（支持文件夹路径，如 `folder/job`）
   - **数量限制**：显示构建的最大数量（默认：5）
   - **描述**：参数描述

3. **流水线使用**：
   ```groovy
   pipeline {
       agent any
       parameters {
           jobBuildNameParam(
               name: 'BUILD_NAME',
               jobName: 'upstream-job',
               description: '从上游任务选择构建名称'
           )
       }
       stages {
           stage('部署') {
               steps {
                   echo "部署构建：${params.BUILD_NAME}"
               }
           }
       }
   }
   ```

### Git 分支环境变量

当你使用 [List Git Branches Parameter](https://plugins.jenkins.io/list-git-branches-parameter/) 插件时，此功能会自动激活。

**可用环境变量**：
- `PARAMS__{参数名称}__REMOTE_URL`：Git 仓库 URL
- `PARAMS__{参数名称}__CREDENTIALS_ID`：Git 凭据 ID
- `{参数名称}`：清理后的分支名称（移除 refs/heads/ 前缀）

**示例**：
```groovy
pipeline {
    agent any
    parameters {
        listGitBranches(
            name: 'BRANCH',
            remoteURL: 'https://github.com/user/repo.git',
            credentialsId: 'git-credentials'
        )
    }
    stages {
        stage('构建') {
            steps {
                echo "分支：${params.BRANCH}"
                echo "仓库：${env.PARAMS__BRANCH__REMOTE_URL}"
                echo "凭据：${env.PARAMS__BRANCH__CREDENTIALS_ID}"
            }
        }
    }
}
```



## 系统要求

- **Jenkins**：2.414 或更高版本
- **Java**：11 或更高版本
- **依赖项**：
  - List Git Branches Parameter Plugin（用于 Git 功能）
  - Structs Plugin

## 开发

### 构建插件
```bash
mvn clean compile
```

### 运行测试
```bash
mvn test
```

### 开发模式运行
```bash
mvn hpi:run
```

### 创建发布版本
```bash
mvn release:prepare release:perform
```

## 贡献

1. Fork 仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启 Pull Request

## 许可证

此项目基于 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 支持

- **问题反馈**：[GitHub Issues](https://github.com/jenkinsci/opsbox-utility-plugin/issues)
- **插件文档**：[Jenkins 插件文档](https://plugins.jenkins.io/opsbox-utility-plugin/)
- **社区**：[Jenkins 社区](https://www.jenkins.io/chat/)

## 作者

**Seanly Liu** - [seanly.me@gmail.com](mailto:seanly.me@gmail.com)



## 常见问题

### Q: 如何处理文件夹中的任务？
A: 使用完整路径，例如 `folder1/folder2/job-name`。

### Q: 为什么看不到构建名称选项？
A: 确保源任务存在且有成功的构建记录。

### Q: 环境变量没有设置怎么办？
A: 确保安装了 List Git Branches Parameter 插件，并且参数配置正确。

---

用 ❤️ 为 Jenkins 社区制作 