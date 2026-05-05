package com.mramfix.subtracker.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "exchange_rates")
public class ExchangeRateEntity {
    @PrimaryKey
    @NonNull
    public String currency;
    public double rubPerUnit;
    public int nominal;
    public long fetchedAtEpochMillis;

    public ExchangeRateEntity(String currency, double rubPerUnit, int nominal, long fetchedAtEpochMillis) {
        this.currency = currency;
        this.rubPerUnit = rubPerUnit;
        this.nominal = nominal;
        this.fetchedAtEpochMillis = fetchedAtEpochMillis;
    }
}
