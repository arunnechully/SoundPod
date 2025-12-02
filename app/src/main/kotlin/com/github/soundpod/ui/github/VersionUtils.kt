package com.github.soundpod.ui.github

object VersionUtils {
    fun extractVersion(text: String): String {
        val regex = Regex("""v?(\d+(\.\d+)+)""")
        return regex.find(text)?.value ?: "0"
    }

    fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.replace("v", "").split(".")
        val currentParts = current.replace("v", "").split(".")
        val maxLength = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until maxLength) {
            val latestNum = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
            val currentNum = currentParts.getOrNull(i)?.toIntOrNull() ?: 0

            if (latestNum > currentNum) return true
            if (latestNum < currentNum) return false
        }
        return false
    }
}