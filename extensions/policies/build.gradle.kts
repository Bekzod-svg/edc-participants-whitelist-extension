plugins {
    `java-library`
}

dependencies {
    api(libs.edc.spi.contract)
    api(libs.edc.spi.policy.engine)
    implementation(libs.ih.spi.core)
    implementation(project(":extensions:trusted-participants-whitelist"))
    testImplementation(libs.edc.core.policy.engine)
}
