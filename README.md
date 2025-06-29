# Opsbox Utility Plugin

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/opsbox-utility-plugin.svg)](https://plugins.jenkins.io/opsbox-utility-plugin/)
[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.414+-blue.svg)](https://jenkins.io/)

A utility plugin providing various helper functions for Jenkins pipelines, including job build name parameter definition and Git branch environment variable enhancement.

## Features

### 🏗️ Job Build Name Parameter Definition
- **Select build names from other jobs**: Allow users to select successful build names from other Jenkins jobs as parameters
- **Configurable count limit**: Set the maximum number of build names to display
- **Smart filtering**: Only shows successful builds, excluding failed or building jobs
- **Folder support**: Supports jobs in folders with full path support

### 🌿 Git Branches Environment Variables
- **Enhanced environment variables**: Automatically add Git repository information to environment variables
- **Clean branch names**: Removes `refs/heads/` and `refs/tags/` prefixes from branch names
- **Credentials support**: Handles Git credentials for private repositories
- **Multi-parameter support**: Works with multiple Git branch parameters in the same job

## Installation

### From Jenkins Plugin Manager
1. Go to Jenkins → Manage Jenkins → Plugin Manager
2. Search for "Opsbox Utility Plugin"
3. Install and restart Jenkins

### Manual Installation
1. Download the `.hpi` file from the [releases page](https://github.com/jenkinsci/opsbox-utility-plugin/releases)
2. Go to Jenkins → Manage Jenkins → Plugin Manager → Advanced
3. Upload the `.hpi` file
4. Restart Jenkins

### Build from Source
```bash
git clone https://github.com/jenkinsci/opsbox-utility-plugin.git
cd opsbox-utility-plugin
mvn clean package
```

## Usage

### Job Build Name Parameter

1. **Add Parameter Definition**:
   - In your job configuration, go to "This project is parameterized"
   - Add "Job Build Name Parameter"
   - Configure the source job name and count limit

2. **Configuration Options**:
   - **Name**: Parameter name (used in pipeline scripts)
   - **Job Name**: Source job name (supports folder paths like `folder/job`)
   - **Count Limit**: Maximum number of builds to show (default: 5)
   - **Description**: Parameter description

3. **Pipeline Usage**:
   ```groovy
   pipeline {
       agent any
       parameters {
           jobBuildNameParam(
               name: 'BUILD_NAME',
               jobName: 'upstream-job',
               description: 'Select build name from upstream job'
           )
       }
       stages {
           stage('Deploy') {
               steps {
                   echo "Deploying build: ${params.BUILD_NAME}"
               }
           }
       }
   }
   ```

### Git Branch Environment Variables

This feature automatically activates when you use the [List Git Branches Parameter](https://plugins.jenkins.io/list-git-branches-parameter/) plugin.

**Available Environment Variables**:
- `PARAMS__{PARAM_NAME}__REMOTE_URL`: Git repository URL
- `PARAMS__{PARAM_NAME}__CREDENTIALS_ID`: Git credentials ID
- `{PARAM_NAME}`: Clean branch name (without refs/heads/ prefix)

**Example**:
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
        stage('Build') {
            steps {
                echo "Branch: ${params.BRANCH}"
                echo "Repository: ${env.PARAMS__BRANCH__REMOTE_URL}"
                echo "Credentials: ${env.PARAMS__BRANCH__CREDENTIALS_ID}"
            }
        }
    }
}
```

## Migration Guide

If you're migrating from an older version of the plugin, use the provided migration scripts:

### Quick Migration
```groovy
// Run this in Jenkins Script Console
load 'migration-scripts/quick-migration.groovy'
```

### Detailed Migration
```groovy
// For complex environments with detailed logging
load 'migration-scripts/migration-script.groovy'
```

### Parameter-Only Migration
```groovy
// For migrating only parameter definitions
load 'migration-scripts/parameter-migration-script.groovy'
```

## Requirements

- **Jenkins**: 2.414 or higher
- **Java**: 11 or higher
- **Dependencies**: 
  - List Git Branches Parameter Plugin (for Git features)
  - Structs Plugin

## Development

### Building the Plugin
```bash
mvn clean compile
```

### Running Tests
```bash
mvn test
```

### Running in Development Mode
```bash
mvn hpi:run
```

### Creating Release
```bash
mvn release:prepare release:perform
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/jenkinsci/opsbox-utility-plugin/issues)
- **Documentation**: [Jenkins Plugin Documentation](https://plugins.jenkins.io/opsbox-utility-plugin/)
- **Community**: [Jenkins Community](https://www.jenkins.io/chat/)

## Author

**Seanly Liu** - [seanly.me@gmail.com](mailto:seanly.me@gmail.com)

## Changelog

### Version 1.0.0
- Initial release
- Job Build Name Parameter Definition
- Git Branches Environment Variables
- Migration scripts for upgrading from older versions

---

Made with ❤️ for the Jenkins community 