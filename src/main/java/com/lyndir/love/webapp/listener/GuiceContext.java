/*
 *   Copyright 2010, Maarten Billemont
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.lyndir.love.webapp.listener;

import com.google.inject.*;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.lyndir.lhunath.opal.jpa.PersistFilter;
import com.lyndir.lhunath.opal.system.logging.Logger;
import com.lyndir.love.webapp.resource.UserResource;
import com.lyndir.love.webapp.data.service.DAOModule;
import com.lyndir.love.webapp.util.GsonJsonProvider;
import com.lyndir.love.webapp.util.ModelExceptionMapper;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;


/**
 * @author lhunath
 */
public class GuiceContext extends GuiceServletContextListener {

    static final Logger logger = Logger.get( GuiceContext.class );

    private static final String PATH_APP = "/app/*";
    private static final String PATH_APP_REST = "/app/rest/*";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Injector getInjector() {
        return Guice.createInjector( Stage.DEVELOPMENT, new DAOModule(), new ServletModule() {
            @Override
            protected void configureServlets() {
                filter( PATH_APP ).through( PersistFilter.class );
                bind( PersistFilter.class ).in( Scopes.SINGLETON );

                bind( UserResource.class );
                bind( GsonJsonProvider.class );
                bind( ModelExceptionMapper.class );
                serve( PATH_APP_REST ).with( GuiceContainer.class );
            }
        } );
    }
}
