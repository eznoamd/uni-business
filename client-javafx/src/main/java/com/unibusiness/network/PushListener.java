package com.unibusiness.network;

import com.unibusiness.dto.Dto;

/**
 * Interface implementada por controllers que precisam receber
 * pushes assíncronos do servidor em tempo real.
 *
 * O TcpClient chama esses métodos a partir da thread de leitura.
 * Sempre use Platform.runLater() dentro das implementações para
 * atualizar a UI do JavaFX com segurança.
 *
 * Métodos com implementação padrão vazia permitem que controllers
 * implementem apenas os pushes que precisam.
 */
public interface PushListener {

    /** Nova mensagem chegou em tempo real (PUSH_MENSAGEM). */
    void onNovaMensagem(Dto.PushMensagem push);

    /** Contadores de não lidas recebidos logo após o login (PUSH_NAOLIDADAS). */
    void onNaoLidas(Dto.PushNaoLidas push);

    /** Outro participante leu as mensagens da conversa (PUSH_MENSAGEM_LIDA). */
    void onMensagemLida(Dto.PushMensagemLida push);

    /** Status de presença de um usuário mudou (PUSH_STATUS_USUARIO). */
    default void onStatusUsuario(Dto.PushStatusUsuario push) {}

    /** Um participante está digitando em uma conversa (PUSH_DIGITANDO). */
    default void onDigitando(Dto.PushDigitando push) {}
}
