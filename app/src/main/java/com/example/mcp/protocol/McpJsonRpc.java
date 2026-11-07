package com.example.mcp.protocol;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Message validation, construction, and serialization for MCP JSON-RPC 2.0 communication.
 */
public class McpJsonRpc {

    private final JSONObject rawJson;

    public McpJsonRpc(JSONObject rawJson) {
        this.rawJson = rawJson != null ? rawJson : new JSONObject();
    }

    public static McpJsonRpc parse(String jsonString) throws JSONException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new JSONException("Empty JSON-RPC payload");
        }
        JSONObject obj = new JSONObject(jsonString);
        validateJsonRpc(obj);
        return new McpJsonRpc(obj);
    }

    public static McpJsonRpc createRequest(long id, String method, JSONObject params) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("jsonrpc", McpProtocolConstants.JSONRPC_VERSION);
            obj.put("id", id);
            obj.put("method", method);
            if (params != null) {
                obj.put("params", params);
            } else {
                obj.put("params", new JSONObject());
            }
        } catch (JSONException ignored) {}
        return new McpJsonRpc(obj);
    }

    public static McpJsonRpc createNotification(String method, JSONObject params) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("jsonrpc", McpProtocolConstants.JSONRPC_VERSION);
            obj.put("method", method);
            if (params != null) {
                obj.put("params", params);
            }
        } catch (JSONException ignored) {}
        return new McpJsonRpc(obj);
    }

    public static McpJsonRpc createResponse(long id, JSONObject result) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("jsonrpc", McpProtocolConstants.JSONRPC_VERSION);
            obj.put("id", id);
            obj.put("result", result != null ? result : new JSONObject());
        } catch (JSONException ignored) {}
        return new McpJsonRpc(obj);
    }

    public static McpJsonRpc createErrorResponse(long id, int code, String message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("jsonrpc", McpProtocolConstants.JSONRPC_VERSION);
            obj.put("id", id);
            JSONObject err = new JSONObject();
            err.put("code", code);
            err.put("message", message != null ? message : "Unknown error");
            obj.put("error", err);
        } catch (JSONException ignored) {}
        return new McpJsonRpc(obj);
    }

    private static void validateJsonRpc(JSONObject obj) throws JSONException {
        if (!obj.has("jsonrpc") || !"2.0".equals(obj.optString("jsonrpc"))) {
            throw new JSONException("Invalid JSON-RPC version: expected '2.0'");
        }
    }

    public boolean isRequest() {
        return rawJson.has("id") && rawJson.has("method");
    }

    public boolean isNotification() {
        return !rawJson.has("id") && rawJson.has("method");
    }

    public boolean isResponse() {
        return rawJson.has("id") && (rawJson.has("result") || rawJson.has("error"));
    }

    public boolean isError() {
        return rawJson.has("error");
    }

    public long getId() {
        return rawJson.optLong("id", -1L);
    }

    public String getMethod() {
        return rawJson.optString("method", "");
    }

    public JSONObject getParams() {
        return rawJson.optJSONObject("params");
    }

    public JSONObject getResult() {
        return rawJson.optJSONObject("result");
    }

    public JSONObject getError() {
        return rawJson.optJSONObject("error");
    }

    public int getErrorCode() {
        JSONObject err = getError();
        return err != null ? err.optInt("code", McpProtocolConstants.INTERNAL_ERROR) : 0;
    }

    public String getErrorMessage() {
        JSONObject err = getError();
        return err != null ? err.optString("message", "Unknown error") : "";
    }

    public JSONObject getRawJson() {
        return rawJson;
    }

    public String serialize() {
        return rawJson.toString();
    }

    @Override
    public String toString() {
        return serialize();
    }
}
