package com.github.core.ui.utils

import android.os.Parcel
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.parcelize.Parceler

object DpParceler : Parceler<Dp> {
    override fun create(parcel: Parcel) = parcel.readFloat().dp

    override fun Dp.write(parcel: Parcel, flags: Int) {
        parcel.writeFloat(value)
    }
} 