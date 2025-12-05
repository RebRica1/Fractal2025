package ru.gr05307.viewmodels


// Артем: Главный ViewModel для управления состоянием приложения
class AppViewModel {
    val mainViewModel = MainViewModel()
    val juliaViewModel = JuliaViewModel()

    init {
        mainViewModel.onJuliaPointSelected = { complex ->
            juliaViewModel.onJuliaPointSelected(complex)
        }

        mainViewModel.shouldCloseJuliaPanel = { shouldClose ->
            if (shouldClose && juliaViewModel.showJuliaPanel) {
                juliaViewModel.closeJuliaPanel()
                mainViewModel.resetCloseJuliaFlag()
            }
        }
    }
}