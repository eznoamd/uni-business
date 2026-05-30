package com.unibusiness.protocol.request;

public class Request {

    private String action;
    private String token;
    private java.util.Map<String, Object> payload;

    public Request() {}

    public String getAction()  { return action; }
    public void   setAction(String action) { this.action = action; }

    public String getToken()   { return token; }
    public void   setToken(String token) { this.token = token; }

    public java.util.Map<String, Object> getPayload() { return payload; }
    public void setPayload(java.util.Map<String, Object> payload) { this.payload = payload; }

    public Object get(String key) {
        return payload != null ? payload.get(key) : null;
    }

    public String getString(String key) {
        Object v = get(key);
        return v != null ? v.toString() : null;
    }

    public Integer getInteger(String key) {
        Object v = get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }
}
