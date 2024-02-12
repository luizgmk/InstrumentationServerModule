package com.example.instrumentationserver

import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex

// Gson creation multiple times can be expensive
// Create only once and reuse thereafter
class GsonHelper {
    companion object {
        private var _gson: Gson? = null
        private val lock = Mutex()
        val gson: Gson
            get() {
                val instance = _gson ?: Gson()
                if (_gson == null) synchronized(lock) {
                    _gson = instance
                }
                return instance
            }
    }
}