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

import com.lyndir.lhunath.opal.system.util.MetaObject;
import javax.annotation.Nonnull;
import javax.persistence.*;


/**
 * @author lhunath
 */
@Entity
public class Receipt extends MetaObject {

    @Id
    private       long   id;
    @ManyToOne
    private final User   user;
    private final String application;
    @Column(length = 512/* kB */ * 1024)
    private       String receiptB64;

    @Deprecated
    public Receipt() {
        user = null;
        application = null;
    }

    public Receipt(@Nonnull final User user, @Nonnull final String application, @Nonnull final String receiptB64) {
        this.user = user;
        this.application = application;
        this.receiptB64 = receiptB64;
    }

    public long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getApplication() {
        return application;
    }

    public String getReceiptB64() {
        return receiptB64;
    }

    public void setReceiptB64(final String receiptB64) {
        this.receiptB64 = receiptB64;
    }
}
