package com.omadroid.launcher

/** One cell. Length is still TBD; one unit is one line of text or one control. */
const val UNIT_DP = 48

enum class Attach {
    Top,
    Bottom,
    Fill,
}

data class GridMetrics(
    val unitPx: Int,
    val columns: Int,
    val rows: Int,
    val widthPx: Int,
    val heightPx: Int,
) {
    val extraWidthPx: Int = widthPx - columns * unitPx
    val extraHeightPx: Int = heightPx - rows * unitPx
}

data class GridRect(
    val column: Int,
    val row: Int,
    val columns: Int,
    val rows: Int,
)

data class PixelRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

data class SpaceClaim(
    val module: BuiltinModule,
    val rows: Int? = null,
    val columns: Int? = null,
    val attach: Attach = Attach.Fill,
)

data class AllocatedSpace(
    val module: BuiltinModule,
    val units: GridRect,
    val pixels: PixelRect,
    val attach: Attach,
)

fun unitLengthPx(density: Float): Int = (UNIT_DP * density).toInt().coerceAtLeast(1)

fun measureGrid(widthPx: Int, heightPx: Int, unitPx: Int): GridMetrics {
    val unit = unitPx.coerceAtLeast(1)
    return GridMetrics(
        unitPx = unit,
        columns = (widthPx / unit).coerceAtLeast(1),
        rows = (heightPx / unit).coerceAtLeast(1),
        widthPx = widthPx.coerceAtLeast(0),
        heightPx = heightPx.coerceAtLeast(0),
    )
}

fun defaultSpaceClaims(): List<SpaceClaim> =
    listOf(
        SpaceClaim(BuiltinModule.Bar, rows = 1, attach = Attach.Top),
        SpaceClaim(BuiltinModule.LauncherBar, rows = 1, attach = Attach.Bottom),
        SpaceClaim(BuiltinModule.Workspaces, attach = Attach.Fill),
    )

fun allocateSpace(grid: GridMetrics, claims: List<SpaceClaim>): Map<BuiltinModule, AllocatedSpace> {
    var nextTop = 0
    var nextBottom = grid.rows
    val units = linkedMapOf<BuiltinModule, Pair<GridRect, Attach>>()

    claims.filter { it.attach == Attach.Top }.forEach { claim ->
        val height = claimedRows(claim, nextBottom - nextTop)
        val width = claim.columns ?: grid.columns
        units[claim.module] = GridRect(0, nextTop, width, height) to claim.attach
        nextTop += height
    }
    claims.filter { it.attach == Attach.Bottom }.forEach { claim ->
        val height = claimedRows(claim, nextBottom - nextTop)
        val width = claim.columns ?: grid.columns
        nextBottom -= height
        units[claim.module] = GridRect(0, nextBottom, width, height) to claim.attach
    }
    claims.filter { it.attach == Attach.Fill }.forEach { claim ->
        val height = (nextBottom - nextTop).coerceAtLeast(0)
        val width = claim.columns ?: grid.columns
        units[claim.module] = GridRect(0, nextTop, width, height) to claim.attach
    }

    return units.mapValues { (module, pair) ->
        val (rect, attach) = pair
        AllocatedSpace(
            module = module,
            units = rect,
            pixels = toPixels(grid, rect, attach),
            attach = attach,
        )
    }
}

private fun claimedRows(claim: SpaceClaim, remaining: Int): Int {
    val wanted = claim.rows ?: 1
    return wanted.coerceAtMost(remaining.coerceAtLeast(0)).coerceAtLeast(0)
}

private fun toPixels(grid: GridMetrics, rect: GridRect, attach: Attach): PixelRect {
    val width =
        rect.columns * grid.unitPx +
            if (rect.column + rect.columns >= grid.columns) grid.extraWidthPx else 0
    val height =
        rect.rows * grid.unitPx +
            if (attach == Attach.Fill) grid.extraHeightPx else 0
    return PixelRect(
        left = rect.column * grid.unitPx,
        top = rect.row * grid.unitPx,
        width = width,
        height = height,
    )
}
