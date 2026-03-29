package dev.slne.surf.microservice.gradle.plugin.task.workflow

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateDockerWorkflowTask : DefaultTask() {
    @get:Input
    abstract val registryUrl: Property<String>

    @get:Input
    abstract val usernameSecret: Property<String>

    @get:Input
    abstract val passwordSecret: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "microservice"
        description = "Generates a GitHub Actions workflow for building and pushing the Docker image"
    }

    @TaskAction
    fun generate() {
        val registry = registryUrl.get()
        val username = usernameSecret.get()
        val password = passwordSecret.get()

        val workflow = """
name: Publish Docker Image

on:
  push:
    branches:
      - version/**
  workflow_dispatch:

env:
  DEFAULT_BRANCH: ${'$'}{{ github.event.repository.default_branch }}

jobs:
  docker:
    runs-on: ubuntu-latest
    environment: production
    permissions:
      contents: read
      packages: write
    steps:
      - name: Collect Workflow Telemetry
        uses: catchpoint/workflow-telemetry-action@v2

      - name: Checkout Repository
        uses: actions/checkout@v6
        with:
          fetch-depth: 0

      - name: Setup Java
        uses: actions/setup-java@v5
        with:
          distribution: "graalvm"
          java-version: 25

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v5
        with:
          allow-snapshot-wrappers: true

      - name: Build with Gradle
        run: ./gradlew shadowJar

      - name: Extract Project Version
        id: get_version
        run: |
          VERSION=${'$'}(./gradlew properties --no-daemon \
            | grep '^version:' \
            | awk '{print ${'$'}2}')

          if [[ "${'$'}VERSION" == *SNAPSHOT* ]]; then
            SNAPSHOT_FLAG=true
          else
            SNAPSHOT_FLAG=false
          fi

          echo "VERSION=${'$'}VERSION" >> ${'$'}GITHUB_ENV
          echo "SNAPSHOT_FLAG=${'$'}SNAPSHOT_FLAG" >> ${'$'}GITHUB_ENV

      - name: Log in to Docker Registry
        uses: docker/login-action@v3
        with:
          registry: $registry
          username: ${'$'}{{ secrets.$username }}
          password: ${'$'}{{ secrets.$password }}

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Determine image tags
        run: |
          CURRENT_BRANCH=${'$'}{GITHUB_REF#refs/heads/}
          if [ "${'$'}{SNAPSHOT_FLAG}" = "true" ] || [ "${'$'}{CURRENT_BRANCH}" != "${'$'}{DEFAULT_BRANCH}" ]; then
            echo "MAKE_LATEST=false" >> ${'$'}GITHUB_ENV
          else
            echo "MAKE_LATEST=true" >> ${'$'}GITHUB_ENV
          fi

      - name: Docker meta
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: $registry/${'$'}{{ github.repository }}
          tags: |
            type=raw,value=${'$'}{{ env.VERSION }}
            type=raw,value=latest,enable=${'$'}{{ env.MAKE_LATEST == 'true' }}

      - name: Build and Push Docker Image
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: ${'$'}{{ steps.meta.outputs.tags }}
          labels: ${'$'}{{ steps.meta.outputs.labels }}
""".trimStart()

        val outputPath = outputFile.get().asFile
        outputPath.parentFile.mkdirs()
        outputPath.writeText(workflow)
        logger.lifecycle("Generated Docker workflow at ${outputPath.absolutePath}")
    }
}
