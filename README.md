# Spring AI Dify MCP 服务器

这是一个将Dify工作流转换为Spring AI MCP（模型控制平台）服务的集成项目。该项目使用Spring Boot和Spring AI提供基于Dify工作流的AI服务。

## 项目概述

Spring AI Dify MCP服务器允许你上传Dify工作流YAML文件，并使用Spring AI处理这些工作流，提供API接口来运行和管理这些工作流。

### 主要功能

- 上传和管理Dify工作流文件
- 解析Dify工作流结构
- 使用Spring AI执行工作流中的LLM节点
- 模拟工作流中的逻辑流程（问题分类、知识检索等）
- 提供REST API用于处理用户查询

## 技术栈

- Spring Boot 3.2
- Spring AI 0.8.0
- Spring WebFlux
- OpenAI API（通过Spring AI集成）

## 安装和运行

### 前提条件

- Java 17+
- Maven 3.6+
- OpenAI API密钥
- Dify工作流YAML文件

### 环境变量配置

在运行项目前，需要设置以下环境变量：

```bash
# OpenAI API配置
export OPENAI_API_KEY=your_openai_api_key
export OPENAI_BASE_URL=https://api.openai.com  # 可选，默认为OpenAI官方API

# Dify API配置（如需连接到Dify后端）
export DIFY_API_URL=http://your-dify-server:5000
export DIFY_API_KEY=your_dify_api_key
```

### 构建和运行

```bash
# 克隆代码库
git clone https://github.com/chenmaolin928/spring-ai-dify-mcp.git
cd spring-ai-dify-mcp

# 使用Maven构建项目
mvn clean package

# 运行应用
java -jar target/spring-ai-dify-mcp-0.0.1-SNAPSHOT.jar
```

## API使用

服务器启动后，可以通过以下API与服务器交互：

### 健康检查

```
GET /api/v1/health
```

### 上传工作流

```
POST /api/v1/workflow/upload
Content-Type: multipart/form-data

file: [YAML工作流文件]
```

### 列出工作流

```
GET /api/v1/workflow
```

### 获取工作流详情

```
GET /api/v1/workflow/{workflowId}
```

### 删除工作流

```
DELETE /api/v1/workflow/{workflowId}
```

### 使用工作流处理查询

```
POST /api/v1/workflow/{workflowId}/process
Content-Type: application/json

{
  "query": "你的问题或查询内容"
}
```

### 直接聊天（不使用工作流）

```
POST /api/v1/chat
Content-Type: application/json

{
  "message": "你的问题或查询内容"
}
```

## Dify工作流支持

目前支持以下Dify工作流节点类型：

- 开始节点（start）
- 问题分类节点（question-classifier）
- 知识检索节点（knowledge-retrieval）
- LLM节点（llm）
- 回答节点（answer）

## 示例

以下是使用curl命令与服务器交互的示例：

### 上传工作流

```bash
curl -X POST \
  http://localhost:8080/api/v1/workflow/upload \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@/path/to/your/workflow.yml'
```

### 使用工作流处理查询

```bash
curl -X POST \
  http://localhost:8080/api/v1/workflow/{workflowId}/process \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "我想了解产品的退货政策"
  }'
```

## 贡献和开发

欢迎提交问题和PR来改进这个项目。如有疑问，请通过GitHub Issues提交。

## 许可证

MIT