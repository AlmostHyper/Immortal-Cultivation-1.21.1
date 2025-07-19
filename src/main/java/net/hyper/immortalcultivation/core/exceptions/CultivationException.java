package net.hyper.immortalcultivation.core.exceptions;

import net.hyper.immortalcultivation.core.attachments.ICultivationData;

/**
 * Thrown when cultivation-related operations fail.
 * Examples: breakthrough failures, realm transitions, cultivation accumulation errors.
 */
public class CultivationException extends ICModException {
    private final ICultivationData.CultivationType type;

    public CultivationException(String message, ICultivationData.CultivationType type) {
        super(message);
        this.type = type;
    }

    public ICultivationData.CultivationType getCultivationType() {
        return type;
    }
}
