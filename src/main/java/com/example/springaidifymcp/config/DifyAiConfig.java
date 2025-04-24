package com.example.springaidifymcp.config;

import com.example.springaidifymcp.service.DifyService;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * 配置类，用于设置Dify与Spring AI的集成
 */
@Configuration
public class DifyAiConfig {

    private final DifyService difyService;
    private final OpenAiChatClient openAiChatClient;

    public DifyAiConfig(DifyService difyService, OpenAiChatClient openAiChatClient) {
        this.difyService = difyService;
        this.openAiChatClient = openAiChatClient;
    }

    /**
     * 创建一个包装了Dify功能的ChatClient
     */
    @Bean
    @Primary
    public ChatClient difyIntegratedChatClient() {
        return new ChatClient() {
            
            @Override
            public ChatResponse call(Prompt prompt) {
                // 默认使用OpenAI实现，如果需要特定工作流处理，可以根据消息内容或其他条件判断
                return openAiChatClient.call(prompt);
            }

            @Override
            public ChatResponse call(Message message) {
                // 默认使用OpenAI实现
                return openAiChatClient.call(message);
            }

            @Override
            public ChatResponse call(List<Message> messages) {
                // 默认使用OpenAI实现
                return openAiChatClient.call(messages);
            }

            @Override
            public ChatResponse call(UserMessage userMessage) {
                // 默认使用OpenAI实现
                return openAiChatClient.call(userMessage);
            }
        };
    }

    /**
     * 配置OpenAI选项的Bean
     */
    @Bean
    public OpenAiChatOptions openAiChatOptions() {
        return OpenAiChatOptions.builder()
                .withModel("gpt-3.5-turbo")
                .withTemperature(0.7f)
                .withMaxTokens(2000)
                .build();
    }
}