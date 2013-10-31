package com.lyndir.love.webapp.resource;

import com.google.gson.annotations.Expose;
import com.google.inject.Inject;
import com.lyndir.lhunath.opal.system.util.URLUtils;
import com.lyndir.love.webapp.data.Mode;
import com.lyndir.love.webapp.data.User;
import com.lyndir.love.webapp.data.service.*;
import java.util.Collection;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * @author lhunath, 2013-10-15
 */
@Path("/user")
public class UserResource {

    private final UserDAO        userDAO;
    private final ReceiptService receiptService;

    @Inject
    public UserResource(final UserDAO userDAO, final ReceiptService receiptService) {
        this.userDAO = userDAO;
        this.receiptService = receiptService;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection<User> list(@MatrixParam("mode") @DefaultValue(Mode.DEFAULT) final Mode mode) {
        return userDAO.listUsers( mode );
    }

    @GET
    @Path("/{emailAddress}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response get(@MatrixParam("mode") @DefaultValue(Mode.DEFAULT) final Mode mode,
                        @PathParam("emailAddress") final String emailAddress, @QueryParam("recheck") final boolean recheck) {
        // Check input.
        User user = userDAO.findUser( mode, emailAddress );
        if (user == null)
            return Response.status( Response.Status.NOT_FOUND ).entity( "No user with `emailAddress`: " + emailAddress ).build();

        // Handle.
        if (recheck)
            receiptService.recheckReceipts( user );

        // Response.
        return Response.ok( user ).build();
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    public Response post(@MatrixParam("mode") @DefaultValue(Mode.DEFAULT) final Mode mode, PostObject input)
            throws EmailAddressUnavailableException {
        // Check input.
        if (input.emailAddress == null)
            return Response.serverError().entity( "Missing `emailAddress`." ).build();

        // Handle.
        User user = userDAO.newUser( mode, input.emailAddress );
        if (input.receiptB64 != null) {
            if (input.application == null)
                return Response.serverError().entity( "Missing `application`." ).build();

            if (!receiptService.importReceipt( user, input.application, input.receiptB64 ))
                return Response.serverError().build();
        }

        // Response.
        return Response.created( URLUtils.newURI( "%s", user.getEmailAddresses().iterator().next().getAddress() ) ).build();
    }

    @PUT
    @Path("/{emailAddress}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response put(@MatrixParam("mode") @DefaultValue(Mode.DEFAULT) final Mode mode,
                        @PathParam("emailAddress") final String emailAddress, PutObject input) {
        // Check input.
        User user = userDAO.findOrNewUser( mode, emailAddress );

        // Handle.
        if (input.receiptB64 != null) {
            if (input.application == null)
                return Response.serverError().entity( "Missing `application`." ).build();

            if (!receiptService.importReceipt( user, input.application, input.receiptB64 ))
                return Response.serverError().build();
        }

        // Response.
        return Response.ok( user ).build();
    }

    public static class PostObject {

        @Expose
        String emailAddress;
        @Expose
        String receiptB64;
        @Expose
        String application;
    }


    public static class PutObject {

        @Expose
        String receiptB64;
        @Expose
        String application;
    }
}
