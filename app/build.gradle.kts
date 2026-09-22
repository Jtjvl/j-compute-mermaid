plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.jcompute.mermaidpocket"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jcompute.mermaidpocket"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Unsigned in this repo. F-Droid (or another store) signs the APK.
            // Minify stays off so release bytecode matches this source.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.webkit:webkit:1.12.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
}

// Muses lock: design/launcher/v2/manifest.json is the checked-in copy of
// https://assets.j-compute.com/mermaidpocket/launcher/v2/manifest.json
tasks.register("verifyLockedLauncher") {
    group = "verification"
    description = "Fail when checked-in launcher assets drift from the Muses v2 lock."
    val manifestFile = rootProject.layout.projectDirectory.file("design/launcher/v2/manifest.json")
    val designDir = rootProject.layout.projectDirectory.dir("design/launcher/v2")
    val resDir = layout.projectDirectory.dir("src/main/res")
    inputs.file(manifestFile)
    inputs.dir(designDir)
    inputs.dir(resDir)
    doLast {
        val manifest = groovy.json.JsonSlurper().parse(manifestFile.asFile) as Map<*, *>
        val errors = mutableListOf<String>()
        fun fail(message: String) {
            errors += message
        }
        if (manifest["status"] != "locked") {
            fail("manifest status is ${manifest["status"]}, expected locked")
        }
        if (manifest["version_label"] != "v2-adaptive-square") {
            fail("manifest version_label is ${manifest["version_label"]}, expected v2-adaptive-square")
        }
        if (manifest["locked_by"] != "Muses") {
            fail("manifest locked_by is ${manifest["locked_by"]}, expected Muses")
        }
        val publicBase = "https://assets.j-compute.com/mermaidpocket/launcher/v2/"
        if (manifest["public_base"] != publicBase) {
            fail("manifest public_base is ${manifest["public_base"]}, expected $publicBase")
        }
        val files = manifest["files"] as? List<*>
        if (files.isNullOrEmpty()) {
            fail("manifest files[] is empty")
        }
        files.orEmpty().forEach { raw ->
            val entry = raw as Map<*, *>
            val path = entry["path"] as String
            val expectedSha = entry["sha256"] as String
            val expectedBytes = (entry["bytes"] as Number).toLong()
            val packed = designDir.file(path).asFile
            if (path == "README.md") {
                if (!packed.isFile) {
                    fail("missing design/launcher/v2/README.md")
                    return@forEach
                }
                val note = packed.readText()
                if (!note.contains(publicBase)) {
                    fail("README.md is missing public_base $publicBase")
                }
                if (!note.contains("locked_by") || !note.contains("Muses")) {
                    fail("README.md is missing locked_by Muses")
                }
                if (!note.contains("curl") || !note.contains("verify")) {
                    fail("README.md is missing the curl manifest → verify → copy refresh steps")
                }
                if (!note.contains("v1 is superseded")) {
                    fail("README.md does not say v1 is superseded")
                }
                if (expectedSha != "fa41984a682ac80d52cef9b0045fd614198a96736ceb6d88489c0f019f35748a" ||
                    expectedBytes != 3248L
                ) {
                    fail("manifest README.md digest drifted from the Muses pack note")
                }
                return@forEach
            }
            if (!packed.isFile) {
                fail("missing design/launcher/v2/$path")
                return@forEach
            }
            val packedSha = sha256Hex(packed)
            if (packedSha != expectedSha || packed.length() != expectedBytes) {
                fail(
                    "design/launcher/v2/$path sha256=$packedSha bytes=${packed.length()} " +
                        "expected sha256=$expectedSha bytes=$expectedBytes",
                )
            }
            if (path.startsWith("mipmap-")) {
                val shipped = resDir.file(path).asFile
                if (!shipped.isFile || sha256Hex(shipped) != expectedSha || shipped.length() != expectedBytes) {
                    fail("app/src/main/res/$path does not match the locked pack")
                }
            }
            if (path == "ic_launcher.xml") {
                listOf(
                    "mipmap-anydpi-v26/ic_launcher.xml",
                    "mipmap-anydpi-v26/ic_launcher_round.xml",
                ).forEach { relative ->
                    val shipped = resDir.file(relative).asFile
                    if (!shipped.isFile || shipped.readBytes().contentEquals(packed.readBytes()).not()) {
                        fail("app/src/main/res/$relative is not the locked adaptive XML")
                    }
                }
            }
        }
        if (errors.isNotEmpty()) {
            throw GradleException(
                "Locked launcher v2 mismatch:\n" + errors.joinToString("\n") { "- $it" },
            )
        }
    }
}

tasks.configureEach {
    if (name == "preBuild") {
        dependsOn("verifyLockedLauncher")
    }
}

fun sha256Hex(file: File): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
