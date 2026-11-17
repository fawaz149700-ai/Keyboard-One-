package com.example.ime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BiometricLockManager {
    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    fun markUnlocked() {
        _isSessionUnlocked.value = true
    }

    fun lock() {
        _isSessionUnlocked.value = false
    }
}
