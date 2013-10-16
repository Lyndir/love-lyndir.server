package com.lyndir.love.webapp.data;

import com.google.common.base.Optional;
import com.lyndir.lhunath.opal.system.util.ConversionUtils;
import javax.annotation.Nullable;


/**
 * @author lhunath, 2013-10-14
 */
public enum LoveLevel {
    UNLOVED,
    LOVED,
    AWESOME;

    @Nullable
    public static LoveLevel of(final String level) {
        Optional<Integer> levelOrdinal = ConversionUtils.toInteger( level );
        if (levelOrdinal.isPresent() && levelOrdinal.get() >= 0 && levelOrdinal.get() < values().length)
            return values()[levelOrdinal.get()];

        for (LoveLevel loveLevel : values())
            if (loveLevel.name().equalsIgnoreCase( level ))
                return loveLevel;

        return null;
    }
}
