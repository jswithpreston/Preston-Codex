#!/usr/bin/env bash
set -e
SDK="$HOME/android-sdk"
mkdir -p "$SDK/cmdline-tools"
cd /tmp
curl -fsSL -o cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q -o cmdtools.zip -d "$SDK/cmdline-tools"
mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null
"$SDK/cmdline-tools/latest/bin/sdkmanager" "platform-tools"
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> "$HOME/.bashrc"
