package com.lyndir.love.webapp.data.service;

import com.lyndir.love.webapp.data.EmailAddress;
import com.lyndir.love.webapp.data.User;
import java.util.List;
import javax.annotation.Nullable;


/**
 * @author lhunath
 */
public interface UserDAO {

    User newUser(String emailAddress);

    @Nullable
    User findUser(String emailAddress);

    List<User> listUsers();
}
