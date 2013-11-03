package com.lyndir.love.webapp.data.service.impl;

import static com.lyndir.lhunath.opal.system.util.ObjectUtils.*;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.JsonString;
import com.google.api.client.util.Key;
import com.google.common.base.*;
import com.google.common.collect.*;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.lyndir.lhunath.opal.system.error.InternalInconsistencyException;
import com.lyndir.lhunath.opal.system.logging.Logger;
import com.lyndir.lhunath.opal.system.util.*;
import com.lyndir.love.webapp.data.*;
import com.lyndir.love.webapp.data.service.ReceiptService;
import com.lyndir.love.webapp.data.service.UserDAO;
import com.lyndir.love.webapp.util.HttpUtils;
import java.io.*;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;


/**
 * @author lhunath, 2013-10-19
 */
public class ReceiptServiceImpl implements ReceiptService {

    private static final Logger logger = Logger.get( ReceiptServiceImpl.class );

    private static final EnumMap<Mode, GenericUrl> verifyReceiptURLByMode = Maps.newEnumMap( Mode.class );

    private static final Splitter.MapSplitter secretSplitter = Splitter.on( CharMatcher.BREAKING_WHITESPACE )
                                                                       .omitEmptyStrings()
                                                                       .trimResults()
                                                                       .withKeyValueSeparator( '=' );
    private static final ImmutableMap<String, String> sharedSecretByApp;

    static {
        logger.dbg( "Configuring receipt service" );
        for (final Mode mode : Mode.values())
            switch (mode) {
                case PRODUCTION:
                    verifyReceiptURLByMode.put( mode, new GenericUrl( "https://buy.itunes.apple.com/verifyReceipt" ) );
                    break;
                case SANDBOX:
                    verifyReceiptURLByMode.put( mode, new GenericUrl( "https://sandbox.itunes.apple.com/verifyReceipt" ) );
                    break;
            }

        try {
            sharedSecretByApp = ImmutableMap.copyOf(
                    secretSplitter.split( Resources.toString( Resources.getResource( "sharedSecrets" ), Charsets.UTF_8 ) ) );
        }
        catch (IOException e) {
            throw new InternalInconsistencyException( "Couldn't load shared secret", e );
        }
    }

    private final UserDAO userDAO;

    @Inject
    public ReceiptServiceImpl(final UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public void recheckReceipts(final User user) {
        updateUserFromReceipts( user );
    }

    @Override
    public boolean importReceipt(final User user, final String application, final String receiptB64) {
        final VerifyReceiptResult result = verifyReceipt( user, application, receiptB64 );
        if (result == null)
            // Failed to parse or verify given receipt.  Won't import.
            return false;

        updateUserFromReceipts( user, result );

        return true;
    }

    private void updateUserFromReceipts(final User user, final VerifyReceiptResult... knownResults) {
        final Date now = new Date();
        final Set<VerifyReceiptResult> knownResultsSet = ImmutableSet.copyOf( knownResults );
        final FluentIterable<Receipt> fromReceipts = FluentIterable.from( user.getReceipts() );
        final FluentIterable<VerifyReceiptResult> fromKnownResults = FluentIterable.from( knownResultsSet );

        Set<LoveLevel> subscriptions = Sets.newHashSet();
        LoveLevel userLoveLevel = LoveLevel.FREE;
        for (final VerifyReceiptResult receipt : fromReceipts.transform( new NFunctionNN<Receipt, VerifyReceiptResult>() {
            @Nullable
            @Override
            public VerifyReceiptResult apply(@Nonnull final Receipt receipt) {
                // Verify each receipt with Apple and get verified info on its purchases.
                return fromKnownResults.firstMatch( new PredicateNN<VerifyReceiptResult>() {
                    @Override
                    public boolean apply(@Nonnull
                                         final VerifyReceiptResult result) {
                        // Check whether we already have a known result for this receipt.
                        return receipt.getApplication().equals( result.receipt.bundleID );
                    }
                } ).or( new Supplier<VerifyReceiptResult>() {
                    @Override
                    public VerifyReceiptResult get() {
                        // Obtain a result for this receipt from Apple.
                        return verifyReceipt( user, receipt.getApplication(), receipt.getReceiptB64() );
                    }
                } );
            }
        } )) {
            // Iterate receipt purchases to determine max user level and amount of subscriptions.
            for (InAppPurchase purchase : ifNotNullElse( ifNotNullElseNullable( receipt.latestExpiredPurchases, receipt.latestPurchases ),
                                                         receipt.receipt.purchases )) {
                LoveLevel purchaseLevel = LoveLevel.ofProductID( purchase.productID );
                if (purchaseLevel == null)
                    // Not a level purchase.
                    continue;

                Long expiresDate = purchase.expiresDate;
                Long purchaseDate = ifNotNullElseNullable( purchase.purchaseDateOriginal, purchase.purchaseDate );
                if (purchaseDate == null || expiresDate == null)
                    // Not an auto-renew subscription.
                    continue;

                Period remainingTime = new Duration( now.getTime(), expiresDate ).toPeriod();
                if (expiresDate < now.getTime() || purchaseDate > now.getTime()) {
                    // Expired or not yet active subscription.
                    logger.trc( "- Expired: %s (elapsed: %s)", purchaseLevel, ISOPeriodFormat.standard().print( remainingTime ) );
                    continue;
                }

                if (purchaseLevel.isSubscription())
                    // This purchase is an active user subscription.
                    subscriptions.add( purchaseLevel );

                logger.dbg( "- Active: %s (remaining: %s)", purchaseLevel, ISOPeriodFormat.standard().print( remainingTime ) );
                userLoveLevel = EnumUtils.max( userLoveLevel, purchaseLevel );
            }
        }

        user.setActiveSubscriptions( subscriptions.size() );
        user.setLoveLevel( userLoveLevel );
        logger.dbg( "-> Updated user: %s (subscriptions: %s, user level: %s)", //
                    user.getEmailAddresses().iterator().next().getAddress(), subscriptions, userLoveLevel );
    }

    private VerifyReceiptResult verifyReceipt(final User user, final String application, final String receiptB64) {
        try {
            VerifyReceiptContent content = new VerifyReceiptContent( receiptB64, sharedSecretByApp.get( application ) );
            HttpResponse response = HttpUtils.postJSON( verifyReceiptURLByMode.get( user.getMode() ), content ).execute();
            if (!response.isSuccessStatusCode()) {
                logger.wrn( "Verify Receipt request unsuccessful: (status %d) %s", response.getStatusCode(), response.getStatusMessage() );
                return null;
            }

            String responseString = response.parseAsString();
            ByteArrayInputStream in = new ByteArrayInputStream( Charsets.UTF_8.encode( responseString ).array() );
            VerifyReceiptResult result = response.getRequest().getParser().parseAndClose( in, Charsets.UTF_8, VerifyReceiptResult.class );
            if (result.statusAsEnum() != VerifyReceiptResult.Status.SUCCESS) {
                logger.wrn( "Verify Receipt response unsuccessful: (status %d) %s", result.status, result.statusAsEnum().name() );
                return null;
            }

            // Write the response to file for auditing.
            File responseLogFile = new File(
                    String.format( "log/responses/%s/%s-%s.json", user.getEmailAddresses().iterator().next().getAddress(),
                                   ISODateTimeFormat.date().print( System.currentTimeMillis() ), application ) );
            if (responseLogFile.getParentFile().isDirectory() || responseLogFile.getParentFile().mkdirs())
                Files.write( responseString, responseLogFile, Charsets.UTF_8 );
            else
                logger.wrn( "Failed to create log path for: %s", responseLogFile );

            // Store latest receipt in user for performing validation when no receipt is available from the app.
            userDAO.addReceipt( user, result.receipt.bundleID, receiptB64 );

            return result;
        }
        catch (IOException e) {
            logger.wrn( e, "Verify Receipt request failed" );
            return null;
        }
    }

    /**
     * https://developer.apple.com/library/ios/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateRemotely.html
     */
    private static class VerifyReceiptContent extends MetaObject {

        @Key("receipt-data")
        final String receiptB64;
        @Key("password")
        final String sharedSecret;

        public VerifyReceiptContent(final String receiptB64, final String sharedSecret) {
            this.receiptB64 = receiptB64;
            this.sharedSecret = sharedSecret;
        }
    }


    /**
     * https://developer.apple.com/library/ios/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateRemotely.html
     */
    public static class VerifyReceiptResult extends MetaObject {

        @Key("status")
        public int                 status;
        @Key("environment")
        public String              environment;
        @Key("receipt")
        public AppReceipt          receipt;
        @Key("latest_receipt")
        public String              latestReceiptB64;
        @Key("latest_receipt_info")
        public List<InAppPurchase> latestPurchases;
        @Key("latest_expired_receipt_info")
        public List<InAppPurchase> latestExpiredPurchases;

        public Status statusAsEnum() {
            for (Status statusEnum : Status.values())
                if (statusEnum.getStatus() == status)
                    return statusEnum;

            throw new UnsupportedOperationException( "Status not supported: " + status );
        }

        public enum Status {
            SUCCESS( 0 ),
            INVALID_REQUEST( 21000 ),
            INVALID_RECEIPT( 21002 ),
            UNAUTHORIZED_RECEIPT( 21003 ),
            UNAUTHORIZED_REQUEST( 21004 ),
            UNAVAILABLE( 21005 ),
            SUBSCRIPTION_EXPIRED( 21006 ),
            RECEIPT_SANDBOX( 21007 ),
            RECEIPT_PRODUCTION( 21008 );
            private final int status;

            Status(int status) {
                this.status = status;
            }

            public int getStatus() {
                return status;
            }
        }
    }


    /**
     * https://developer.apple.com/library/ios/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html
     */
    public static class AppReceipt extends MetaObject {

        @Key("bundle_id")
        public String              bundleID;
        @Key("application_version")
        public String              appVersion;
        @Key("original_application_version")
        public String              appVersionOriginal;
        @Key("in_app")
        public List<InAppPurchase> purchases;
    }


    /**
     * https://developer.apple.com/library/ios/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html
     */
    public static class InAppPurchase extends MetaObject {

        @Key("quantity")
        public int    quantity;
        @Key("product_id")
        public String productID;
        @Key("transaction_id")
        public String transactionID;
        @Key("original_transaction_id")
        public String transactionIDOriginal;
        @JsonString
        @Key("purchase_date_ms")
        public Long   purchaseDate;
        @JsonString
        @Key("original_purchase_date_ms")
        public Long   purchaseDateOriginal;
        @JsonString
        @Key("expires_date_ms")
        public Long   expiresDate;
        @JsonString
        @Key("cancellation_date_ms")
        public Long   cancellationDate;
        @Key("app_item_id")
        public String bundleID;
        @Key("version_external_identifier")
        public String versionID;
        @Key("web_order_line_item_id")
        public String orderItemID;
    }
}
