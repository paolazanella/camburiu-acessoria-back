package br.com.camburiu.camburiu_acessoria.model;

import java.io.Serializable;

public class JwtRequest implements Serializable {
    private static final long serialVersionUID = 5926468583005150707L;

    private String username; // 🔥 Pode ser email
    private String password;

    public JwtRequest() {}

    public JwtRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Método de conveniência para usar email como username
    public String getEmail() {
        return this.username;
    }

    public void setEmail(String email) {
        this.username = email;
    }
}