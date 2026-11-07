package com.example.mcp.transport;

import com.example.mcp.protocol.McpJsonRpc;

/**
 * Transport contract for communicating with Model Context Protocol servers.
 */
public interface McpTransport {
    void connect() throws Exception;
    void disconnect();
    boolean isConnected();
    McpJsonRpc sendRequest(McpJsonRpc request, long timeoutMs) throws Exception;
    void sendNotification(McpJsonRpc notification) throws Exception;
    String getTransportName();
}
