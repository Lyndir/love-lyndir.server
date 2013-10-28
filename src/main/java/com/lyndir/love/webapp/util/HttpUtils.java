package com.lyndir.love.webapp.util;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.lyndir.lhunath.opal.system.error.InternalInconsistencyException;
import java.io.IOException;


/**
 * @author lhunath, 2013-10-19
 */
public abstract class HttpUtils {

    private static final HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory( new HttpRequestInitializer() {
        @Override
        public void initialize(HttpRequest request) {
            request.setParser( new JsonObjectParser( GsonFactory.getDefaultInstance() ) );
        }
    } );

    public static HttpRequestFactory getRequestFactory() {
        return requestFactory;
    }

    public static HttpRequest postJSON(final GenericUrl url, final Object data) {
        try {
            return getRequestFactory().buildPostRequest( url, new JsonHttpContent( GsonFactory.getDefaultInstance(), data ) );
        }
        catch (IOException e) {
            throw new InternalInconsistencyException( "Error while configuring the request", e );
        }
    }
}
