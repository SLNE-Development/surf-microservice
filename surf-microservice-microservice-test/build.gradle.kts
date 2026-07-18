import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("dev.slne.surf.api.gradle.standalone")
}

dependencies {
    compileOnlyApi(projects.surfMicroserviceApi.surfMicroserviceApiMicroservice)
    runtimeOnly(projects.surfMicroserviceMicroservice)
}

tasks.withType<ShadowJar>().configureEach {
    mainClass.set("dev.slne.surf.microservice.runtime.microservice.MicroserviceLauncherKt")
}
