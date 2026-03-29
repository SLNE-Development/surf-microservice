package dev.slne.surf.microservice.gradle.plugin.task.workflow

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateBuildWorkflowTask : DefaultTask() {
    @get:Input
    abstract val moduleRegex: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "microservice"
        description = "Generates a GitHub Actions workflow for building JARs and publishing to Maven"
    }

    @TaskAction
    fun generate() {
        val regex = moduleRegex.get()

        val moduleRegexEnv = if (regex.isNotEmpty()) {
            "\n  MODULE_REGEX: \"$regex\""
        } else {
            ""
        }

        val findJarsStep = if (regex.isNotEmpty()) {
            """
      - name: Find and filter JAR files
        id: find_jars
        run: |
          echo "JAR_FILES<<EOF" >> ${'$'}GITHUB_OUTPUT
          find . -path "*/build/libs/*-all.jar" -type f | while read -r f; do
            if echo "${'$'}f" | grep -qP "${'$'}{MODULE_REGEX}"; then
              echo "${'$'}f"
            fi
          done
          echo "EOF" >> ${'$'}GITHUB_OUTPUT"""
        } else {
            """
      - name: Find JAR files
        id: find_jars
        run: |
          echo "JAR_FILES<<EOF" >> ${'$'}GITHUB_OUTPUT
          find . -path "*/build/libs/*-all.jar" -type f | while read -r f; do
            echo "${'$'}f"
          done
          echo "EOF" >> ${'$'}GITHUB_OUTPUT"""
        }

        val workflow = """
name: Build & Publish

on:
  push:
    branches:
      - version/**
  workflow_dispatch:

env:
  SLNE_SNAPSHOTS_REPO_USERNAME: ${'$'}{{ secrets.SLNE_SNAPSHOTS_REPO_USERNAME }}
  SLNE_SNAPSHOTS_REPO_PASSWORD: ${'$'}{{ secrets.SLNE_SNAPSHOTS_REPO_PASSWORD }}
  SLNE_RELEASES_REPO_USERNAME: ${'$'}{{ secrets.SLNE_RELEASES_REPO_USERNAME }}
  SLNE_RELEASES_REPO_PASSWORD: ${'$'}{{ secrets.SLNE_RELEASES_REPO_PASSWORD }}
  DEFAULT_BRANCH: ${'$'}{{ github.event.repository.default_branch }}$moduleRegexEnv

jobs:
  build:
    runs-on: ubuntu-latest
    environment: production
    permissions:
      contents: write
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

      - name: Check with Gradle
        run: ./gradlew check

      - name: Publish to Maven
        run: ./gradlew publish

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

      - name: Determine release flags
        run: |
          CURRENT_BRANCH=${'$'}{GITHUB_REF#refs/heads/}
          if [ "${'$'}{SNAPSHOT_FLAG}" = "true" ]; then
            echo "PRERELEASE=true" >> ${'$'}GITHUB_ENV
          else
            echo "PRERELEASE=false" >> ${'$'}GITHUB_ENV
          fi
          if [ "${'$'}{SNAPSHOT_FLAG}" = "true" ] || [ "${'$'}{CURRENT_BRANCH}" != "${'$'}{DEFAULT_BRANCH}" ]; then
            echo "MAKE_LATEST=false" >> ${'$'}GITHUB_ENV
          else
            echo "MAKE_LATEST=true" >> ${'$'}GITHUB_ENV
          fi
$findJarsStep

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v${'$'}{{ env.VERSION }}
          name: Release v${'$'}{{ env.VERSION }}
          prerelease: ${'$'}{{ env.PRERELEASE }}
          make_latest: ${'$'}{{ env.MAKE_LATEST }}
          generate_release_notes: true
          files: ${'$'}{{ steps.find_jars.outputs.JAR_FILES }}
""".trimStart()

        val outputPath = outputFile.get().asFile
        outputPath.parentFile.mkdirs()
        outputPath.writeText(workflow)
        logger.lifecycle("Generated build workflow at ${outputPath.absolutePath}")
    }
}
