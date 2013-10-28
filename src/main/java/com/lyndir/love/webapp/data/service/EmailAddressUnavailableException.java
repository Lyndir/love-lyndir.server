package com.lyndir.love.webapp.data.service;

public class EmailAddressUnavailableException extends ModelException {

    public EmailAddressUnavailableException(final String address) {
        super( "Address unavailable: `" + address + "`" );
    }
}
