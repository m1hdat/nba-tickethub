package com.example.nbatickethub.model;

import java.util.Currency;

/** Exact minor units (USD cents in mock assets), never a floating-point price. */
public final class Money {
    public final long amountMinor;
    public final String currencyCode;

    public Money(long amountMinor, String currencyCode) {
        if (amountMinor < 0) { throw new IllegalArgumentException("Negative price"); }
        if (currencyCode == null || !currencyCode.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Expected ISO currency code");
        }
        Currency.getInstance(currencyCode);
        this.amountMinor = amountMinor;
        this.currencyCode = currencyCode;
    }
}

