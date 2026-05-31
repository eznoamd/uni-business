package com.unibusiness.network.handler;

import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.PontoService;
import com.unibusiness.service.impl.PontoServiceImpl;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PontoHandler implements ActionHandler {

    private final PontoService service = new PontoServiceImpl();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.PONTO_REGISTRAR_ENTRADA -> registrarEntrada(session);
            case Actions.PONTO_REGISTRAR_SAIDA   -> registrarSaida(session);
            case Actions.PONTO_LIST              -> listar(req, session);
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    private Response registrarEntrada(ClientSession session) {
        try {
            var ponto = service.registrarEntrada(session.getUsuario().getId());
            return Response.ok(Actions.PONTO_REGISTRAR_ENTRADA, Map.of(
                "id",          ponto.getId(),
                "horaEntrada", ponto.getHoraEntrada().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
        } catch (IllegalStateException e) {
            return Response.error(Actions.PONTO_REGISTRAR_ENTRADA, e.getMessage());
        }
    }

    private Response registrarSaida(ClientSession session) {
        try {
            var ponto = service.registrarSaida(session.getUsuario().getId());
            return Response.ok(Actions.PONTO_REGISTRAR_SAIDA, Map.of(
                "id",        ponto.getId(),
                "horaSaida", ponto.getHoraSaida().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
        } catch (IllegalStateException e) {
            return Response.error(Actions.PONTO_REGISTRAR_SAIDA, e.getMessage());
        }
    }

    private Response listar(Request req, ClientSession session) {
        Integer uid = req.getInteger("usuarioId");
        if (uid == null) uid = session.getUsuario().getId();

        List<Map<String, Object>> lista = service.listar(uid).stream()
            .map(r -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id",          r.getId());
                m.put("data",        r.getData().toString());
                m.put("horaEntrada", r.getHoraEntrada() != null
                    ? r.getHoraEntrada().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
                m.put("horaSaida",   r.getHoraSaida() != null
                    ? r.getHoraSaida().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
                return m;
            })
            .collect(Collectors.toList());
        return Response.ok(Actions.PONTO_LIST, lista);
    }
}
