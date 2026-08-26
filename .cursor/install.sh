#!/usr/bin/env bash
# Idempotent Cloud Agent bootstrap for the Chisels & Bits Fabric mod.
# Installs the JDK 25 toolchain and headless-GL dependencies required by the
# Fabric client GameTests, then warms the Gradle cache with a full build.
set -euo pipefail

JDK_DIR="/opt/java/jdk-25"
JDK_URL="https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk"

echo "==> Ensuring Temurin JDK 25 is installed"
if [[ ! -x "${JDK_DIR}/bin/java" ]]; then
	tmp="$(mktemp -d)"
	curl -fsSL -o "${tmp}/jdk25.tar.gz" "${JDK_URL}"
	sudo mkdir -p /opt/java
	sudo tar xzf "${tmp}/jdk25.tar.gz" -C /opt/java
	extracted="$(sudo find /opt/java -maxdepth 1 -type d -name 'jdk-25*' | head -1)"
	sudo ln -sfn "${extracted}" "${JDK_DIR}"
	rm -rf "${tmp}"
fi

export JAVA_HOME="${JDK_DIR}"
export PATH="${JAVA_HOME}/bin:${PATH}"
java -version

echo "==> Ensuring headless X / OpenGL dependencies for client GameTests"
if ! command -v xvfb-run >/dev/null 2>&1; then
	sudo apt-get update -qq
	sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
		xvfb libgl1-mesa-dri libglu1-mesa xauth
fi

echo "==> Persisting JAVA_HOME for interactive shells"
if ! grep -q 'JAVA_HOME=/opt/java/jdk-25' "${HOME}/.bashrc" 2>/dev/null; then
	{
		echo 'export JAVA_HOME=/opt/java/jdk-25'
		echo 'export PATH="$JAVA_HOME/bin:$PATH"'
	} >>"${HOME}/.bashrc"
fi

echo "==> Warming the Gradle cache (downloads Minecraft 26.2 + dependencies)"
./gradlew build --console=plain

echo "==> Environment ready"
