package net.hyper.immortalcultivation.core.exceptions;

/**
 * Base exception for all Immortal Cultivation mod errors.
 * Provides consistent error handling and logging patterns.
 */
public class ICModException extends Exception {
    public ICModException(String message) {
        super(message);
    }

    public ICModException(String message, Throwable cause) {
        super(message, cause);
    }
}

