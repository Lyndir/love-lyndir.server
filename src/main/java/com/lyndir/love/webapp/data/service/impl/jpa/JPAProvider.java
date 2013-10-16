package com.lyndir.love.webapp.data.service.impl.jpa;

import com.google.inject.Provider;
import com.lyndir.lhunath.opal.jpa.Persist;


/**
 * @author lhunath
 */
public class JPAProvider implements Provider<Persist> {

    @Override
    public Persist get() {
        return Persist.persistence();
    }
}
