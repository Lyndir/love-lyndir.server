package com.lyndir.love.webapp.data.service.impl.jpa;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.lyndir.lhunath.opal.jpa.Persist;
import com.lyndir.love.webapp.data.EmailAddress;
import com.lyndir.love.webapp.data.service.EmailAddressDAO;
import com.lyndir.love.webapp.data.service.EmailAddressUnavailableException;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;


/**
 * @author lhunath
 */
public class EmailAddressDAOImpl implements EmailAddressDAO {

    private final EntityManager   db;
    private final CriteriaBuilder cb;

    @Inject
    public EmailAddressDAOImpl(final Persist persist) {
        this.db = persist.getEntityManager();
        this.cb = db.getCriteriaBuilder();
    }

    @Override
    public EmailAddress newAddress(final String address)
            throws EmailAddressUnavailableException {
        EmailAddress emailAddress = Iterables.getFirst(
                db.createQuery( "SELECT e FROM EmailAddress e WHERE e.address = :address", EmailAddress.class ) //
                        .setParameter( "address", address ) //
                        .getResultList(), null );
        if (emailAddress != null)
            throw new EmailAddressUnavailableException( address );

        emailAddress = new EmailAddress( address );
        db.persist( emailAddress );

        return emailAddress;
    }
}
