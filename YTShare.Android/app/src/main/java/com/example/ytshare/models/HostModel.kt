package com.example.ytshare.models

data class HostModel(
    val address: String,
    val hostName: String,
    val port: Int
) {
    override fun toString(): String = "$address:$port"
}
