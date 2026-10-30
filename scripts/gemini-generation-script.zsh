#!/usr/bin/env zsh
set -e

echo "Scaffolding complete EnvDown multiplatform workspace..."
#mkdir -p envdown-cli-project/.github/workflows
#cd envdown-cli-project

# Create directory structures
mkdir -p envdown-cli-core/src
mkdir -p envdown-cli-mac/src
mkdir -p envdown-cli-linux/src
mkdir -p envdown-cli-windows/src

# 1. Root Toolchain Configuration
cat << 'EOF' > project.yaml
group: dev.fnc314.tools.envdown
modules:
  - envdown-cli-core
  - envdown-cli-mac
  - envdown-cli-linux
  - envdown-cli-windows
EOF

# 2. Core Library Configuration
cat << 'EOF' > envdown-cli-core/module.yaml
product:
  type: kmp/lib
  platforms: [
    macosArm64,
    macosX64,
    linuxX64,
    mingwX64
  ]
dependencies:
  - $libs.kotlinx.io
EOF

# 3. Core Engine Processor
cat << 'EOF' > envdown-cli-core/src/EnvDown.kt
package dev.fnc314.tools.envdown.cli.core

private val unescapePattern = Regex("\\\\\\$\\{env\\.")

fun processEnvDown(
    input: String, 
    strictMode: Boolean = false,
    getEnv: (String) -> String?
): String {
    val patternWithDefault = Regex("(?<!\\\\)\\$\\{env\\.([a-zA-Z_][a-zA-Z0-9_]*)\\:-(.*?)\\}")
    val patternWithoutDefault = Regex("(?<!\\\\)\\$\\{env\\.([a-zA-Z_][a-zA-Z0-9_]*)\\}")

    val processedText = patternWithDefault.replace(input) { matchResult ->
        val varName = matchResult.groups[1]?.value ?: ""
        val defaultValue = matchResult.groups[2]?.value ?: ""
        val envValue = getEnv(varName)

        when {
            !envValue.isNullOrEmpty() -> envValue
            else -> defaultValue
        }
    }

    val processedText2 = patternWithoutDefault.replace(processedText) { matchResult ->
        val varName = matchResult.groups[1]?.value ?: ""
        val envValue = getEnv(varName)

        when {
            !envValue.isNullOrEmpty() -> envValue
            strictMode -> throw IllegalArgumentException(
                "EnvDown Error: Environment variable '$varName' is missing and no default was provided."
            )
            else -> ""
        }
    }

    return processedText2.replace(unescapePattern, """\$\{env.""")
}
EOF

# 4. Core CLI Runner
cat << 'EOF' > envdown-cli-core/src/EnvDownCli.kt
package dev.fnc314.tools.envdown.cli.core

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.system.exitProcess

fun runCli(args: Array<String>, getEnv: (String) -> String?) {
    var strictMode = false
    var filePath: String? = null

    for (arg in args) {
        if (arg == "--strict") {
            strictMode = true
        } else if (!arg.startsWith("-")) {
            filePath = arg
        }
    }

    if (filePath == null) {
        println("Usage: envdown-cli [--strict] <file.envdown>")
        exitProcess(1)
    }

    val path = Path(filePath)
    if (!SystemFileSystem.exists(path)) {
        println("Error: File '$filePath' not found.")
        exitProcess(1)
    }

    try {
        val input = SystemFileSystem.source(path).buffered().readString()
        val output = processEnvDown(input, strictMode, getEnv)
        println(output)
    } catch (e: Exception) {
        println(e.message)
        exitProcess(1)
    }
}
EOF

# 5. macOS App Module
cat << 'EOF' > envdown-cli-mac/module.yaml
product: macos/app
dependencies:
  - //envdown-cli-core
settings:
  native:
    entryPoint: dev.fnc314.tools.envdown.cli.mac.main
EOF

cat << 'EOF' > envdown-cli-mac/src/Main.kt
package dev.fnc314.tools.envdown.cli.mac

import dev.fnc314.tools.envdown.cli.core.runCli
import kotlinx.cinterop.toKString
import platform.posix.getenv

public fun main(args: Array<String>) {
    runCli(args) { key -> getenv(key)?.toKString() }
}
EOF

# 6. Linux App Module
cat << 'EOF' > envdown-cli-linux/module.yaml
product: linux/app
dependencies:
  - //envdown-cli-core
settings:
  native:
    entryPoint: dev.fnc314.tools.envdown.cli.linux.main
EOF

cat << 'EOF' > envdown-cli-linux/src/Main.kt
package dev.fnc314.tools.envdown.cli.linux

import dev.fnc314.tools.envdown.cli.core.runCli
import kotlinx.cinterop.toKString
import platform.posix.getenv

public fun main(args: Array<String>) {
    runCli(args) { key -> getenv(key)?.toKString() }
}
EOF

# 7. Windows App Module
cat << 'EOF' > envdown-cli-windows/module.yaml
product: windows/app
dependencies:
  - //envdown-cli-core
settings:
  native:
    entryPoint: dev.fnc314.tools.envdown.cli.windows.main
EOF

cat << 'EOF' > envdown-cli-windows/src/Main.kt
package dev.fnc314.tools.envdown.cli.windows

import dev.fnc314.tools.envdown.cli.core.runCli
import kotlinx.cinterop.toKString
import platform.posix.getenv

public fun main(args: Array<String>) {
    runCli(args) { key -> getenv(key)?.toKString() }
}
EOF

# 8. GitHub Actions Workflow (Mac, Linux, Windows, & Homebrew)
cat << 'EOF' > .github/workflows/release.yml
name: Publish Release and Update Homebrew

on:
  release:
    types: [published]

jobs:
  build-and-upload:
    strategy:
      matrix:
        include:
          - os: macos-14
            module: cli-mac
            target: macos-arm64
            ext: ""
          - os: macos-13
            module: cli-mac
            target: macos-x64
            ext: ""
          - os: ubuntu-latest
            module: cli-linux
            target: linux-x64
            ext: ""
          - os: windows-latest
            module: cli-windows
            target: mingw-x64
            ext: ".exe"
    runs-on: ${{ matrix.os }}
    defaults:
      run:
        shell: bash
    steps:
      - name: Checkout Source
        uses: actions/checkout@v4

      - name: Build Native Binary
        run: kotlin build -m ${{ matrix.module }}

      - name: Test Native Binary
        run: |
          cat << 'TEST' > test.envdown
          Var: ${env.TEST_VAR}
          TEST

          export TEST_VAR="Success"
          OUTPUT=$(${{ matrix.module }}/build/bin/${{ matrix.target }}/debug/executable/envdown-cli${{ matrix.ext }} test.envdown)

          if [[ "$OUTPUT" != *"Var: Success"* ]]; then
            echo "::error::Standard interpolation test failed!"
            exit 1
          fi

      - name: Package Archive
        run: |
          cd ${{ matrix.module }}/build/bin/${{ matrix.target }}/debug/executable/
          if [ "${{ matrix.os }}" = "windows-latest" ]; then
            zip -r ../../../../../../../envdown-cli-${{ matrix.target }}.zip envdown-cli.exe
          else
            tar -czvf ../../../../../../../envdown-cli-${{ matrix.target }}.tar.gz envdown-cli
          fi

      - name: Upload to GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: |
            envdown-cli-${{ matrix.target }}.tar.gz
            envdown-cli-${{ matrix.target }}.zip

  update-tap:
    needs: build-and-upload
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Tap Repository
        uses: actions/checkout@v4
        with:
          repository: fnc314/homebrew-tools
          token: ${{ secrets.TAP_GITHUB_TOKEN }}
          path: homebrew-tools

      - name: Calculate Checksums
        id: shasums
        env:
          VERSION: ${{ github.event.release.tag_name }}
        run: |
          CLEAN_VERSION=${VERSION#v}
          echo "version=$CLEAN_VERSION" >> $GITHUB_OUTPUT
          URL_BASE="https://github.com/fnc314/envdown-cli/releases/download/${VERSION}"

          curl -sL -o macos-arm64.tar.gz "$URL_BASE/envdown-cli-macos-arm64.tar.gz"
          curl -sL -o macos-x64.tar.gz "$URL_BASE/envdown-cli-macos-x64.tar.gz"
          curl -sL -o linux-x64.tar.gz "$URL_BASE/envdown-cli-linux-x64.tar.gz"

          echo "mac_arm=$(shasum -a 256 macos-arm64.tar.gz | awk '{print $1}')" >> $GITHUB_OUTPUT
          echo "mac_intel=$(shasum -a 256 macos-x64.tar.gz | awk '{print $1}')" >> $GITHUB_OUTPUT
          echo "linux_intel=$(shasum -a 256 linux-x64.tar.gz | awk '{print $1}')" >> $GITHUB_OUTPUT

      - name: Update Formula
        env:
          VERSION: ${{ steps.shasums.outputs.version }}
          MAC_ARM: ${{ steps.shasums.outputs.mac_arm }}
          MAC_INTEL: ${{ steps.shasums.outputs.mac_intel }}
          LINUX_INTEL: ${{ steps.shasums.outputs.linux_intel }}
        run: |
          python3 -c '
          import os, re
          path = "homebrew-tools/Formula/envdown-cli.rb"
          with open(path, "r") as f: content = f.read()

          content = re.sub(r"version\s+\".*\"", f"version \"{os.environ[\"VERSION\"]}\"", content)
          content = re.sub(r"(on_arm do.*?sha256\s+\").*?(\")", fr"\g<1>{os.environ[\"MAC_ARM\"]}\g<2>", content, flags=re.DOTALL)
          content = re.sub(r"(on_intel do.*?sha256\s+\").*?(\")", fr"\g<1>{os.environ[\"MAC_INTEL\"]}\g<2>", content, count=1, flags=re.DOTALL)
          content = re.sub(r"(on_linux do.*?on_intel do.*?sha256\s+\").*?(\")", fr"\g<1>{os.environ[\"LINUX_INTEL\"]}\g<2>", content, flags=re.DOTALL)

          with open(path, "w") as f: f.write(content)
          '

      - name: Commit Tap Update
        run: |
          cd homebrew-tools
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add Formula/envdown-cli.rb
          git commit -m "Bump envdown-cli to v${{ steps.shasums.outputs.version }}"
          git push
EOF

echo ""