package com.lyndir.love.webapp.data;

import com.google.common.base.Optional;
import com.lyndir.lhunath.opal.system.util.ConversionUtils;
import javax.annotation.Nullable;


/**
 * @author lhunath, 2013-10-14
 */
public enum LoveLevel {
    FREE( null ),
    LIKED( ".love.liked" ),
    LOVED( ".love.loved" );
    private final String productIDSuffix;

    LoveLevel(final String productIDSuffix) {
        this.productIDSuffix = productIDSuffix;
    }

    public boolean isSubscription() {
        return this.productIDSuffix != null;
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
        if (productID != null && !productID.isEmpty())
            for (LoveLevel loveLevel : values())
                if (loveLevel.productIDSuffix != null && productID.endsWith( loveLevel.productIDSuffix ))
                    return loveLevel;

        return null;
    }
}
