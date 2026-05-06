package com.example.videovault.data.repository

sealed class DeleteRecordingResult {
    data class Success(val fileDeleteOutcome: FileDeleteOutcome) : DeleteRecordingResult()
    data class DatabaseFailure(val throwable: Throwable) : DeleteRecordingResult()
}

enum class FileDeleteOutcome {
    Deleted,
    Missing,
    Failed
}
