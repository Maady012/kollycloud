package com.lagradost.cloudstream3.ui.kollygame

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class KollyBackupWorker(
    val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("KollyBackupWorker", "Starting scheduled auto-backup")
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val autoBackup = sp.getBoolean("backup_auto_daily", true)
            if (!autoBackup) {
                Log.d("KollyBackupWorker", "Auto-backup disabled by user settings")
                return@withContext Result.success()
            }

            val folderUriStr = sp.getString("backup_google_drive_folder", null)
            if (folderUriStr.isNullOrBlank()) {
                Log.w("KollyBackupWorker", "No backup folder linked")
                return@withContext Result.failure()
            }

            val folderUri = Uri.parse(folderUriStr)
            val treeFile = DocumentFile.fromTreeUri(context, folderUri)
            if (treeFile == null || !treeFile.exists() || !treeFile.isDirectory) {
                Log.e("KollyBackupWorker", "Invalid tree URI or folder does not exist")
                return@withContext Result.failure()
            }

            // Gather local KollyCloud data
            val securePrefs = context.getSharedPreferences("kolly_gaming_secure_prefs", Context.MODE_PRIVATE)
            val watchlistJson = securePrefs.getString("cache_movies_watchlist", "[]") ?: "[]"
            val watchedListJson = securePrefs.getString("cache_movies_watched_list", "[]") ?: "[]"
            val watchedIdsJson = securePrefs.getStringSet("watched_movies_ids_v2", emptySet())?.toList()?.joinToString(",") ?: ""

            val backupObj = JSONObject().apply {
                put("watchlist", watchlistJson)
                put("watched_list", watchedListJson)
                put("watched_ids", watchedIdsJson)
                put("timestamp", System.currentTimeMillis())
            }

            // Create or replace the backup file
            val fileName = "kollycloud_backup.json"
            var file = treeFile.findFile(fileName)
            if (file == null) {
                file = treeFile.createFile("application/json", fileName)
            }

            if (file == null) {
                Log.e("KollyBackupWorker", "Could not create backup file")
                return@withContext Result.failure()
            }

            context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                outputStream.write(backupObj.toString().toByteArray())
            }

            Log.d("KollyBackupWorker", "Auto-backup successfully completed: ${file.uri}")
            Result.success()
        } catch (e: Exception) {
            Log.e("KollyBackupWorker", "Error during scheduled auto-backup", e)
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            try {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                val autoBackup = sp.getBoolean("backup_auto_daily", true)
                val folderUriStr = sp.getString("backup_google_drive_folder", null)
                
                if (autoBackup && !folderUriStr.isNullOrBlank()) {
                    val constraints = androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                    val request = androidx.work.PeriodicWorkRequestBuilder<KollyBackupWorker>(
                        24, java.util.concurrent.TimeUnit.HOURS
                    )
                        .setConstraints(constraints)
                        .build()
                    
                    androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        "KollyCloudDailyBackup",
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        request
                    )
                    Log.d("KollyBackupWorker", "Enqueued daily periodic backup work")
                } else {
                    androidx.work.WorkManager.getInstance(context).cancelUniqueWork("KollyCloudDailyBackup")
                    Log.d("KollyBackupWorker", "Cancelled daily periodic backup work (disabled or no folder)")
                }
            } catch (e: Exception) {
                Log.e("KollyBackupWorker", "Failed to schedule periodic backup work", e)
            }
        }

        fun runBackupImmediately(context: Context): Boolean {
            return try {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                val folderUriStr = sp.getString("backup_google_drive_folder", null)
                if (folderUriStr.isNullOrBlank()) return false

                val folderUri = Uri.parse(folderUriStr)
                val treeFile = DocumentFile.fromTreeUri(context, folderUri)
                if (treeFile == null || !treeFile.exists() || !treeFile.isDirectory) return false

                val securePrefs = context.getSharedPreferences("kolly_gaming_secure_prefs", Context.MODE_PRIVATE)
                val watchlistJson = securePrefs.getString("cache_movies_watchlist", "[]") ?: "[]"
                val watchedListJson = securePrefs.getString("cache_movies_watched_list", "[]") ?: "[]"
                val watchedIdsJson = securePrefs.getStringSet("watched_movies_ids_v2", emptySet())?.toList()?.joinToString(",") ?: ""

                val backupObj = JSONObject().apply {
                    put("watchlist", watchlistJson)
                    put("watched_list", watchedListJson)
                    put("watched_ids", watchedIdsJson)
                    put("timestamp", System.currentTimeMillis())
                }

                val fileName = "kollycloud_backup.json"
                var file = treeFile.findFile(fileName)
                if (file == null) {
                    file = treeFile.createFile("application/json", fileName)
                }
                if (file == null) return false

                context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                    outputStream.write(backupObj.toString().toByteArray())
                }
                true
            } catch (e: Exception) {
                Log.e("KollyBackupWorker", "Failed to run immediate backup", e)
                false
            }
        }

        fun runRestore(context: Context, backupUri: Uri): Boolean {
            return try {
                val content = context.contentResolver.openInputStream(backupUri)?.bufferedReader()?.use { it.readText() }
                if (content.isNullOrBlank()) return false

                val backupObj = JSONObject(content)
                val watchlistJson = backupObj.optString("watchlist", "[]")
                val watchedListJson = backupObj.optString("watched_list", "[]")
                val watchedIdsStr = backupObj.optString("watched_ids", "")

                val securePrefs = context.getSharedPreferences("kolly_gaming_secure_prefs", Context.MODE_PRIVATE)
                val editor = securePrefs.edit()

                editor.putString("cache_movies_watchlist", watchlistJson)
                editor.putString("cache_movies_watched_list", watchedListJson)

                if (watchedIdsStr.isNotEmpty()) {
                    val idsSet = watchedIdsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    editor.putStringSet("watched_movies_ids_v2", idsSet)
                } else {
                    editor.putStringSet("watched_movies_ids_v2", emptySet())
                }

                editor.apply()
                true
            } catch (e: Exception) {
                Log.e("KollyBackupWorker", "Failed to run restore", e)
                false
            }
        }
    }
}
