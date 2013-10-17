/*
 *   Copyright 2009, Maarten Billemont
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
package com.lyndir.love.webapp.data;

import static com.google.common.base.Preconditions.*;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.lyndir.lhunath.opal.system.i18n.Localized;
import com.lyndir.lhunath.opal.system.i18n.MessagesFactory;
import com.lyndir.lhunath.opal.system.util.MetaObject;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.persistence.*;


/**
 * @author lhunath
 */
@Entity
public class User extends MetaObject implements Localized {

    private static final Random   random = new SecureRandom();
    private static final Messages msgs   = MessagesFactory.create( Messages.class );

    @Id
    private final long               id             = random.nextLong();
    @OneToMany(mappedBy = "user")
    private final List<EmailAddress> emailAddresses = Lists.newLinkedList();
    private       LoveLevel          loveLevel      = LoveLevel.UNLOVED;

    @Deprecated
    public User() {
    }

    public User(@Nonnull final EmailAddress emailAddress) {
        emailAddress.setUser( this );
    }

    public long getId() {
        return id;
    }

    @Nonnull
    public List<EmailAddress> getEmailAddresses() {
        return emailAddresses;
    }

    @Nonnull
    public LoveLevel getLoveLevel() {
        return checkNotNull( loveLevel );
    }

    public void setLoveLevel(@Nonnull final LoveLevel loveLevel) {
        this.loveLevel = checkNotNull( loveLevel );
    }

    @Override
    public String getLocalizedType() {

        return msgs.type();
    }

    @Override
    public String getLocalizedInstance() {

        return msgs.instance( Iterables.getFirst( getEmailAddresses(), "<no email>" ) );
    }

    interface Messages {

        String type();

        String instance(Serializable userName);
    }
}