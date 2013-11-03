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
import com.google.gson.annotations.Expose;
import com.lyndir.lhunath.opal.system.i18n.Localized;
import com.lyndir.lhunath.opal.system.i18n.MessagesFactory;
import com.lyndir.lhunath.opal.system.util.MetaObject;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nonnull;
import javax.persistence.*;


/**
 * @author lhunath
 */
@Entity(name = "LLUser")
public class User extends MetaObject implements Localized {

    private static final Messages msgs = MessagesFactory.create( Messages.class );

    @Id
    @GeneratedValue
    private final long               id             = 0;
    @Expose
    @OneToMany(mappedBy = "user")
    private final List<EmailAddress> emailAddresses = Lists.newLinkedList();
    @OneToMany(mappedBy = "user")
    private final List<Receipt>      receipts       = Lists.newLinkedList();
    @Expose
    private final Mode mode;
    @Expose
    private LoveLevel loveLevel = LoveLevel.FREE;
    @Expose
    private int activeSubscriptions;

    @Deprecated
    public User() {
        mode = null;
    }

    public User(@Nonnull final Mode mode, @Nonnull final EmailAddress emailAddress) {
        this.mode = mode;
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
    public Mode getMode() {
        return mode;
    }

    @Nonnull
    public LoveLevel getLoveLevel() {
        return checkNotNull( loveLevel );
    }

    public void setLoveLevel(@Nonnull final LoveLevel loveLevel) {
        this.loveLevel = checkNotNull( loveLevel );
    }

    public int getActiveSubscriptions() {
        return activeSubscriptions;
    }

    public void setActiveSubscriptions(final int activeSubscriptions) {
        this.activeSubscriptions = activeSubscriptions;
    }

    @Override
    public String getLocalizedType() {
        return msgs.type();
    }

    @Override
    public String getLocalizedInstance() {
        return msgs.instance( Iterables.getFirst( getEmailAddresses(), "<no email>" ) );
    }

    @Nonnull
    public List<Receipt> getReceipts() {
        return receipts;
    }

    interface Messages {

        String type();

        String instance(Serializable userName);
    }
}
