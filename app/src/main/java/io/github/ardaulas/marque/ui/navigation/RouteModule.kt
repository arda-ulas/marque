package io.github.ardaulas.marque.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Decodes typed routes from the destination's [SavedStateHandle] (a default binding of the
 * ViewModel component) so ViewModels take their route as a plain constructor argument. This is
 * the only place `toRoute()` is called: it needs an Android `Bundle` underneath, so keeping it
 * out of the ViewModels lets their unit tests run on the JVM with `ModelsRoute(448)` directly.
 */
@Module
@InstallIn(ViewModelComponent::class)
object RouteModule {
    @Provides
    fun provideModelsRoute(savedStateHandle: SavedStateHandle): ModelsRoute = savedStateHandle.toRoute<ModelsRoute>()
}
