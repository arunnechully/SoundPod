package com.github.soundpod.github

object VersionUtils {
    fun extractVersion(text: String): String {
        // Matches v1.2.3 or 1.2.3 and keeps suffixes like -rc1
        val regex = Regex("""v?(\d+(\.\d+)+(-[a-zA-Z0-9.]+)*)""")
        return regex.find(text)?.groupValues?.get(1) ?: "0"
    }

    fun isNewerVersion(latest: String, current: String): Boolean {
        val latestClean = latest.replace("v", "")
        val currentClean = current.replace("v", "")

        val latestBase = latestClean.split("-")[0].split(".")
        val currentBase = currentClean.split("-")[0].split(".")
        val latestSuffix = latestClean.substringAfter("-", "")
        val currentSuffix = currentClean.substringAfter("-", "")

        val maxLength = maxOf(latestBase.size, currentBase.size)

        for (i in 0 until maxLength) {
            val latestNum = latestBase.getOrNull(i)?.toIntOrNull() ?: 0
            val currentNum = currentBase.getOrNull(i)?.toIntOrNull() ?: 0

            if (latestNum > currentNum) return true
            if (latestNum < currentNum) return false
        }

        // If numeric parts are equal, compare suffixes
        return when {
            // latest is stable (no suffix), current is pre-release (has suffix) -> latest is newer
            latestSuffix.isEmpty() && currentSuffix.isNotEmpty() -> true
            // latest is pre-release, current is stable -> latest is NOT newer
            latestSuffix.isNotEmpty() && currentSuffix.isEmpty() -> false
            // Both are pre-releases, compare alphabetically (simple rc1 vs rc2)
            latestSuffix.isNotEmpty() && currentSuffix.isNotEmpty() -> latestSuffix > currentSuffix
            // Both are stable and equal
            else -> false
        }
    }
}
