import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.0"
}

val pluginVersion: String by project
version = pluginVersion

allprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://nexus.velocitypowered.com/repository/maven-public/")
        maven("https://repo.codemc.org/repository/maven-public")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://jitpack.io")
        maven("https://repo.codemc.org/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

project(":hackedserver-core") {

    dependencies {
        implementation("net.kyori:adventure-text-minimessage:4.14.0")
        implementation("io.github.xtomlj:xtomlj:1.1.0")
    }

}

project(":hackedserver-spigot") {

    dependencies {
        compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
        compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0-SNAPSHOT")
        compileOnly("net.kyori:adventure-text-minimessage:4.14.0")
        compileOnly("io.netty:netty-all:4.1.68.Final")
        compileOnly (project(path = ":hackedserver-core", configuration =  "shadow"))

        implementation("dev.jorel:commandapi-bukkit-shade:9.2.0")
        implementation("net.kyori:adventure-platform-bukkit:4.3.0")
        implementation("org.bstats:bstats-bukkit:3.0.0")
    }

}

project(":hackedserver-bungeecord") {

    repositories {
        maven("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
    }

    dependencies {
        compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
        compileOnly("net.kyori:adventure-text-minimessage:4.14.0")
        compileOnly(project(path = ":hackedserver-core", configuration = "shadow"))

        implementation("net.kyori:adventure-platform-bungeecord:4.3.0")
        implementation("org.bstats:bstats-bungeecord:2.2.1")
    }

}

project(":hackedserver-velocity") {

    repositories {
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    }

    dependencies {
        compileOnly("net.kyori:adventure-text-minimessage:4.13.0")
        compileOnly(project(path = ":hackedserver-core", configuration = "shadow"))
        compileOnly("com.velocitypowered:velocity-api:3.1.1")
        annotationProcessor("com.velocitypowered:velocity-api:3.1.1")

        implementation("org.bstats:bstats-velocity:2.2.1")
    }

}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filesNotMatching(listOf("**/*.png", "**/*.ogg", "**/models/**", "**/textures/**", "**/font/**.json", "**/plugin.yml")) {
            expand(mapOf(project.version.toString() to pluginVersion))
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        dependsOn(":hackedserver-core:shadowJar")
        dependsOn(":hackedserver-spigot:shadowJar")
        dependsOn(":hackedserver-bungeecord:shadowJar")
        dependsOn(":hackedserver-velocity:shadowJar")

        relocate("org.bstats", "org.hackedserver.shaded.bstats")
        relocate("org.tomlj", "org.hackedserver.shaded.tomlj")
        relocate("net.kyori", "org.hackedserver.shaded.kyori")
        relocate("org.bstats", "org.hackedserver.shaded.bstats")
        manifest {
            attributes(
                mapOf(
                    "Built-By" to System.getProperty("user.name"),
                    "Version" to pluginVersion,
                    "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSSZ").format(Date.from(Instant.now())),
                    "Created-By" to "Gradle ${gradle.gradleVersion}",
                    "Build-Jdk" to "${System.getProperty("java.version")} ${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")}",
                    "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}",
                    "Compiled" to (project.findProperty("oraxen_compiled")?.toString() ?: "true").toBoolean()
                )
            )
        }
        archiveFileName.set("hackedserver-${pluginVersion}.jar")

        compileJava.get().dependsOn(clean)
        build.get().dependsOn(shadowJar)
    }
}



dependencies {
    implementation(project(path = "hackedserver-core", configuration = "shadow"))
    implementation(project(path = "hackedserver-spigot", configuration = "shadow"))
    implementation(project(path = "hackedserver-bungeecord", configuration = "shadow"))
    implementation(project(path = "hackedserver-velocity", configuration = "shadow"))
    implementation("net.kyori:adventure-text-minimessage:4.13.0")
    implementation("org.yaml:snakeyaml:1.30")
    implementation(kotlin("stdlib-jdk8"))
}

val pluginPath = project.findProperty("oraxen_plugin_path")
val velocityPluginPath = project.findProperty("velocity_plugin_path")
val bungeePluginPath = project.findProperty("bungee_plugin_path")

if (pluginPath != null) {
    tasks {
        register<Copy>("copyJar") {
            this.doNotTrackState("Overwrites the plugin jar to allow for easier reloading")
            dependsOn(shadowJar, jar)
            from(findByName("reobfJar") ?: findByName("shadowJar") ?: findByName("jar"))
            into(pluginPath)
            doLast {
                println("Copied to plugin directory $pluginPath")
            }
        }
        named<DefaultTask>("build").get().dependsOn("copyJar")
    }
}

repositories {
    mavenCentral()
}
