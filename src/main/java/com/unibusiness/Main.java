package com.unibusiness;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.service.UsuarioService;
import com.unibusiness.service.impl.UsuarioServiceImpl;
import com.unibusiness.model.UsuarioEntity;

public class Main {
    public static void main(String[] args) {
        UsuarioService usuarioService = new UsuarioServiceImpl();

        PersistenceManager.close();
    }
}