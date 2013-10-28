package com.lyndir.love.webapp.data.service;

import com.lyndir.love.webapp.data.User;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * @author lhunath
 */
public interface UserDAO {

    @Nonnull
    User newUser(String emailAddress)
            throws EmailAddressUnavailableException;

    @Nullable
    User findUser(String emailAddress);

    @Nonnull
    User findOrNewUser(String emailAddress);

    @Nonnull
    List<User> listUsers();
}
