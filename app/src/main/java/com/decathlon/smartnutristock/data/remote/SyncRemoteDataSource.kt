package com.decathlon.smartnutristock.data.remote

/**
 * Abstraction for remote sync operations against the active_stocks table.
 *
 * Decouples SyncRepositoryImpl from Supabase Postgrest internals,
 * enabling unit testability without mocking inline extension functions.
 *
 * Following Dependency Inversion Principle: the repository depends on
 * this abstraction, not on the concrete Supabase SDK.
 */
interface SyncRemoteDataSource {

    /**
     * Upsert active stock records to the remote store.
     *
     * @param records List of [SupabaseActiveStock] rows to upsert
     */
    suspend fun upsertActiveStocks(records: List<SupabaseActiveStock>)

    /**
     * Fetch active stock records from the remote store that were synced
     * after the given timestamp.
     *
     * @param storeId Store identifier to filter by
     * @param lastSyncedAt ISO-8601 timestamp; only records synced after this are returned
     * @return List of [SupabaseActiveStock] rows from the remote store
     */
    suspend fun fetchActiveStocks(
        storeId: String,
        lastSyncedAt: String
    ): List<SupabaseActiveStock>
}
