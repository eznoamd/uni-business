package com.unibusiness.network;

import com.unibusiness.dto.Dto;

/**
 * Roteador global de pushes TCP.
 *
 * Problema original: cada aba (ChatController) registrava seu próprio PushListener
 * diretamente no TcpClient. Ao navegar para outra aba, o listener era sobrescrito
 * e os pushes de PUSH_STATUS_USUARIO, PUSH_MENSAGEM etc. se perdiam.
 *
 * Solução: um único PushRouter fica registrado no TcpClient para sempre.
 * Ele mantém dois slots:
 *
 *   - globalListener  → sempre ativo (DashboardController registra aqui)
 *   - currentListener → aba atualmente visível (ChatController etc.)
 *
 * Cada push é entregue primeiro ao globalListener (que pode atualizar badges,
 * contadores etc.) e depois ao currentListener se ele implementar o método.
 */
public final class PushRouter implements PushListener {

    private static final PushRouter INSTANCE = new PushRouter();

    private volatile PushListener globalListener;
    private volatile PushListener currentListener;

    private PushRouter() {}

    public static PushRouter getInstance() { return INSTANCE; }

    // ── Registro ──────────────────────────────────────────────────────────────

    /**
     * Registra o listener global (DashboardController).
     * Deve ser chamado uma única vez, no initialize() do dashboard.
     */
    public void setGlobalListener(PushListener listener) {
        this.globalListener = listener;
        // Garante que o router está registrado no TcpClient
        TcpClient.getInstance().setPushListener(this);
    }

    /**
     * Registra o listener da aba atual (ChatController, etc.).
     * Chame no initialize() de cada aba que precisa de pushes.
     * Chame com null (ou não chame) ao sair da aba.
     */
    public void setCurrentListener(PushListener listener) {
        this.currentListener = listener;
    }

    public void clearCurrentListener() {
        this.currentListener = null;
    }

    // ── Despacho ──────────────────────────────────────────────────────────────

    @Override
    public void onNovaMensagem(Dto.PushMensagem push) {
        dispatch(l -> l.onNovaMensagem(push));
    }

    @Override
    public void onNaoLidas(Dto.PushNaoLidas push) {
        dispatch(l -> l.onNaoLidas(push));
    }

    @Override
    public void onMensagemLida(Dto.PushMensagemLida push) {
        dispatch(l -> l.onMensagemLida(push));
    }

    @Override
    public void onStatusUsuario(Dto.PushStatusUsuario push) {
        dispatch(l -> l.onStatusUsuario(push));
    }

    @Override
    public void onDigitando(Dto.PushDigitando push) {
        dispatch(l -> l.onDigitando(push));
    }

    // ── Interno ───────────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface Dispatch {
        void to(PushListener l);
    }

    private void dispatch(Dispatch d) {
        PushListener g = globalListener;
        PushListener c = currentListener;
        if (g != null) { try { d.to(g); } catch (Exception ignored) {} }
        // Não entrega duas vezes se global == current
        if (c != null && c != g) { try { d.to(c); } catch (Exception ignored) {} }
    }
}