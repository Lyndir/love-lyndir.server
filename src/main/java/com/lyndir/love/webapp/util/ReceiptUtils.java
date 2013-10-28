package com.lyndir.love.webapp.util;

import static com.lyndir.lhunath.opal.system.util.ObjectUtils.*;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.JsonString;
import com.google.api.client.util.Key;
import com.google.common.base.*;
import com.google.common.collect.FluentIterable;
import com.google.common.io.Resources;
import com.lyndir.lhunath.opal.system.error.InternalInconsistencyException;
import com.lyndir.lhunath.opal.system.logging.Logger;
import com.lyndir.lhunath.opal.system.util.*;
import com.lyndir.love.webapp.data.*;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * @author lhunath, 2013-10-19
 */
public abstract class ReceiptUtils {

    private static final Logger     logger           = Logger.get( ReceiptUtils.class );
    private static final GenericUrl verifyReceiptURL = new GenericUrl( "https://sandbox.itunes.apple.com/verifyReceipt" );
    //private static final GenericUrl verifyReceiptURL = new GenericUrl( "https://buy.itunes.apple.com/verifyReceipt" );
    private static final String sharedSecret;

    static {
        try {
            sharedSecret = Resources.toString( Resources.getResource( "sharedSecret" ), Charsets.UTF_8 ).trim();
        }
        catch (IOException e) {
            throw new InternalInconsistencyException( "Couldn't load shared secret", e );
        }
    }

    /**
     * Recheck all the user's known receipts, updating his love level in the process.
     */
    public static void recheckReceipts(final User user) {
        LoveLevel userLoveLevel = LoveLevel.FREE;
        for (final LoveLevel receiptLoveLevel : FluentIterable.from( user.getReceipts() )
                                                              .transform( new NFunctionNN<Receipt, VerifyReceiptResult>() {
                                                                  @Nullable
                                                                  @Override
                                                                  public VerifyReceiptResult apply(@Nonnull final Receipt input) {
                                                                      return verifyReceipt( user, input.getReceiptB64() );
                                                                  }
                                                              } )
                                                              .filter( Predicates.notNull() )
                                                              .transform( new Function<VerifyReceiptResult, LoveLevel>() {
                                                                  @Nullable
                                                                  @Override
                                                                  public LoveLevel apply(@Nullable final VerifyReceiptResult input) {
                                                                      return levelForReceipt( input );
                                                                  }
                                                              } ))
            userLoveLevel = EnumUtils.max( userLoveLevel, receiptLoveLevel );
        user.setLoveLevel( userLoveLevel );
    }

    /**
     * Import a new receipt for the user, updating his love level in the process.
     *
     * @param receiptB64 RFC 4648 encoded iOS app/transaction receipt data.
     *
     * @return true if the receipt could be verified.  false if it was not parsable or accepted by Apple.
     */
    public static boolean importReceipt(final User user, final String receiptB64) {
        final VerifyReceiptResult result = verifyReceipt( user, receiptB64 );
        if (result == null)
            // Failed to parse or verify given receipt.  Won't import.
            return false;

        LoveLevel userLoveLevel = LoveLevel.FREE;
        for (final LoveLevel receiptLoveLevel : FluentIterable.from( user.getReceipts() )
                                                              .transform( new NFunctionNN<Receipt, VerifyReceiptResult>() {
                                                                  @Nullable
                                                                  @Override
                                                                  public VerifyReceiptResult apply(@Nonnull final Receipt input) {
                                                                      if (input.getApplication().equals( result.receipt.bundleID )) {
                                                                          return result;
                                                                      }
                                                                      return verifyReceipt( user, input.getReceiptB64() );
                                                                  }
                                                              } )
                                                              .filter( Predicates.notNull() )
                                                              .transform( new Function<VerifyReceiptResult, LoveLevel>() {
                                                                  @Nullable
                                                                  @Override
                                                                  public LoveLevel apply(@Nullable final VerifyReceiptResult input) {
                                                                      return levelForReceipt( input );
                                                                  }
                                                              } ))
            userLoveLevel = EnumUtils.max( userLoveLevel, receiptLoveLevel );
        user.setLoveLevel( userLoveLevel );

        return true;
    }

    private static VerifyReceiptResult verifyReceipt(final User user, final String receiptB64) {
        try {
            HttpResponse response = HttpUtils.postJSON( verifyReceiptURL, new VerifyReceiptContent( receiptB64, sharedSecret ) ).execute();
            if (!response.isSuccessStatusCode()) {
                logger.wrn( "Verify Receipt request unsuccessful: (status %d) %s", response.getStatusCode(), response.getStatusMessage() );
                return null;
            }

            VerifyReceiptResult result = response.parseAs( VerifyReceiptResult.class );
            if (result.statusAsEnum() != VerifyReceiptResult.Status.SUCCESS) {
                logger.wrn( "Verify Receipt response unsuccessful: (status %d) %s", result.status, result.statusAsEnum().name() );
                return null;
            }

            logger.inf( "Receipt valid:\n%s", ObjectUtils.toString( result ) );

            // Store latest receipt in user for performing validation when no receipt is available from the app.
            user.addReceipt( result.receipt.bundleID, ifNotNullElse( result.latestReceiptB64, receiptB64 ) );

            return result;
        }
        catch (IOException e) {
            logger.wrn( e, "Verify Receipt request failed" );
            return null;
        }
    }

    private static LoveLevel levelForReceipt(final VerifyReceiptResult result) {
        LoveLevel receiptLoveLevel = LoveLevel.FREE;
        List<InAppPurchase> purchases = ifNotNullElse( ifNotNullElseNullable( result.latestExpiredPurchases, result.latestPurchases ),
                                                       result.receipt.purchases );

        Date now = new Date();
        for (InAppPurchase purchase : purchases) {
            LoveLevel purchaseLevel = LoveLevel.ofProductID( purchase.productID );
            if (purchaseLevel == null)
                // Not a level purchase.
                continue;
            Long expiresDate = purchase.expiresDate;
            Long purchaseDate = ifNotNullElseNullable( purchase.purchaseDate, purchase.purchaseDateOriginal );
            if (purchaseDate == null || expiresDate == null)
                // Not an auto-renew subscription.
                continue;
            if (expiresDate < now.getTime() || purchaseDate > now.getTime())
                // Expired or not yet active subscription.
                continue;

            logger.dbg( "Receipt purchase for level: %s", purchaseLevel );
            receiptLoveLevel = EnumUtils.max( receiptLoveLevel, purchaseLevel );
        }

        logger.dbg( "Receipt grants level: %s", receiptLoveLevel );
        return receiptLoveLevel;
    }

    /**
     * https://developer.apple.com/library/ios/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateRemotely.html
     */
    private static class VerifyReceiptContent {

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
    public static class VerifyReceiptResult {

        @Key("status")
        public int                 status;
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
                if (statusEnum.status == status)
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
    public static class AppReceipt {

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
    public static class InAppPurchase {

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
