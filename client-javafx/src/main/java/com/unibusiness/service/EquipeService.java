package com.unibusiness.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unibusiness.dto.Dto;
import com.unibusiness.dto.ServerResponse;
import com.unibusiness.network.TcpClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class EquipeService {

    private final TcpClient client = TcpClient.getInstance();
    private final Gson      gson   = client.getGson();

    public List<Dto.Equipe> listar() {
        ServerResponse resp = client.send("EQUIPE_LIST");
        if (resp.isError() || resp.getData() == null) return List.of();
        Type t = new TypeToken<List<Dto.Equipe>>(){}.getType();
        return gson.fromJson(resp.getData(), t);
    }

    public Dto.Equipe criar(String nome) {
        ServerResponse resp = client.send("EQUIPE_CREATE", Map.of("nome", nome));
        if (resp.isError() || resp.getData() == null) return null;
        return gson.fromJson(resp.getData(), Dto.Equipe.class);
    }

    public boolean atualizar(int id, String nome) {
        ServerResponse resp = client.send("EQUIPE_UPDATE", Map.of("id", id, "nome", nome));
        return resp.isOk();
    }

    public boolean deletar(int id) {
        ServerResponse resp = client.send("EQUIPE_DELETE", Map.of("id", id));
        return resp.isOk();
    }
}
