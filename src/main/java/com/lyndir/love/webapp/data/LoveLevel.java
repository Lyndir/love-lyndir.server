package com.lyndir.love.webapp.data;

import com.google.common.base.Optional;
import com.lyndir.lhunath.opal.system.util.ConversionUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * @author lhunath, 2013-10-14
 */
public enum LoveLevel {
    FREE( null ),
    LOVED( "com.lyndir.lhunath.MasterPassword.love.loved" ),
    AWESOME( "com.lyndir.lhunath.MasterPassword.love.awesome" );
    private final String productID;

    LoveLevel(final String productID) {
        this.productID = productID;
    }

    @Nullable
    public static LoveLevel ofName(final String level) {
        Optional<Integer> levelOrdinal = ConversionUtils.toInteger( level );
        if (levelOrdinal.isPresent() && levelOrdinal.get() >= 0 && levelOrdinal.get() < values().length)
            return values()[levelOrdinal.get()];

        for (LoveLevel loveLevel : values())
            if (loveLevel.name().equalsIgnoreCase( level ))
                return loveLevel;

        return null;
    }

    @Nullable
    public static LoveLevel ofProductID(final String productID) {
        for (LoveLevel loveLevel : values())
            if (productID.equals( loveLevel.productID ))
                return loveLevel;

        return null;
    }

    @Nonnull
    public static LoveLevel max(@Nonnull final LoveLevel one, @Nonnull final LoveLevel two) {
        return one.ordinal() > two.ordinal()? one: two;
    }
}
