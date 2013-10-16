package com.lyndir.love.webapp;

import static com.lyndir.lhunath.opal.system.util.ObjectUtils.ifNotNullElse;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.lyndir.love.webapp.data.User;
import com.lyndir.love.webapp.data.service.UserDAO;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;


/**
 * @author lhunath, 2013-10-14
 */
public class UserServlet extends HttpServlet {

    public static final String PATH = "/user";
    private final Provider<UserDAO> userDAOProvider;

    @Inject
    public UserServlet(final Provider<UserDAO> userDAOProvider) {
        this.userDAOProvider = userDAOProvider;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        UserDAO userDAO = userDAOProvider.get();

        String emailAddress = req.getParameter( "emailAddress" );
        resp.getWriter().write( String.format( "Found user: %d", ifNotNullElse( User.class, userDAO.findUser( emailAddress ), 0 ).getId() ) );
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        UserDAO userDAO = userDAOProvider.get();

        String emailAddress = req.getParameter( "emailAddress" );
        resp.getWriter().write( String.format( "New user: %d", userDAO.newUser( emailAddress ) ) );
    }
}
