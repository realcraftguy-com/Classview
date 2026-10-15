#!/usr/bin/env bash
# Baut die Tailscale-Brücke als Android-Bibliothek: android/app-host/libs/tsbridge.aar
# Voraussetzungen: Go (lädt die in go.mod verlangte Version selbst), Android SDK + NDK (ANDROID_HOME / ANDROID_NDK_HOME).
set -euo pipefail
cd "$(dirname "$0")"
export GOTOOLCHAIN=auto
MOBILE_VERSION=$(go list -m -f '{{.Version}}' golang.org/x/mobile)
go install "golang.org/x/mobile/cmd/gomobile@${MOBILE_VERSION}" "golang.org/x/mobile/cmd/gobind@${MOBILE_VERSION}"
export PATH="$PATH:$(go env GOPATH)/bin"
gomobile init
mkdir -p ../app-host/libs
gomobile bind -target=android/arm64 -androidapi 29 -javapkg=com.realcraftguy.classview \
  -trimpath -ldflags="-s -w" -o ../app-host/libs/tsbridge.aar .
echo "Fertig: android/app-host/libs/tsbridge.aar"
