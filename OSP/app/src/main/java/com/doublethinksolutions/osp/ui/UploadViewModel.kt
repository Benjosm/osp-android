package com.doublethinksolutions.osp.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doublethinksolutions.osp.data.PhotoMetadata
import com.doublethinksolutions.osp.tasks.TrustScoreCalculationTask
import com.doublethinksolutions.osp.upload.UploadManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class UploadViewModel : ViewModel() {

    // Linger duration for a completed item in the main queue before being archived.
    private val LINGER_DURATION_MS = 4000L
    // Maximum number of items to keep in the history.
    private val MAX_HISTORY_SIZE = 5

    // This queue holds active uploads and recently completed ones that are "lingering".
    private val _uploadQueue = MutableStateFlow<List<UploadItem>>(emptyList())
    val uploadQueue: StateFlow<List<UploadItem>> = _uploadQueue.asStateFlow()

    // This holds the archived history of completed uploads.
    private val _uploadHistory = MutableStateFlow<List<UploadItem>>(emptyList())
    val uploadHistory: StateFlow<List<UploadItem>> = _uploadHistory.asStateFlow()

    fun startUpload(context: Context, photoFile: File, metadata: PhotoMetadata?) {
        val newItem = UploadItem(fileName = photoFile.name)
        // Add to the front of the queue
        _uploadQueue.update { listOf(newItem) + it }

        viewModelScope.launch {
            UploadManager.upload(
                context = context,
                file = photoFile,
                metadata = metadata,
                onProgress = { progress ->
                    updateUploadItem(newItem.id) { it.copy(status = UploadStatus.UPLOADING, progress = progress) }
                },
                onSuccess = { uploadDurationMs ->
                    Log.d("ViewModel", "Upload success for ${newItem.fileName}")
                    calculateTrustScore(context, photoFile, newItem.id, uploadDurationMs)
                },
                onFailure = { exception ->
                    Log.e("ViewModel", "Upload failed for ${newItem.fileName}", exception)
                    updateUploadItem(newItem.id) {
                        it.copy(status = UploadStatus.FAILED, progress = 1f, errorMessage = exception.message ?: "Unknown error")
                    }
                    // Schedule item to be moved to history after a delay
                    scheduleForArchival(newItem.id)
                }
            )
        }
    }

    private fun calculateTrustScore(context: Context, photoFile: File, itemId: String, uploadDurationMs: Long) {
        TrustScoreCalculationTask(
            context = context,
            onResult = { trustScore ->
                val result = UploadResult(
                    trustScore = trustScore,
                    uploadTimeMs = uploadDurationMs,
                    fileSizeBytes = photoFile.length()
                )
                updateUploadItem(itemId) {
                    it.copy(status = UploadStatus.SUCCESS, progress = 1f, result = result)
                }
                scheduleForArchival(itemId)
            },
            onError = { exception ->
                Log.e("ViewModel", "Trust score calculation failed", exception)
                val result = UploadResult(
                    trustScore = -1.0, // Indicate N/A
                    uploadTimeMs = uploadDurationMs,
                    fileSizeBytes = photoFile.length()
                )
                updateUploadItem(itemId) {
                    it.copy(
                        status = UploadStatus.SUCCESS, // Still a success from upload perspective
                        progress = 1f,
                        result = result,
                        errorMessage = "Trust score failed."
                    )
                }
                scheduleForArchival(itemId)
            }
        ).execute(photoFile.absolutePath)
    }

    private fun scheduleForArchival(itemId: String) {
        viewModelScope.launch {
            delay(LINGER_DURATION_MS)
            archiveItem(itemId)
        }
    }

    private fun archiveItem(itemId: String) {
        val itemToArchive = _uploadQueue.value.find { it.id == itemId }

        if (itemToArchive != null) {
            // Add item to the beginning of the history list, maintaining max size
            _uploadHistory.update { currentHistory ->
                (listOf(itemToArchive) + currentHistory).take(MAX_HISTORY_SIZE)
            }
            // Remove item from the active/lingering queue
            _uploadQueue.update { currentQueue ->
                currentQueue.filter { it.id != itemId }
            }
        }
    }

    private fun updateUploadItem(id: String, updateAction: (UploadItem) -> UploadItem) {
        _uploadQueue.update { currentList ->
            currentList.map { if (it.id == id) updateAction(it) else it }
        }
    }
}
