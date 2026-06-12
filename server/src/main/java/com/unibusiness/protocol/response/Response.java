package com.unibusiness.protocol.response;
public class Response {
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    private String status;
    private String action;
    private String message;
    private Object data;
    private Response() {}
    public static Response ok(String action, String message, Object data) { Response r = new Response(); r.status = OK; r.action = action; r.message = message; r.data = data; return r; }
    public static Response ok(String action, Object data) { return ok(action, "Sucesso.", data); }
    public static Response error(String action, String message) { Response r = new Response(); r.status = ERROR; r.action = action; r.message = message; return r; }
    public static Response push(String pushAction, Object data) { Response r = new Response(); r.status = OK; r.action = pushAction; r.message = "Notificação em tempo real."; r.data = data; return r; }
    public String getStatus() { return status; }
    public String getAction() { return action; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}