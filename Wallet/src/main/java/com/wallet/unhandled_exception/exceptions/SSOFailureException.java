package com.wallet.unhandled_exception.exceptions;

import java.io.Serial;

public class SSOFailureException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 7527645394540878099L;

    private boolean showLogout;

    public SSOFailureException(String message) {
        super(message);
    }

    public SSOFailureException(String message, boolean showLogout) {
        super(message);
        this.showLogout = showLogout;
    }

    public boolean isShowLogout() {
        return showLogout;
    }

    public void setShowLogout(boolean showLogout) {
        this.showLogout = showLogout;
    }

}
