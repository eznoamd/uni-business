package com.unibusiness.network.handler;

import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;

/**
 * Contrato de todos os handlers de action.
 */
public interface ActionHandler {
    Response handle(Request request, ClientSession session);
}
