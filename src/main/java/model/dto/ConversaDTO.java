package model.dto;

public class ConversaDTO {
    private Integer id;
    private String tipo;

    // Construtores
    public ConversaDTO() {}

    public ConversaDTO(Integer id, String tipo) {
        this.id = id;
        this.tipo = tipo;
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
