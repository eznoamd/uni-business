package com.unibusiness.network.handler;

import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;

public interface ActionHandler {
    Response handle(Request request, ClientSession session);
}
