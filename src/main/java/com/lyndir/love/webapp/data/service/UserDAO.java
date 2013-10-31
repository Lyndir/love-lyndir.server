package com.lyndir.love.webapp.data.service;

import com.lyndir.love.webapp.data.*;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * @author lhunath
 */
public interface UserDAO {

    @Nonnull
    User newUser(Mode mode, String emailAddress)
            throws EmailAddressUnavailableException;

    @Nullable
    User findUser(Mode mode, String emailAddress);

    @Nonnull
    User findOrNewUser(Mode mode, String emailAddress);

    @Nonnull
    List<User> listUsers(Mode mode);

    void addReceipt(@Nonnull User user, @Nonnull String application, @Nonnull String appReceiptB64);
}
