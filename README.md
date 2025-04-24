# AsunaEngine
基于Webmagic的安卓爬虫引擎，可通过自定义视频源来抓取网站数据和视频数据。

### 引入方式  
 
 1.在项目级 settings.gradle.kts 内添加
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") } //引入jitpack远程仓库
    }
}
``` 
2.在app模块下的 build.gradle.kts 内的 dependencies 中添加
```kotlin
dependencies {
    implementation("com.github.KamiNoYuki:AsunaEngine:1.0.0-beta")
}
```

### 使用
通过实例化 **AsunaEngine()** 来进行调用，例如发起搜索。
```kotlin
AsunaEngine().search(sourcesList, "keyword", object : NewSearchListener() {
    override fun onStarted(keyword String) {
        println("onSearchStarted")
    }
    ....省略其他回调
})
```
