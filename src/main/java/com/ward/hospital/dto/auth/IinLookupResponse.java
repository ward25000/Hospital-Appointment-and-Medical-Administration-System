package com.ward.hospital.dto.auth;

/**
 * Result of the search by IIN. It deliberately carries no personal data,
 * because the search is available before authentication.
 */
public record IinLookupResponse(Status status) {

    public enum Status {
        /** No card in the clinic database. */
        NEW,
        /** The card exists and will be linked to the new account. */
        CARD_FOUND,
        /** The card already has an account. */
        REGISTERED
    }
}
