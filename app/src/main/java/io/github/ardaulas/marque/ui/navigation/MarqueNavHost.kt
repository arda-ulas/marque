package io.github.ardaulas.marque.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.ardaulas.marque.ui.makes.MakesRoute
import io.github.ardaulas.marque.ui.models.ModelsRoute

/**
 * The app's only navigation graph. Route classes are the typed contract between destinations;
 * each destination's ViewModel reads its arguments back from `SavedStateHandle`, so the graph
 * never passes data to a screen directly.
 */
@Composable
fun MarqueNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = MakesRoute,
        modifier = modifier,
    ) {
        composable<MakesRoute> {
            MakesRoute(onMakeClick = { makeId -> navController.navigate(ModelsRoute(makeId)) })
        }
        composable<ModelsRoute> {
            ModelsRoute(onBack = { navController.popBackStack() })
        }
    }
}
