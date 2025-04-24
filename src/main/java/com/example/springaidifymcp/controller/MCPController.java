package com.example.springaidifymcp.controller;

import com.example.springaidifymcp.model.DifyWorkflow;
import com.example.springaidifymcp.service.DifyService;
import com.example.springaidifymcp.service.WorkflowProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class MCPController {

    private final DifyService difyService;
    private final WorkflowProcessor workflowProcessor;
    private final ChatClient chatClient;
    
    // 保存上传的工作流文件路径
    private final Map<String, String> uploadedWorkflows = new ConcurrentHashMap<>();

    public MCPController(DifyService difyService, WorkflowProcessor workflowProcessor, ChatClient chatClient) {
        this.difyService = difyService;
        this.workflowProcessor = workflowProcessor;
        this.chatClient = chatClient;
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "spring-ai-dify-mcp");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    /**
     * 上传Dify工作流文件
     */
    @PostMapping("/workflow/upload")
    public ResponseEntity<Map<String, Object>> uploadWorkflow(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 生成唯一ID作为工作流ID
            String workflowId = UUID.randomUUID().toString();
            
            // 保存上传的文件
            String fileName = workflowId + "_" + file.getOriginalFilename();
            Path tempFile = Files.createTempFile("workflow_", fileName);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            // 保存工作流信息
            uploadedWorkflows.put(workflowId, tempFile.toString());
            
            // 加载工作流验证有效性
            DifyWorkflow workflow = difyService.loadWorkflowFromYaml(tempFile.toString());
            
            response.put("workflowId", workflowId);
            response.put("name", workflow.getApp().getName());
            response.put("description", workflow.getApp().getDescription());
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("上传工作流失败: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "上传工作流失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 列出所有上传的工作流
     */
    @GetMapping("/workflow")
    public ResponseEntity<Map<String, Object>> listWorkflows() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> workflows = new HashMap<>();
            
            for (Map.Entry<String, String> entry : uploadedWorkflows.entrySet()) {
                String workflowId = entry.getKey();
                String filePath = entry.getValue();
                
                try {
                    DifyWorkflow workflow = difyService.loadWorkflowFromYaml(filePath);
                    Map<String, Object> workflowInfo = new HashMap<>();
                    workflowInfo.put("name", workflow.getApp().getName());
                    workflowInfo.put("description", workflow.getApp().getDescription());
                    workflowInfo.put("filePath", filePath);
                    
                    workflows.put(workflowId, workflowInfo);
                } catch (Exception e) {
                    log.warn("无法加载工作流 {}: {}", workflowId, e.getMessage());
                }
            }
            
            response.put("workflows", workflows);
            response.put("count", workflows.size());
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("列出工作流失败: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "列出工作流失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取特定工作流的详情
     */
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<Map<String, Object>> getWorkflow(@PathVariable String workflowId) {
        Map<String, Object> response = new HashMap<>();
        
        if (!uploadedWorkflows.containsKey(workflowId)) {
            response.put("status", "error");
            response.put("message", "工作流不存在");
            return ResponseEntity.notFound().build();
        }
        
        try {
            String filePath = uploadedWorkflows.get(workflowId);
            DifyWorkflow workflow = difyService.loadWorkflowFromYaml(filePath);
            
            response.put("workflowId", workflowId);
            response.put("name", workflow.getApp().getName());
            response.put("description", workflow.getApp().getDescription());
            response.put("mode", workflow.getApp().getMode());
            response.put("nodeCount", workflow.getWorkflow().getGraph().getNodes().size());
            response.put("modelInfo", difyService.extractModelInfo(workflow));
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取工作流失败: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "获取工作流失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除工作流
     */
    @DeleteMapping("/workflow/{workflowId}")
    public ResponseEntity<Map<String, Object>> deleteWorkflow(@PathVariable String workflowId) {
        Map<String, Object> response = new HashMap<>();
        
        if (!uploadedWorkflows.containsKey(workflowId)) {
            response.put("status", "error");
            response.put("message", "工作流不存在");
            return ResponseEntity.notFound().build();
        }
        
        try {
            String filePath = uploadedWorkflows.get(workflowId);
            Files.deleteIfExists(Path.of(filePath));
            uploadedWorkflows.remove(workflowId);
            
            response.put("status", "success");
            response.put("message", "工作流已删除");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("删除工作流失败: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "删除工作流失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 使用工作流处理用户查询
     */
    @PostMapping("/workflow/{workflowId}/process")
    public ResponseEntity<Map<String, Object>> processWorkflow(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (!uploadedWorkflows.containsKey(workflowId)) {
            response.put("status", "error");
            response.put("message", "工作流不存在");
            return ResponseEntity.notFound().build();
        }
        
        // 获取用户查询
        String query = (String) request.get("query");
        if (query == null || query.isEmpty()) {
            response.put("status", "error");
            response.put("message", "查询不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 加载工作流
            String filePath = uploadedWorkflows.get(workflowId);
            DifyWorkflow workflow = difyService.loadWorkflowFromYaml(filePath);
            
            // 处理工作流
            String result = workflowProcessor.processWorkflow(workflow, query);
            
            response.put("workflowId", workflowId);
            response.put("result", result);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("处理工作流失败: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "处理工作流失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 直接使用ChatClient进行会话
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        String message = (String) request.get("message");
        if (message == null || message.isEmpty()) {
            response.put("status", "error");
            response.put("message", "消息不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 使用Spring AI进行聊天
            Prompt prompt = new Prompt(new UserMessage(message));
            ChatResponse chatResponse = chatClient.call(prompt);
            String result = chatResponse.getResult().getOutput().getContent();
            
            response.put("result", result);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("聊天失败: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "聊天失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}