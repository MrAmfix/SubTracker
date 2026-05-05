package com.mramfix.subtracker.data.repository

import com.mramfix.subtracker.data.local.SubscriptionDao
import com.mramfix.subtracker.data.local.toDomain
import com.mramfix.subtracker.data.local.toEntity
import com.mramfix.subtracker.domain.model.Subscription
import com.mramfix.subtracker.domain.model.SubscriptionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SubscriptionRepository(private val dao: SubscriptionDao) {
    fun observeAll(): Flow<List<Subscription>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeById(id: Long): Flow<Subscription?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun getAll(): List<Subscription> = withContext(Dispatchers.IO) {
        dao.getAll().map { it.toDomain() }
    }

    suspend fun upsert(subscription: Subscription): Long = withContext(Dispatchers.IO) {
        dao.insert(subscription.toEntity())
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun setActive(id: Long, active: Boolean) = withContext(Dispatchers.IO) {
        val current = dao.getById(id)?.toDomain() ?: return@withContext
        dao.update(
            current.copy(
                status = if (active) SubscriptionStatus.ACTIVE else SubscriptionStatus.INACTIVE,
                updatedAtEpochMillis = System.currentTimeMillis()
            ).toEntity()
        )
    }

    suspend fun replaceAll(subscriptions: List<Subscription>) = withContext(Dispatchers.IO) {
        dao.clear()
        dao.insertAll(subscriptions.map { it.toEntity() })
    }
}
