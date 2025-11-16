package com.github.core.ui.utils

import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.flow.MutableStateFlow

fun <Type : Any> stateFlowSaver() = stateFlowSaverOf<Type, Type>(
    from = { it },
    to = { it }
)

inline fun <Type, Saveable : Any> stateFlowSaverOf(
    crossinline from: (Saveable) -> Type,
    crossinline to: (Type) -> Saveable
) = object : Saver<MutableStateFlow<Type>, Saveable> {
    override fun restore(value: Saveable) = MutableStateFlow(from(value))
    override fun SaverScope.save(value: MutableStateFlow<Type>) = to(value.value)
}

inline fun <reified T : Parcelable> stateListSaver() = listSaver<SnapshotStateList<T>, T>(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)

inline fun <reified E : Enum<E>> enumSaver() = object : Saver<E, String> {
    override fun restore(value: String) = enumValues<E>().first { it.name == value }
    override fun SaverScope.save(value: E) = value.name
}
