import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.sonar)
    id("jacoco")
    alias(libs.plugins.gms)
}

extensions.configure<BaseAppModuleExtension>("android") {
    namespace = "com.android.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.android.sample"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testCoverage {
        jacocoVersion = "0.8.11"
    }

    buildFeatures.compose = true

    composeOptions {
        // Keep as you had; if you upgrade your Compose BOM, align this version.
        kotlinCompilerExtensionVersion = "1.4.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions.jvmTarget = "11"

    // Avoid META-INF collisions during :merge*JavaResource
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE*,NOTICE*,DEPENDENCIES}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE.txt"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    // 🔧 Robolectric source-set mapping you had
    sourceSets.getByName("testDebug") {
        val test = sourceSets.getByName("test")
        java.setSrcDirs(test.java.srcDirs)
        res.setSrcDirs(test.res.srcDirs)
        resources.setSrcDirs(test.resources.srcDirs)
    }
    sourceSets.getByName("test") {
        java.setSrcDirs(emptyList<File>())
        res.setSrcDirs(emptyList<File>())
        resources.setSrcDirs(emptyList<File>())
    }
}

sonar {
    properties {
        property("sonar.projectKey", "EduMonSwent_EduMon")
        property("sonar.projectName", "EduMon")
        property("sonar.organization", "edumonswent")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.junit.reportPaths", "${project.layout.buildDirectory.get()}/test-results/testDebugUnitTest/")
        property("sonar.androidLint.reportPaths", "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
    }
}

// Helper to add a dep to both unit & instrumented tests when truly needed
fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
    androidTestImplementation(dep)
    testImplementation(dep)
}

dependencies {
    // ---- Core ----
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ---- Compose with BOM ----
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    testImplementation(composeBom)          // ✅ make BOM available to test configs
    androidTestImplementation(composeBom)   // ✅ make BOM available to androidTest

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.preview)
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation(libs.compose.tooling)

    // Navigation (single version)
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // ---- Networking ----
    implementation(libs.okhttp)

    // ---- Firebase / Google ----
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-tasks:18.1.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database.ktx)

    // (keep these if your version catalog defines them and you use Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // ===================== Tests =====================

    // JVM unit tests (JUnit4 so Jupiter jars don't leak into androidTest)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation(libs.mockk)

    // Compose UI tests on JVM (resolved via BOM above)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Android instrumented tests
    // AndroidX Test (aligned versions)
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.agent)

    implementation("androidx.navigation:navigation-compose:2.8.3")

    globalTestImplementation("androidx.navigation:navigation-testing:2.8.3")

    // (Optional) Kaspresso — if you use it in both test types
    // globalTestImplementation(libs.kaspresso)
    // globalTestImplementation(libs.kaspresso.compose)
}

tasks.withType<Test> {
    // Jacoco for unit tests
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

// ✅ JaCoCo aggregate report (unit + instrumented)
tasks.register("jacocoTestReport", JacocoReport::class) {
    mustRunAfter("testDebugUnitTest", "connectedDebugAndroidTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.layout.projectDirectory}/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        include("outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec")
    })

    // Patch to remove invalid JaCoCo lines (nr="65535") that can break Sonar coverage
    doLast {
        val reportFile = reports.xml.outputLocation.asFile.get()
        if (reportFile.exists()) {
            val content = reportFile.readText()
            val cleanedContent = content.replace("<line[^>]+nr=\"65535\"[^>]*>".toRegex(), "")
            reportFile.writeText(cleanedContent)
        }
    }
}
