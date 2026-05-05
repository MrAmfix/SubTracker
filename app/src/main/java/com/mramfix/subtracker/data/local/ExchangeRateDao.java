package com.mramfix.subtracker.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates")
    Flow<List<ExchangeRateEntity>> observeAll();

    @Query("SELECT * FROM exchange_rates")
    List<ExchangeRateEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<ExchangeRateEntity> rates);
}
