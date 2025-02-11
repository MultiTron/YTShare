package com.example.ytshare.models

import java.net.InetAddress

class HostModel(var address: String,
                var hostName: String,
                var port: Int) {
    override fun toString(): String {
        return "${address}:${port}"
    }
}