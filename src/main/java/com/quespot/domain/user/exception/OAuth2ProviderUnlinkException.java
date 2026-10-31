package com.quespot.domain.user.exception;

public class OAuth2ProviderUnlinkException extends RuntimeException {

    private final boolean retryable;

    private OAuth2ProviderUnlinkException(boolean retryable, Throwable cause) {
        super(cause);
        this.retryable = retryable;
    }

    public static OAuth2ProviderUnlinkException retryable(Throwable cause) {
        return new OAuth2ProviderUnlinkException(true, cause);
    }

    public static OAuth2ProviderUnlinkException permanent(Throwable cause) {
        return new OAuth2ProviderUnlinkException(false, cause);
    }

    public boolean isRetryable() {
        return retryable;
    }
}
