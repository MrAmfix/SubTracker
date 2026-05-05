package com.mramfix.subtracker.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {SubscriptionEntity.class, ExchangeRateEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SubscriptionDao subscriptionDao();
    public abstract ExchangeRateDao exchangeRateDao();

    private static volatile AppDatabase instance;

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "subtracker.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
