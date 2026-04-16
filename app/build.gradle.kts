plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.secrets)
    id("io.objectbox")
}

android {
    namespace = "com.example.runup"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.runup"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // 중복 리소스 제외 (기본 설정)
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/AL2.0")
            excludes.add("META-INF/LGPL2.1")

            // 만약 빌드 시 .so 파일 충돌이 다시 나면 아래 줄의 주석을 푸세요.
            // pickFirsts.add("**/libobjectbox-jni.so")
            pickFirst("lib/x86/libtensorflowlite_jni.so")
            pickFirst("lib/x86_64/libtensorflowlite_jni.so")
            pickFirst("lib/armeabi-v7a/libtensorflowlite_jni.so")
            pickFirst("lib/arm64-v8a/libtensorflowlite_jni.so")
        }
    }

    androidResources {
        noCompress += listOf("task", "bin", "tflite")
    }
}

configurations.all {
    resolutionStrategy {
        // 모든 중복된 모듈 요청을 특정 버전으로 강제 고정하여 통합합니다.
        force("io.objectbox:objectbox-android:4.0.0")

        // 혹은 브라우저 라이브러리가 들고 오는 녀석을 아예 무시하게 만듭니다.
        dependencySubstitution {
            substitute(module("io.objectbox:objectbox-android-objectbrowser"))
                .using(module("io.objectbox:objectbox-android-objectbrowser:4.0.0"))
                .because("중복 클래스 충돌 방지를 위해 버전을 명시적으로 제어함")
        }
    }
}

hilt{
    enableAggregatingTask = false
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.compose.foundation.layout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    //구글맵 의존성
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation(libs.google.play.services.location)

    //room db 관련
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    //DataStore 의존성
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation(libs.accompanist.permissions)
    implementation(libs.maps.compose)

    // Firebase (BOM 방식을 사용하면 개별 라이브러리 버전을 맞출 필요가 없어 편리합니다)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    // 구글 로그인 & Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Kotlin Coroutines Play Services (await() 함수 사용을 위해 필수)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("com.google.firebase:firebase-storage-ktx")
    // 파이어베이스의 데이터들을 로컬에서 objectbox로 관리하기 위함
    implementation(libs.objectbox.kotlin)

    // gson 형태로 매핑하기 위해 필요함
    implementation(libs.google.gson)

    // tflite 모델 불러오기 위해 필요함
    implementation(libs.litert)
    implementation(libs.litert.api)
    implementation(libs.litert.support)
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")

    // Version Catalog를 이용한 라이브러리 추가
    implementation(libs.mediapipe.tasks.genai)

    // Version Catalog를 통해 Gemini SDK 추가
    implementation(libs.google.generativeai)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    // Hilt Worker용 Annotation Processor (KSP 권장)
    ksp(libs.androidx.hilt.compiler)

    // TOML에서 정의한 네이버 지도 라이브러리 추가
    implementation(libs.naver.map.compose)
    implementation(libs.naver.map.sdk)  // 이거 추가
    implementation("io.github.fornewid:naver-map-location:21.0.2")
    //아이콘 확장팩 추가
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0")
    // Coil 기본 라이브러리
    implementation("io.coil-kt.coil3:coil-compose:3.0.0")
}




















