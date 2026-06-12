package com.unibusiness.service;

import com.unibusiness.core.model.TarefaEntity;
import com.unibusiness.core.model.UsuarioEntity;
import com.unibusiness.core.service.impl.TarefaServiceImpl;
import com.unibusiness.core.service.impl.UsuarioServiceImpl;
import com.unibusiness.dto.Dto;
import com.unibusiness.session.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de tarefas — antes via TCP (TAREFA_*), agora via TarefaService (JPA) direto.
 *
 * Diferente da versão TCP, aqui o "responsavel" da tarefa é de fato persistido
 * (campo usuariosAtribuidos da TarefaEntity), então o Dto.Tarefa.responsavel
 * passa a vir preenchido corretamente.
 */
public class TarefaService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final com.unibusiness.core.service.TarefaService tarefaService = new TarefaServiceImpl();
    private final com.unibusiness.core.service.UsuarioService usuarioService = new UsuarioServiceImpl();

    // ── Listar ────────────────────────────────────────────────────────────────

    public List<Dto.Tarefa> listar() {
        return tarefaService.listAll().stream().map(TarefaService::toDto).collect(Collectors.toList());
    }

    // ── Criar ─────────────────────────────────────────────────────────────────

    public Dto.Tarefa criar(String titulo, String descricao, String prioridade,
                             String dataFim, Integer responsavelId) {
        UsuarioEntity criador = SessionManager.getInstance().getUsuario();

        TarefaEntity tarefa = new TarefaEntity(
            titulo,
            "PENDENTE",
            prioridade != null ? prioridade : "BAIXA",
            LocalDateTime.now(),
            criador
        );

        if (descricao != null && !descricao.isBlank()) {
            tarefa.setDescricao(descricao);
        }

        if (dataFim != null && !dataFim.isBlank()) {
            LocalDateTime fim = converterParaIsoDatetime(dataFim);
            if (fim != null) tarefa.setDataFim(fim);
        }

        if (responsavelId != null) {
            usuarioService.findById(responsavelId).ifPresent(resp -> {
                HashSet<UsuarioEntity> atribuidos = new HashSet<>();
                atribuidos.add(resp);
                tarefa.setUsuariosAtribuidos(atribuidos);
            });
        }

        try {
            return toDto(tarefaService.create(tarefa));
        } catch (Exception e) {
            return null;
        }
    }

    // ── Atualizar ─────────────────────────────────────────────────────────────

    public boolean atualizar(int id, String status, String prioridade) {
        return tarefaService.findById(id).map(t -> {
            t.setStatus(status);
            t.setPrioridade(prioridade);
            tarefaService.update(t);
            return true;
        }).orElse(false);
    }

    // ── Deletar ───────────────────────────────────────────────────────────────

    public boolean deletar(int id) {
        try {
            tarefaService.delete(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LocalDateTime converterParaIsoDatetime(String data) {
        try {
            if (data.length() == 10) {
                return LocalDateTime.parse(data + "T00:00:00", ISO);
            }
            return LocalDateTime.parse(data, ISO);
        } catch (Exception e) {
            return null;
        }
    }

    private static Dto.Tarefa toDto(TarefaEntity t) {
        Dto.Tarefa dto = new Dto.Tarefa();
        dto.id = t.getId();
        dto.titulo = t.getTitulo();
        dto.descricao = t.getDescricao();
        dto.status = t.getStatus();
        dto.prioridade = t.getPrioridade();
        dto.dataInicio = t.getDataInicio() != null ? t.getDataInicio().format(ISO) : null;
        dto.dataFim = t.getDataFim() != null ? t.getDataFim().format(ISO) : null;

        UsuarioEntity responsavel = t.getUsuariosAtribuidos().stream().findFirst().orElse(null);
        if (responsavel != null) {
            dto.responsavelId = responsavel.getId();
            dto.responsavel = responsavel.getNome();
        }
        return dto;
    }
}
