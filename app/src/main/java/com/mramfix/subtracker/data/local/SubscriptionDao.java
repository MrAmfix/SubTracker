package com.mramfix.subtracker.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.annotation.Nullable;
import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions")
    Flow<List<SubscriptionEntity>> observeAll();

    @Query("SELECT * FROM subscriptions")
    List<SubscriptionEntity> getAll();

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    Flow<SubscriptionEntity> observeById(long id);

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    @Nullable
    SubscriptionEntity getById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SubscriptionEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SubscriptionEntity> entities);

    @Update
    void update(SubscriptionEntity entity);

    @Delete
    void delete(SubscriptionEntity entity);

    @Query("DELETE FROM subscriptions WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM subscriptions")
    void clear();
}
