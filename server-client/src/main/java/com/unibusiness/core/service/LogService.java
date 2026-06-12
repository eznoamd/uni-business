package com.unibusiness.core.service;
import com.unibusiness.core.model.LogSistemaEntity;
import java.util.List;
public interface LogService { List<LogSistemaEntity> listar(Integer usuarioId, Integer limit); }