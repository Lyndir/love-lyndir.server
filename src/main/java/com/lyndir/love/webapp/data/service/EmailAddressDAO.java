package com.lyndir.love.webapp.data.service;

import com.lyndir.love.webapp.data.EmailAddress;


/**
 * @author lhunath
 */
public interface EmailAddressDAO {

    EmailAddress newAddress(String address)
            throws EmailAddressUnavailableException;
}
