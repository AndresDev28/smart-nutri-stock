package com.decathlon.smartnutristock.data.remote

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase Postgrest implementation of [SyncRemoteDataSource].
 *
 * This class owns all direct Supabase SDK interactions (upsert, select),
 * keeping them out of the repository so the repository can be unit-tested
 * without mocking Supabase's inline extension functions.
 */
@Singleton
class SupabaseSyncRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) : SyncRemoteDataSource {

    companion object {
        private const val TABLE = "active_stocks"
    }

    override suspend fun upsertActiveStocks(records: List<SupabaseActiveStock>) {
        postgrest[TABLE].upsert(records)
    }

    override suspend fun fetchActiveStocks(
        storeId: String,
        lastSyncedAt: String
    ): List<SupabaseActiveStock> {
        val response = postgrest[TABLE]
            .select {
                filter {
                    eq("store_id", storeId)
                    gt("synced_at", lastSyncedAt)
                }
            }
        return response.decodeList<SupabaseActiveStock>()
    }
}
