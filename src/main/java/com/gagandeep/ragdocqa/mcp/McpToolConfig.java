package com.gagandeep.ragdocqa.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    /**
     * Spring AI's MCP server auto-configuration picks up any ToolCallbackProvider
     * bean and exposes its @Tool-annotated methods over the MCP protocol - no
     * manual JSON-RPC handling or schema authoring needed.
     */
    @Bean
    public ToolCallbackProvider ragToolCallbackProvider(RagMcpTools ragMcpTools) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(ragMcpTools)
            .build();
    }
}
