package com.keptang.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The curated set of icons offered by the category icon picker, keyed by a stable string
 * stored in [com.keptang.data.db.CategoryEntity.iconKey] (so the DB never stores an
 * [ImageVector] directly).
 */
object CategoryIcons {
    const val CAR = "car"
    const val RESTAURANT = "restaurant"
    const val CAFE = "cafe"
    const val CART = "cart"
    const val HOME = "home"
    const val BOLT = "bolt"
    const val HEALTH = "health"
    const val MOVIE = "movie"
    const val FLIGHT = "flight"
    const val SCHOOL = "school"
    const val PETS = "pets"
    const val GIFT = "gift"
    const val TAG = "tag"

    private val ICONS: Map<String, ImageVector> = mapOf(
        CAR to Icons.Filled.DirectionsCar,
        RESTAURANT to Icons.Filled.Restaurant,
        CAFE to Icons.Filled.LocalCafe,
        CART to Icons.Filled.ShoppingCart,
        HOME to Icons.Filled.Home,
        BOLT to Icons.Filled.Bolt,
        HEALTH to Icons.Filled.LocalHospital,
        MOVIE to Icons.Filled.Movie,
        FLIGHT to Icons.Filled.Flight,
        SCHOOL to Icons.Filled.School,
        PETS to Icons.Filled.Pets,
        GIFT to Icons.Filled.Redeem,
        TAG to Icons.Filled.Sell
    )

    val KEYS: List<String> = ICONS.keys.toList()

    fun iconFor(key: String): ImageVector = ICONS[key] ?: Icons.Filled.Sell
}
