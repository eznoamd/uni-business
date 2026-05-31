package com.unibusiness.network;

import com.unibusiness.dto.Dto;

/**
 * Interface implementada por controllers que precisam receber
 * pushes assíncronos do servidor em tempo real.
 *
 * O TcpClient chama esses métodos a partir da thread de leitura.
 * Sempre use Platform.runLater() dentro das implementações
 * para atualizar a UI do JavaFX com segurança.
 *
 * Exemplo de uso no ChatController:
 *
 *   TcpClient.getInstance().setPushListener(this);
 *
 *   @Override
 *   public void onNovaMensagem(Dto.PushMensagem push) {
 *       Platform.runLater(() -> renderizarMensagemNaTela(...));
 *   }
 */
public interface PushListener {

    /**
     * Nova mensagem chegou em tempo real (PUSH_MENSAGEM).
     * Chamado para todos os participantes online da conversa.
     */
    void onNovaMensagem(Dto.PushMensagem push);

    /**
     * Contadores de não lidas recebidos logo após o login (PUSH_NAOLIDADAS).
     * Use para atualizar badges na lista de conversas.
     */
    void onNaoLidas(Dto.PushNaoLidas push);

    /**
     * Outro participante leu as mensagens da conversa (PUSH_MENSAGEM_LIDA).
     * Use para exibir "✓✓ lido" na UI.
     */
    void onMensagemLida(Dto.PushMensagemLida push);
}
