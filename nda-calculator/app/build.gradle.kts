plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.kapt")
	id("org.jetbrains.kotlin.plugin.compose")
}

android {
	namespace = "com.example.ndacalculator"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.example.ndacalculator"
		minSdk = 24
		targetSdk = 34
		versionCode = 1
		versionName = "1.0.0"

		vectorDrawables {
			useSupportLibrary = true
		}
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
		isCoreLibraryDesugaringEnabled = true
	}

	kotlinOptions {
		jvmTarget = "17"
	}

	buildFeatures {
		compose = true
	}

	composeOptions {
		kotlinCompilerExtensionVersion = "1.5.14"
	}

	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

kotlin {
	jvmToolchain(17)
}

dependencies {
	val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
	implementation(composeBom)
	androidTestImplementation(composeBom)

	implementation("androidx.core:core-ktx:1.13.1")
	implementation("androidx.activity:activity-compose:1.9.2")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

	implementation("androidx.room:room-runtime:2.6.1")
	kapt("androidx.room:room-compiler:2.6.1")
	implementation("androidx.room:room-ktx:2.6.1")

	implementation("com.google.android.material:material:1.12.0")

	testImplementation("junit:junit:4.13.2")
	androidTestImplementation("androidx.test.ext:junit:1.2.1")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
	androidTestImplementation("androidx.compose.ui:ui-test-junit4")
	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")
	coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
}