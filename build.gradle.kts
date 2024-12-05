import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

val pluginVersion = "3.4.1"

allprojects {
    apply(plugin = "idea")
    apply(plugin = "java")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = "org.hackedserver"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.processResources {
        expand(mapOf("projectVersion" to pluginVersion))
    }

    repositories {
        mavenLocal()
        mavenCentral()
        // server software
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://nexus.velocitypowered.com/repository/maven-public/") }
        // BStats
        maven { url = uri("https://repo.codemc.org/repository/maven-public") }
        // ProtocolLib
        maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
        // JitPack
        maven { url = uri("https://jitpack.io") }
        // nbt api (used by command api)
        maven { url = uri("https://repo.codemc.org/repository/maven-public/") }
        // adventure
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        // commandAPI snapshots
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }
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
        compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
        compileOnly("net.kyori:adventure-text-minimessage:4.14.0")
        compileOnly("io.netty:netty-all:4.1.68.Final")
        compileOnly(project(path = ":hackedserver-core", configuration = "shadow"))

        implementation("dev.jorel:commandapi-bukkit-shade:9.6.1")
        implementation("net.kyori:adventure-platform-bukkit:4.3.0")
        implementation("org.bstats:bstats-bukkit:3.1.0")
    }
}

project(":hackedserver-bungeecord") {
    repositories {
        maven { url = uri("https://mvn.exceptionflug.de/repository/exceptionflug-public/") }
    }

    dependencies {
        compileOnly("net.md-5:bungeecord-api:1.20-R0.2")
        compileOnly("net.kyori:adventure-text-minimessage:4.14.0")
        compileOnly(project(path = ":hackedserver-core", configuration = "shadow"))

        implementation("net.kyori:adventure-platform-bungeecord:4.3.0")
        implementation("org.bstats:bstats-bungeecord:3.1.0")
    }
}

project(":hackedserver-velocity") {
    repositories {
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://mvn.exceptionflug.de/repository/exceptionflug-public/") }
    }

    dependencies {
        compileOnly("net.kyori:adventure-text-minimessage:4.13.0")
        compileOnly(project(path = ":hackedserver-core", configuration = "shadow"))
        compileOnly("com.velocitypowered:velocity-api:3.1.0")
        annotationProcessor("com.velocitypowered:velocity-api:3.1.0")
        compileOnly("dev.simplix:protocolize-api:2.4.1")

        implementation("org.bstats:bstats-velocity:3.1.0")
    }
}

tasks.shadowJar {
    relocate("org.bstats", "org.hackedserver.shaded.bstats")
    relocate("org.tomlj", "org.hackedserver.shaded.tomlj")
    relocate("org.bstats", "org.hackedserver.shaded.bstats")
    relocate("net.kyori.adventure.platform.bukkit", "org.hackedserver.shaded.kyori.adventure.platform.bukkit")
    manifest {
        attributes(
            "Built-By" to System.getProperty("user.name"),
            "Version" to pluginVersion,
            "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSSZ").format(Date()),
            "Created-By" to "Gradle ${gradle.gradleVersion}",
            "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})",
            "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}"
        )
    }
    archiveFileName.set("hackedserver-${pluginVersion}.jar")
}

dependencies {
    implementation(project(path = "hackedserver-core", configuration = "shadow"))
    implementation(project(path = "hackedserver-spigot", configuration = "shadow"))
    implementation(project(path = "hackedserver-bungeecord", configuration = "shadow"))
    implementation(project(path = "hackedserver-velocity", configuration = "shadow"))
    implementation("net.kyori:adventure-text-minimessage:4.13.0")
    implementation("org.yaml:snakeyaml:1.30")
}

tasks.compileJava {
    dependsOn(tasks.clean)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

val copyJar: Boolean = project.findProperty("copyJar")?.toString()?.toBoolean() ?: false
val pluginPath: String? = project.findProperty("hacked_server_plugin_path")?.toString()
val velocityPluginPath: String? = project.findProperty("velocity_plugin_path")?.toString()
val bungeePluginPath: String? = project.findProperty("bungee_plugin_path")?.toString()

if (copyJar) {
    val copyJarTask = tasks.register<Copy>("copyJarTask") {
        if (pluginPath != null) {
            from("build/libs/hackedserver-all.jar")
            into(pluginPath)
            doLast {
                println("Copied to plugin directory $pluginPath")
            }
        }
        if (velocityPluginPath != null) {
            from("build/libs/hackedserver-all.jar")
            into(velocityPluginPath)
            doLast {
                println("Copied to plugin directory $velocityPluginPath")
            }
        }
        if (bungeePluginPath != null) {
            from("build/libs/hackedserver-all.jar")
            into(bungeePluginPath)
            doLast {
                println("Copied to plugin directory $bungeePluginPath")
            }
        }
    }

    copyJarTask {
        dependsOn(tasks.shadowJar)
    }

    tasks.named("build") {
        dependsOn(copyJarTask)
    }
}