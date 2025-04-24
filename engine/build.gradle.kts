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
    // 1. 定义 sourcesJar 任务，明确分类器为 "sources"
    val sourcesJar = tasks.register("sourcesJar", Jar::class) {
        from(fileTree("src/main/java"))
        from(fileTree("src/main/kotlin"))
        archiveClassifier.set("sources")
        archiveExtension.set("jar")  // 显式设置扩展名（可选）
    }

    // 2. 确保主组件生成的是 AAR（Android 库）
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.sho.ss.asuna"
                artifactId = "asuna-engine"
                version = "1.0.0"  // 使用语义化版本

                // 主组件为 Android 库的 AAR 文件
                artifact("${layout.buildDirectory}/outputs/aar/${project.name}-release.aar") {
                    // 主构件无需分类器
                    classifier = null
                    extension = "aar"
                }

                // 附加源码包
                artifact(sourcesJar)
            }
        }
        repositories {
            maven {
                url = uri("https://jitpack.io")
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