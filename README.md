# AsunaEngine
基于Webmagic的安卓爬虫引擎模块，可通过自定义视频源配置文件来抓取视频数据。

添加方式  
1.在项目级settings.gradle.kts内加入
`kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}
`  
2.在app模块下的build.gradle.kts内的dependencies中加入  
`kotlin
dependencies {
		implementation("com.github.KamiNoYuki:AsunaEngine:1.0.0-beta")
	}
`
