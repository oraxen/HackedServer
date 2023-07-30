rootProject.name = "HackedServer"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

include(
    "hackedserver-core",
    "hackedserver-spigot",
    "hackedserver-bungeecord",
    "hackedserver-velocity",
)

