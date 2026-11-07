package com.example.mcp.protocol;

/**
 * Standard Model Context Protocol (MCP) constants according to official specifications.
 */
public final class McpProtocolConstants {
    private McpProtocolConstants() {}

    public static final String JSONRPC_VERSION = "2.0";
    public static final String LATEST_PROTOCOL_VERSION = "2024-11-05";
    public static final String SUPPORTED_PROTOCOL_VERSION_V1 = "2024-11-05";
    public static final String SUPPORTED_PROTOCOL_VERSION_DRAFT = "2024-10-07";

    // Standard RPC Methods
    public static final String METHOD_INITIALIZE = "initialize";
    public static final String METHOD_INITIALIZED = "notifications/initialized";
    public static final String METHOD_PING = "ping";
    public static final String METHOD_TOOLS_LIST = "tools/list";
    public static final String METHOD_TOOLS_CALL = "tools/call";
    public static final String METHOD_RESOURCES_LIST = "resources/list";
    public static final String METHOD_RESOURCES_READ = "resources/read";
    public static final String METHOD_PROMPTS_LIST = "prompts/list";
    public static final String METHOD_PROMPTS_GET = "prompts/get";

    // Standard JSON-RPC Error Codes
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int SERVER_ERROR_START = -32000;
    public static final int SERVER_ERROR_END = -32099;
    public static final int PERMISSION_DENIED = -32001;
    public static final int TIMEOUT = -32002;
}
