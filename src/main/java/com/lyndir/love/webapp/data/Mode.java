package com.lyndir.love.webapp.data;

import com.lyndir.lhunath.opal.system.logging.Logger;
import javax.annotation.Nonnull;


/**
 * @author lhunath, 10/28/2013
 */
public enum Mode {
    PRODUCTION( null ),
    SANDBOX( "Sandbox" );

    public static final String DEFAULT = "PRODUCTION";

    private static final Logger logger = Logger.get( Mode.class );
    private final String environment;

    Mode(final String environment) {
        this.environment = environment;
    }

    @Nonnull
    public static Mode ofEnvironment(final String environment) {
        if (environment != null)
            for (Mode mode : values())
                if (environment.equalsIgnoreCase( mode.environment ))
                    return mode;

        if (environment != null)
            logger.wrn( "Assuming environment %s means PRODUCTION.", environment );

        return PRODUCTION;
    }
}
