package com.unibusiness.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unibusiness.dto.Dto;
import com.unibusiness.dto.ServerResponse;
import com.unibusiness.network.TcpClient;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Serviço de usuários — usado para popular a lista de participantes
 * na criação de conversas e exibição de funcionários.
 *
 * Depende da action USUARIO_LIST no servidor, que deve retornar
 * um array JSON de objetos Dto.Usuario.
 */
public class UsuarioService {

    private final TcpClient client = TcpClient.getInstance();
    private final Gson gson = client.getGson();

    /**
     * Lista todos os usuários ativos do sistema.
     * @return lista de usuários, ou lista vazia em caso de erro.
     */
    public List<Dto.Usuario> listarUsuarios() {
        ServerResponse resp = client.send("USUARIO_LIST");
        if (resp.isError() || resp.getData() == null) return List.of();

        Type listType = new TypeToken<List<Dto.Usuario>>(){}.getType();
        return gson.fromJson(resp.getData(), listType);
    }
}