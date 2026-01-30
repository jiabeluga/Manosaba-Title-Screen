plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    `maven-publish`
}

version = property("mod_version") as String
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/skiko/dev") }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn.mappings) { classifier("v2") })
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.kotlin)

    // Compose Desktop
    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
    }
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.runtime)

    // Include Compose and Skia for standalone mod
    include(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
    }
    include(compose.foundation)
    include(compose.ui)
    include(compose.runtime)
    
    // Manual include for required modules using string notation to avoid property errors
    include("org.jetbrains.compose.ui:ui-unit:1.7.3")
    include("org.jetbrains.compose.ui:ui-graphics:1.7.3")
    include("org.jetbrains.compose.ui:ui-geometry:1.7.3")
    include("org.jetbrains.compose.ui:ui-text:1.7.3")
    include("org.jetbrains.compose.ui:ui-util:1.7.3")
    include("org.jetbrains.skiko:skiko-awt:0.8.12")
    // 移除错误的 Linux ARM64 依赖，尝试替换为 Android ARM64 依赖
    // include("org.jetbrains.skiko:skiko-awt-runtime-linux-arm64:0.8.12")
    // 尝试直接包含 Android ARM64 运行时，期望 ZL2/PojavLauncher 能正确加载
    // 使用 implementation 而不是 include，因为 Android 运行时可能不需要（也不应该）被打入 Fabric JAR 的 AWT 部分
    // 如果 ZL2 需要它，它应该作为依赖项存在
    // 使用通用 Skiko 运行时依赖，由 Gradle 根据目标环境自动解析
    implementation("org.jetbrains.skiko:skiko:0.8.12")
    include("androidx.collection:collection:1.4.0")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", libs.versions.minecraft.get())
    inputs.property("loader_version", libs.versions.fabric.loader.get())
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to libs.versions.minecraft.get(),
            "loader_version" to libs.versions.fabric.loader.get()
        )
    }
}

val targetJavaVersion = 21

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withSourcesJar()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.base.archivesName.get()
            from(components["java"])
        }
    }

    repositories {
        // Add repositories to publish to here.
    }
}
