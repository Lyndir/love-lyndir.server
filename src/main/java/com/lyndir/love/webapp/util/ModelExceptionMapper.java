package com.lyndir.love.webapp.util;

import com.google.inject.Singleton;
import com.lyndir.love.webapp.data.service.ModelException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
 * @author lhunath, 2013-10-22
 */
@Provider
@Singleton
public class ModelExceptionMapper implements ExceptionMapper<ModelException> {

    @Override
    public Response toResponse(final ModelException exception) {
        return Response.serverError().entity( exception.getLocalizedMessage() ).build();
    }
}
