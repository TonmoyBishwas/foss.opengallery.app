package foss.opengallery.app.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import foss.opengallery.app.OpenGalleryApp
import foss.opengallery.app.di.AppContainer

/**
 * ViewModel factory hook for our manual DI: gives every ViewModel access to
 * the [AppContainer] without a DI framework.
 */
@Composable
inline fun <reified VM : ViewModel> ogViewModel(
    key: String? = null,
    noinline create: (AppContainer) -> VM,
): VM = viewModel(
    key = key,
    factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                as OpenGalleryApp
            @Suppress("UNCHECKED_CAST")
            return create(app.container) as T
        }
    },
)

/** Non-composable variant for places holding an Application reference. */
fun appContainer(application: Application): AppContainer =
    (application as OpenGalleryApp).container
