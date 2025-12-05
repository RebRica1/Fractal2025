package ru.gr05307.viewmodels

import androidx.compose.runtime.*
import ru.gr05307.math.Complex

class JuliaViewModel {
    var currentJuliaPoint by mutableStateOf<Complex?>(null)
    var showJuliaPanel by mutableStateOf(false)

    fun onJuliaPointSelected(complex: Complex) {
        currentJuliaPoint = complex
        showJuliaPanel = true
    }

    fun closeJuliaPanel() {
        currentJuliaPoint = null
        showJuliaPanel = false
    }
}