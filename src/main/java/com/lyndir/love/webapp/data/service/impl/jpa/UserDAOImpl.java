package com.lyndir.love.webapp.data.service.impl.jpa;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.lyndir.lhunath.opal.jpa.Persist;
import com.lyndir.lhunath.opal.system.error.AlreadyCheckedException;
import com.lyndir.love.webapp.data.EmailAddress;
import com.lyndir.love.webapp.data.User;
import com.lyndir.love.webapp.data.service.*;
import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;


/**
 * @author lhunath
 */
public class UserDAOImpl implements UserDAO {

    private final EntityManager   db;
    private final CriteriaBuilder cb;
    private final EmailAddressDAO emailAddressDAO;

    @Inject
    public UserDAOImpl(final Persist persist, final EmailAddressDAO emailAddressDAO) {
        this.db = persist.getEntityManager();
        this.cb = db.getCriteriaBuilder();
        this.emailAddressDAO = emailAddressDAO;
    }

    @Override
    public User newUser(final String emailAddress) {
        User user = findUser( emailAddress );
        if (user == null)
            try {
                user = new User( emailAddressDAO.newAddress( emailAddress ) );
                db.persist( user );
            }
            catch (EmailAddressUnavailableException e) {
                throw new AlreadyCheckedException( "Should have found existing user.", e );
            }

        return user;
    }

    @Override
    @Nullable
    public User findUser(final String emailAddress) {
        return Iterables.getFirst( db.createQuery( "SELECT u FROM EmailAddress e JOIN e.user u WHERE e.address = ?", User.class )
                                     .setParameter( 1, emailAddress )
                                     .getResultList(), null );
    }

    @Override
    public List<User> listUsers() {
        return db.createQuery( "SELECT u FROM User u", User.class ).getResultList();
    }
}
