package com.example.springaidifymcp.service;

import com.example.springaidifymcp.model.DifyWorkflow;
import com.example.springaidifymcp.model.DifyWorkflow.Graph.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 工作流处理器，负责执行工作流中的节点逻辑
 */
@Service
@Slf4j
public class WorkflowProcessor {

    private final DifyService difyService;
    private final ChatClient chatClient;

    public WorkflowProcessor(DifyService difyService, ChatClient chatClient) {
        this.difyService = difyService;
        this.chatClient = chatClient;
    }

    /**
     * 处理一个完整的工作流
     */
    public String processWorkflow(DifyWorkflow workflow, String userQuery) {
        log.info("开始处理工作流: {}", workflow.getApp().getName());
        
        // 获取起始节点
        Node startNode = findStartNode(workflow);
        if (startNode == null) {
            throw new IllegalStateException("无法找到起始节点");
        }
        
        // 执行工作流节点
        Map<String, Object> context = new HashMap<>();
        context.put("sys.query", userQuery);
        
        // 模拟工作流执行过程
        String result = executeWorkflowFromNode(workflow, startNode.getId(), context);
        
        log.info("工作流处理完成，返回结果");
        return result;
    }
    
    /**
     * 从指定节点开始执行工作流
     */
    private String executeWorkflowFromNode(DifyWorkflow workflow, String nodeId, Map<String, Object> context) {
        // 获取当前节点
        Node currentNode = findNodeById(workflow, nodeId);
        if (currentNode == null) {
            throw new IllegalStateException("找不到节点: " + nodeId);
        }
        
        log.debug("执行节点: {} ({})", currentNode.getData().getTitle(), currentNode.getData().getType());
        
        // 根据节点类型处理
        String nodeType = currentNode.getData().getType();
        String result;
        
        switch (nodeType) {
            case "start":
                // 起始节点，找到下一个节点继续执行
                result = executeNextNode(workflow, nodeId, context);
                break;
                
            case "question-classifier":
                // 问题分类节点
                result = executeQuestionClassifier(workflow, currentNode, context);
                break;
                
            case "knowledge-retrieval":
                // 知识检索节点
                result = executeKnowledgeRetrieval(workflow, currentNode, context);
                break;
                
            case "llm":
                // LLM节点
                result = executeLlmNode(workflow, currentNode, context);
                break;
                
            case "answer":
                // 回答节点
                result = executeAnswerNode(workflow, currentNode, context);
                break;
                
            default:
                log.warn("未知节点类型: {}", nodeType);
                result = "未能处理该节点类型: " + nodeType;
                break;
        }
        
        return result;
    }
    
    /**
     * 执行问题分类节点
     */
    private String executeQuestionClassifier(DifyWorkflow workflow, Node node, Map<String, Object> context) {
        String query = (String) context.get("sys.query");
        log.debug("执行问题分类，用户查询: {}", query);
        
        // 使用Spring AI进行分类
        List<Object> classes = node.getData().getClasses();
        if (classes == null || classes.isEmpty()) {
            log.warn("问题分类节点没有定义类别");
            return executeNextNode(workflow, node.getId(), context);
        }
        
        // 构建分类提示
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请将以下问题分类到最合适的类别中，只返回类别ID：\n\n");
        promptBuilder.append("问题: ").append(query).append("\n\n");
        promptBuilder.append("类别:\n");
        
        classes.forEach(cls -> {
            if (cls instanceof Map) {
                Map<String, Object> classMap = (Map<String, Object>) cls;
                promptBuilder.append("- ID: ").append(classMap.get("id"))
                        .append(", 名称: ").append(classMap.get("name")).append("\n");
            } else if (cls instanceof DifyWorkflow.Graph.Node.NodeData.NodeClass) {
                DifyWorkflow.Graph.Node.NodeData.NodeClass classObj = (DifyWorkflow.Graph.Node.NodeData.NodeClass) cls;
                promptBuilder.append("- ID: ").append(classObj.getId())
                        .append(", 名称: ").append(classObj.getName()).append("\n");
            }
        });
        
        // 发送请求到模型
        Prompt prompt = new Prompt(new UserMessage(promptBuilder.toString()));
        String classificationResult = chatClient.call(prompt).getResult().getOutput().getContent();
        classificationResult = classificationResult.trim();
        
        log.debug("分类结果: {}", classificationResult);
        
        // 根据分类结果找到下一个节点
        String nextEdgeId = findEdgeBySourceAndSourceHandle(workflow, node.getId(), classificationResult)
                .map(edge -> edge.getTarget())
                .orElseGet(() -> {
                    log.warn("无法找到分类结果对应的边，使用默认边");
                    return findDefaultNextEdge(workflow, node.getId()).map(edge -> edge.getTarget()).orElse(null);
                });
        
        if (nextEdgeId == null) {
            log.warn("没有找到下一个节点");
            return "无法继续处理，未找到下一个节点";
        }
        
        return executeWorkflowFromNode(workflow, nextEdgeId, context);
    }
    
    /**
     * 执行知识检索节点
     */
    private String executeKnowledgeRetrieval(DifyWorkflow workflow, Node node, Map<String, Object> context) {
        String query = (String) context.get("sys.query");
        log.debug("执行知识检索，用户查询: {}", query);
        
        // 这里应该调用实际的知识检索，但示例中模拟检索结果
        String retrievalResult = "这是一个模拟的知识检索结果，包含了与查询相关的信息。";
        
        // 将检索结果添加到上下文
        context.put(node.getId() + ".result", retrievalResult);
        
        // 执行下一个节点
        return executeNextNode(workflow, node.getId(), context);
    }
    
    /**
     * 执行LLM节点
     */
    private String executeLlmNode(DifyWorkflow workflow, Node node, Map<String, Object> context) {
        log.debug("执行LLM节点");
        
        // 获取上下文内容
        String contextContent = "";
        if (node.getData().getContext() != null && node.getData().getContext().isEnabled()) {
            List<String> variableSelector = node.getData().getContext().getVariableSelector();
            if (variableSelector != null && variableSelector.size() >= 2) {
                String contextNodeId = variableSelector.get(0);
                String contextVarName = variableSelector.get(1);
                String contextKey = contextNodeId + "." + contextVarName;
                contextContent = (String) context.getOrDefault(contextKey, "");
            }
        }
        
        // 获取提示模板
        List<Map<String, Object>> promptTemplates = node.getData().getPromptTemplate();
        if (promptTemplates == null || promptTemplates.isEmpty()) {
            log.warn("LLM节点没有提示模板");
            return "无法处理，LLM节点没有提示模板";
        }
        
        // 构建提示
        String systemPrompt = "";
        for (Map<String, Object> template : promptTemplates) {
            if ("system".equals(template.get("role"))) {
                systemPrompt = (String) template.get("text");
                break;
            }
        }
        
        // 替换提示中的变量
        if (systemPrompt.contains("{{#context#}}")) {
            systemPrompt = systemPrompt.replace("{{#context#}}", contextContent);
        }
        
        String userQuery = (String) context.get("sys.query");
        
        // 使用Spring AI执行LLM请求
        Prompt prompt = new Prompt(
                Arrays.asList(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userQuery)
                )
        );
        
        ChatResponse response = chatClient.call(prompt);
        String llmResult = response.getResult().getOutput().getContent();
        
        // 保存结果到上下文
        context.put(node.getId() + ".text", llmResult);
        
        // 执行下一个节点
        return executeNextNode(workflow, node.getId(), context);
    }
    
    /**
     * 执行回答节点
     */
    private String executeAnswerNode(DifyWorkflow workflow, Node node, Map<String, Object> context) {
        log.debug("执行回答节点");
        
        // 获取回答内容
        String answer = node.getData().getAnswer();
        
        // 如果回答包含变量引用，替换为上下文中的值
        if (answer != null && answer.startsWith("{{#") && answer.endsWith("#}}")) {
            String varRef = answer.substring(3, answer.length() - 3);
            answer = (String) context.getOrDefault(varRef, "无法获取回答内容");
        }
        
        return answer;
    }
    
    /**
     * 查找并执行下一个节点
     */
    private String executeNextNode(DifyWorkflow workflow, String currentNodeId, Map<String, Object> context) {
        Optional<DifyWorkflow.Graph.Edge> nextEdge = findDefaultNextEdge(workflow, currentNodeId);
        
        if (nextEdge.isPresent()) {
            String nextNodeId = nextEdge.get().getTarget();
            return executeWorkflowFromNode(workflow, nextNodeId, context);
        } else {
            log.warn("没有找到下一个节点");
            return "工作流执行完成";
        }
    }
    
    // 辅助方法
    
    /**
     * 查找起始节点
     */
    private Node findStartNode(DifyWorkflow workflow) {
        return workflow.getWorkflow().getGraph().getNodes().stream()
                .filter(node -> "start".equals(node.getData().getType()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 通过ID查找节点
     */
    private Node findNodeById(DifyWorkflow workflow, String nodeId) {
        return workflow.getWorkflow().getGraph().getNodes().stream()
                .filter(node -> nodeId.equals(node.getId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 查找默认的下一个边
     */
    private Optional<DifyWorkflow.Graph.Edge> findDefaultNextEdge(DifyWorkflow workflow, String sourceNodeId) {
        return workflow.getWorkflow().getGraph().getEdges().stream()
                .filter(edge -> sourceNodeId.equals(edge.getSource()))
                .findFirst();
    }
    
    /**
     * 按源节点ID和源句柄查找边
     */
    private Optional<DifyWorkflow.Graph.Edge> findEdgeBySourceAndSourceHandle(
            DifyWorkflow workflow, String sourceNodeId, String sourceHandle) {
        return workflow.getWorkflow().getGraph().getEdges().stream()
                .filter(edge -> sourceNodeId.equals(edge.getSource()) && sourceHandle.equals(edge.getSourceHandle()))
                .findFirst();
    }
}