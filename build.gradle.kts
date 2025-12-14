val fullVersion = "3.0.0"
val snapshot = true

fun getVersionMeta(includeHash: Boolean): String {
    if (!snapshot) {
        return ""
    }

    var commitHash = ""
    if (includeHash && file(".git").isDirectory) {
        val result = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()

        commitHash = "+$result"
    }

    return "$commitHash-SNAPSHOT"
}
version = "$fullVersion${getVersionMeta(true)}"
ext["versionNoHash"] = "$fullVersion${getVersionMeta(false)}"