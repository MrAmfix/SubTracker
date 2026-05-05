package com.mramfix.subtracker.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "subscriptions")
public class SubscriptionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String iconUri;
    public String description;
    public double cost;
    public String currency;
    public String status;
    public long nextPaymentEpochDay;
    public String billingType;
    public Integer intervalDays;
    public Integer calendarDay;
    public long createdAtEpochMillis;
    public long updatedAtEpochMillis;

    public SubscriptionEntity(
            long id,
            String name,
            String iconUri,
            String description,
            double cost,
            String currency,
            String status,
            long nextPaymentEpochDay,
            String billingType,
            Integer intervalDays,
            Integer calendarDay,
            long createdAtEpochMillis,
            long updatedAtEpochMillis
    ) {
        this.id = id;
        this.name = name;
        this.iconUri = iconUri;
        this.description = description;
        this.cost = cost;
        this.currency = currency;
        this.status = status;
        this.nextPaymentEpochDay = nextPaymentEpochDay;
        this.billingType = billingType;
        this.intervalDays = intervalDays;
        this.calendarDay = calendarDay;
        this.createdAtEpochMillis = createdAtEpochMillis;
        this.updatedAtEpochMillis = updatedAtEpochMillis;
    }
}
