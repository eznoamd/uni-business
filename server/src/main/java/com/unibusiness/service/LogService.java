package com.unibusiness.service;
import com.unibusiness.model.LogSistemaEntity;
import java.util.List;
public interface LogService { List<LogSistemaEntity> listar(Integer usuarioId, Integer limit); }