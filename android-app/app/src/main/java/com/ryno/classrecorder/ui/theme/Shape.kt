package com.ryno.classrecorder.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * DSH Go 5-stop shape scale + stadium pill, ported from dsh_shapes.dart.
 */
object AppShape {
    val xs = 6.dp
    val sm = 10.dp
    val md = 14.dp
    val lg = 20.dp
    val xl = 28.dp

    val radiusXs: CornerBasedShape = RoundedCornerShape(xs)
    val radiusSm: CornerBasedShape = RoundedCornerShape(sm)
    val radiusMd: CornerBasedShape = RoundedCornerShape(md)
    val radiusLg: CornerBasedShape = RoundedCornerShape(lg)
    val radiusXl: CornerBasedShape = RoundedCornerShape(xl)
    val sheetTop: CornerBasedShape = RoundedCornerShape(topStart = xl, topEnd = xl)
    val pill: CornerBasedShape = CircleShape
}

/**
 * DSH Go spacing & reading-column rhythm, ported from dsh_shapes.dart.
 */
object AppSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    val gutter = 16.dp
    val sheetGutter = 20.dp
    val floatInset = 12.dp
    val readingWidth = 720.dp
    val floatingWidth = 696.dp
}

val DshMaterialShapes = Shapes(
    extraSmall = AppShape.radiusXs,
    small = AppShape.radiusSm,
    medium = AppShape.radiusMd,
    large = AppShape.radiusLg,
    extraLarge = AppShape.radiusXl
)
