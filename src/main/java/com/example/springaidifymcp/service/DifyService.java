package com.example.springaidifymcp.service;

import com.example.springaidifymcp.model.DifyWorkflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class DifyService {

    private final WebClient webClient;
    private final Map<String, DifyWorkflow> workflowCache = new ConcurrentHashMap<>();

    @Value("${dify.api.api-key}")
    private String apiKey;

    public DifyService(@Value("${dify.api.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 从YAML文件加载Dify工作流
     */
    public DifyWorkflow loadWorkflowFromYaml(String yamlPath) {
        try {
            if (workflowCache.containsKey(yamlPath)) {
                return workflowCache.get(yamlPath);
            }

            Yaml yaml = new Yaml();
            InputStream inputStream = Files.newInputStream(Path.of(yamlPath));
            DifyWorkflow workflow = yaml.loadAs(inputStream, DifyWorkflow.class);
            workflowCache.put(yamlPath, workflow);
            log.info("成功加载工作流: {}", workflow.getApp().getName());
            return workflow;
        } catch (Exception e) {
            log.error("加载工作流失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法加载工作流文件: " + yamlPath, e);
        }
    }

    /**
     * 向Dify API发送请求
     */
    public Mono<Map<String, Object>> sendMessageToDify(String appId, String query, Map<String, Object> inputs) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("inputs", inputs);
        requestBody.put("response_mode", "streaming");
        requestBody.put("user", "spring-ai-user");

        return webClient.post()
                .uri("/v1/app-api/{appId}/chat-messages", appId)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(response -> log.debug("Dify API响应: {}", response))
                .doOnError(error -> log.error("Dify API错误: {}", error.getMessage(), error));
    }

    /**
     * 分析工作流并提取模型信息
     */
    public Map<String, Object> extractModelInfo(DifyWorkflow workflow) {
        Map<String, Object> modelInfo = new HashMap<>();
        
        // 提取所有LLM节点的模型信息
        workflow.getWorkflow().getGraph().getNodes().stream()
                .filter(node -> "llm".equals(node.getData().getType()))
                .forEach(node -> {
                    String nodeId = node.getId();
                    Map<String, Object> model = node.getData().getModel();
                    String modelName = (String) model.get("name");
                    String provider = (String) model.get("provider");
                    
                    Map<String, Object> nodeInfo = new HashMap<>();
                    nodeInfo.put("modelName", modelName);
                    nodeInfo.put("provider", provider);
                    nodeInfo.put("promptTemplates", node.getData().getPromptTemplate());
                    
                    modelInfo.put(nodeId, nodeInfo);
                });
                
        return modelInfo;
    }
}