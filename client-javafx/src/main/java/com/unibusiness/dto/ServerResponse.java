package com.unibusiness.dto;

import com.google.gson.JsonElement;

/**
 * Envelope genérico de toda resposta/push do servidor.
 *
 * JSON recebido:
 * {
 *   "status":  "OK" | "ERROR",
 *   "action":  "NOME_DA_ACTION",
 *   "message": "...",
 *   "data":    { ... }   ← pode ser qualquer objeto JSON
 * }
 *
 * O campo 'data' é mantido como JsonElement bruto para que
 * cada service possa desserializar para o DTO correto.
 */
public class ServerResponse {

    private String status;
    private String action;
    private String message;
    private JsonElement data;

    public boolean isOk()    { return "OK".equals(status); }
    public boolean isError() { return "ERROR".equals(status); }

    public String getStatus()      { return status; }
    public String getAction()      { return action; }
    public String getMessage()     { return message; }
    public JsonElement getData()   { return data; }
}
