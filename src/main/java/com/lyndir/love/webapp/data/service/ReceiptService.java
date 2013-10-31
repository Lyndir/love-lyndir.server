package com.lyndir.love.webapp.data.service;

import com.lyndir.love.webapp.data.Mode;
import com.lyndir.love.webapp.data.User;


/**
 * @author lhunath, 10/28/2013
 */
public interface ReceiptService {

    /**
     * Recheck all the user's known receipts, updating his love level in the process.
     */
    void recheckReceipts(User user);

    /**
     * Import a new receipt for the user, updating his love level in the process.
     *
     * @param application The name of the application for which we're importing a receipt.
     * @param receiptB64  RFC 4648 encoded iOS app/transaction receipt data.
     *
     * @return true if the receipt could be verified.  false if it was not parsable or accepted by Apple.
     */
    boolean importReceipt(User user, String application, String receiptB64);
}
