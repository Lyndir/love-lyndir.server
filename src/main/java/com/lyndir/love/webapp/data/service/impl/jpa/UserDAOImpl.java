package com.lyndir.love.webapp.data.service.impl.jpa;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.lyndir.lhunath.opal.jpa.Persist;
import com.lyndir.lhunath.opal.system.error.AlreadyCheckedException;
import com.lyndir.lhunath.opal.system.logging.Logger;
import com.lyndir.love.webapp.data.*;
import com.lyndir.love.webapp.data.service.*;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;


/**
 * @author lhunath
 */
public class UserDAOImpl implements UserDAO {

    private static final Logger logger = Logger.get( UserDAOImpl.class );

    private final EntityManager   db;
    private final EmailAddressDAO emailAddressDAO;

    @Inject
    public UserDAOImpl(final Persist persist, final EmailAddressDAO emailAddressDAO) {
        this.db = persist.getEntityManager();
        this.emailAddressDAO = emailAddressDAO;
    }

    @Nonnull
    @Override
    public User newUser(final Mode mode, final String emailAddress)
            throws EmailAddressUnavailableException {
        User user = new User( mode, emailAddressDAO.newAddress( emailAddress ) );
        db.persist( user );

        return user;
    }

    @Nonnull
    @Override
    public User findOrNewUser(final Mode mode, final String emailAddress) {
        User user = findUser( mode, emailAddress );
        if (user == null)
            try {
                user = newUser( mode, emailAddress );
                logger.dbg( "Created new user for: %s", emailAddress );
            }
            catch (EmailAddressUnavailableException e) {
                throw new AlreadyCheckedException( "Should have found existing user.", e );
            }

        return user;
    }

    @Override
    @Nullable
    public User findUser(final Mode mode, final String emailAddress) {
        return Iterables.getFirst( db.createQuery( "SELECT u FROM EmailAddress e JOIN e.user u " + //
                                                   "WHERE u.mode = :mode AND e.address = :emailAddress", User.class )
                                     .setParameter( "mode", mode )
                                     .setParameter( "emailAddress", emailAddress )
                                     .getResultList(), null );
    }

    @Nonnull
    @Override
    public List<User> listUsers(final Mode mode) {
        return db.createQuery( "SELECT u FROM User u " + //
                               "WHERE u.mode = :mode", User.class ).setParameter( "mode", mode ).getResultList();
    }

    @Override
    public void addReceipt(@Nonnull final User user, @Nonnull final String application, @Nonnull final String appReceiptB64) {
        List<Receipt> userReceipts = user.getReceipts();
        for (final Receipt receipt : userReceipts)
            if (receipt.getApplication().equals( application )) {
                receipt.setReceiptB64( appReceiptB64 );
                return;
            }

        Receipt receipt = new Receipt( user, application, appReceiptB64 );
        db.persist( receipt );
        userReceipts.add( receipt );
    }
}
