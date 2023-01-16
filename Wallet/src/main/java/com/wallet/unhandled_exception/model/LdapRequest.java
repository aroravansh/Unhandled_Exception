package com.wallet.unhandled_exception.model;

import java.io.Serial;
import java.io.Serializable;

public class LdapRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1761243422480552716L;
    private String accessToken;
    private String tempCode;
    private String email;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTempCode() {
        return tempCode;
    }

    public void setTempCode(String tempCode) {
        this.tempCode = tempCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String emailId) {
        this.email = emailId;
    }
}
