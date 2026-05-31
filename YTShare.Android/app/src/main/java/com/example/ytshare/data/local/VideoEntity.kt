package com.example.ytshare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val createdAt: Long,
    val synced: Boolean = true
)
