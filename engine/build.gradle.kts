import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.testImplementation

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

android {
    namespace = "com.sho.ss.asuna"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("release") {
                // Applies the component for the release build variant.
                // from(components["release"])
                // You can then customize attributes of the publication as shown below.
                groupId = "com.sho.ss.asuna"
                artifactId = "asuna-engine"
                version = version
            }
        }
    }
}

dependencies {

    // Engine依赖项
    implementation(files("libs/codec-base64.jar"))
    implementation(libs.kotlinx.serialization.json)
    //暂时不可升级，因为新版和app模块下的依赖库LeanCloud自带的gson冲突
    implementation(libs.gson)
    //Xpath1.0 parser
    implementation(libs.jsoupxpath)
    implementation(libs.slf4j.log4j12)
    //依赖kotlin反射包，可解决kotlin数据类被JSON反射序列化时找不到默认无参构造器的问题
    implementation(libs.kotlin.reflect)
    implementation(libs.androidx.core.ktx.v1120)

    //Extension依赖项
    implementation(libs.jedis)
    implementation(libs.guava)

    // Core依赖项
    implementation(libs.commons.io)
    implementation(libs.fastjson)
    implementation(libs.commons.lang3)
    implementation(libs.commons.codec)
    implementation(libs.httpclient)
    implementation(libs.xsoup)
    implementation(libs.commons.collections)
    implementation(libs.jsonpath.json.path)
    testImplementation(libs.moco.core) {
        exclude("org.slf4j", "slf4j-simple")
    }
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.all)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}