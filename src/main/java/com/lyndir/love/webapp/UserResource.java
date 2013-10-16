package com.lyndir.love.webapp;

import com.google.inject.Inject;
import com.lyndir.love.webapp.data.LoveLevel;
import com.lyndir.love.webapp.data.User;
import com.lyndir.love.webapp.data.service.UserDAO;
import java.net.URI;
import javax.ws.rs.*;
import javax.ws.rs.core.*;


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
    @Produces(MediaType.TEXT_PLAIN)
    public String list() {
        StringBuilder result = new StringBuilder();
        for (User user : userDAO.listUsers())
            result.append( user.getId() ).append( '\n' );

        return result.toString();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{emailAddress}")
    public Response get(@PathParam("emailAddress") final String emailAddress) {
        // Check input.
        User user = userDAO.findUser( emailAddress );
        if (user == null)
            return Response.status( Response.Status.NOT_FOUND ).entity( "No user with `emailAddress`: " + emailAddress ).build();

        // Response.
        return Response.ok( String.valueOf( user.getId() ) ).build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{emailAddress}/level")
    public Response level(@PathParam("emailAddress") final String emailAddress) {
        // Check input.
        User user = userDAO.findUser( emailAddress );
        if (user == null)
            return Response.status( Response.Status.NOT_FOUND ).entity( "No user with `emailAddress`: " + emailAddress ).build();

        // Response.
        return Response.ok( user.getLoveLevel().name() ).build();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{emailAddress}")
    public Response create(@PathParam("emailAddress") final String emailAddress, @FormParam("level") final String level) {
        // Check input.
        if (level == null)
            return Response.serverError().entity( "Missing `level` form parameter." ).build();

        LoveLevel loveLevel = LoveLevel.of( level );
        if (loveLevel == null)
            return Response.serverError().entity( "Unsupported `level` form parameter: " + level ).build();

        // Handle.
        User user = userDAO.newUser( emailAddress );
        user.setLoveLevel( loveLevel );

        // Response.
        return Response.created( URI.create( "/" ) ).build();
    }

    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{emailAddress}")
    public Response update(@PathParam("emailAddress") final String emailAddress, @FormParam("level") final String level) {
        // Check input.
        User user = userDAO.findUser( emailAddress );
        if (user == null)
            return Response.status( Response.Status.NOT_FOUND ).entity( "No user with `emailAddress`: " + emailAddress ).build();
        LoveLevel loveLevel = null;
        if (level != null) {
            loveLevel = LoveLevel.of( level );
            if (loveLevel == null)
                return Response.serverError().entity( "Unsupported `level` form parameter: " + level ).build();
        }

        // Handle.
        if (loveLevel != null)
            user.setLoveLevel( loveLevel );

        // Response.
        return Response.ok( String.valueOf( user.getId() ) ).build();
    }
}
