package com.unibusiness.network.handler;

import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.LogService;
import com.unibusiness.service.impl.LogServiceImpl;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LogHandler implements ActionHandler {

    private final LogService service = new LogServiceImpl();

    @Override
    public Response handle(Request req, ClientSession session) {
        Integer uid   = req.getInteger("usuarioId");
        Integer limit = req.getInteger("limit");

        List<Map<String, Object>> lista = service.listar(uid, limit).stream()
            .map(l -> Map.<String, Object>of(
                "id",       l.getId(),
                "acao",     l.getAcao(),
                "data",     l.getData().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "detalhes", l.getDetalhes() != null ? l.getDetalhes() : ""
            ))
            .collect(Collectors.toList());
        return Response.ok(Actions.LOG_LIST, lista);
    }
}
