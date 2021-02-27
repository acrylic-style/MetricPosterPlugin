plugins {
    kotlin("jvm") version "1.4.21"
}

group = "xyz.acrylicstyle.metric"
version = "1.0"
val mainClassName = "xyz.acrylicstyle.metric.MetricPosterKt"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.acrylicstyle.xyz") }
    maven { url = uri("https://repo2.acrylicstyle.xyz") }
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")
    implementation("xyz.acrylicstyle:java-util-all:0.14.4")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xjsr305=strict"
        )
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "utf-8"
    }

    withType<ProcessResources> {
        filteringCharset = "UTF-8"
        from(sourceSets.main.get().resources.srcDirs) {
            include("**")

            val tokenReplacementMap = mapOf(
                "version" to project.version,
                "name" to project.rootProject.name
            )

            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
        }

        from(projectDir) { include("LICENSE") }
    }

    withType<Jar> {
        manifest {
            attributes(
                "Main-Class" to mainClassName
            )
        }
        from(configurations.getByName("implementation").apply { isCanBeResolved = true }.map { if (it.isDirectory) it else zipTree(it) })
    }
}
