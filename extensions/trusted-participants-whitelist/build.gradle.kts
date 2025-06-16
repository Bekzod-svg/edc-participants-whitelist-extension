plugins {
    `java-library`
}

val rsApi: String by project

dependencies {
    implementation(libs.edc.http)
    implementation(libs.edc.configuration.filesystem)
    implementation(libs.jakarta.rsApi)
    implementation(libs.jakarta.inject.api)
    compileOnly(libs.jakarta.cdi.api)
}