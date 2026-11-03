plugins {
    java
}

group = "de.kibot"
version = "1.8"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper supplies the Bukkit/Paper APIs at runtime; do not bundle them in the plugin JAR.
    compileOnly("io.papermc.paper:paper-api:[26.3.build,)")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveBaseName.set("ki-plugin")
}
