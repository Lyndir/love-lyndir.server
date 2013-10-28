package com.lyndir.love.webapp.resource;

import com.google.gson.annotations.Expose;
import com.google.inject.Inject;
import com.lyndir.lhunath.opal.system.util.URLUtils;
import com.lyndir.love.webapp.data.User;
import com.lyndir.love.webapp.data.service.EmailAddressUnavailableException;
import com.lyndir.love.webapp.data.service.UserDAO;
import com.lyndir.love.webapp.util.ReceiptUtils;
import java.util.Collection;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * @author lhunath, 2013-10-15
 */
@Path("/user")
public class UserResource {

    private final UserDAO userDAO;

    @Inject
    public UserResource(final UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection<User> list() {
        return userDAO.listUsers();
    }

    @GET
    @Path("/{emailAddress}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response get(@PathParam("emailAddress") final String emailAddress, @QueryParam("recheck") final boolean recheck) {
        // Check input.
        User user = userDAO.findUser( emailAddress );
        if (user == null)
            return Response.status( Response.Status.NOT_FOUND ).entity( "No user with `emailAddress`: " + emailAddress ).build();

        // Handle.
        if (recheck)
            ReceiptUtils.recheckReceipts( user );

        // Response.
        return Response.ok( user ).build();
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    public Response post(PostObject input)
            throws EmailAddressUnavailableException {
        // Check input.
        if (input.emailAddress == null)
            return Response.serverError().entity( "Missing `emailAddress` form parameter." ).build();

        // Handle.
        User user = userDAO.newUser( input.emailAddress );
        if (input.receiptB64 != null)
            if (!ReceiptUtils.importReceipt( user, input.receiptB64 ))
                return Response.serverError().build();

        // Response.
        return Response.created( URLUtils.newURI( "%s", user.getEmailAddresses().iterator().next().getAddress() ) ).build();
    }

    @PUT
    @Path("/{emailAddress}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response put(@PathParam("emailAddress") final String emailAddress, PutObject input) {
        // Check input.
        User user = userDAO.findOrNewUser( emailAddress );

        // Handle.
        if (input.receiptB64 != null)
            if (!ReceiptUtils.importReceipt( user, input.receiptB64 ))
                return Response.serverError().build();

        // Response.
        return Response.ok( user ).build();
    }

    public static class PostObject {

        @Expose
        String emailAddress;
        @Expose
        String receiptB64;
    }


    public static class PutObject {

        @Expose
        String receiptB64;
    }
}
