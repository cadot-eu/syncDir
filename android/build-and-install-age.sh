#!/bin/bash

# Script pour builder et installer l'app Android avec support age

set -e

echo "======================================"
echo "Build SyncDir Android (version age)"
echo "======================================"

cd /home/michael/sites/syncDirAndroid

# Vérifier Android SDK
if [ -z "$ANDROID_HOME" ]; then
    export ANDROID_HOME=/home/michael/Android/Sdk
fi

export PATH=$PATH:$ANDROID_HOME/platform-tools

echo "✓ Android SDK: $ANDROID_HOME"

# Vérifier téléphone connecté
echo ""
echo "Vérification connexion ADB..."
adb devices

device_count=$(adb devices | grep -v "List" | grep "device$" | wc -l)

if [ $device_count -eq 0 ]; then
    echo "❌ Aucun appareil connecté"
    echo "Branchez votre téléphone en USB et activez le débogage USB"
    exit 1
fi

echo "✓ Appareil connecté"

# Clean
echo ""
echo "Nettoyage..."
./gradlew clean

# Build
echo ""
echo "Build de l'APK..."
./gradlew assembleDebug

# Installer
echo ""
echo "Installation sur l'appareil..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo ""
echo "======================================"
echo "✓ Installation réussie!"
echo "======================================"
echo ""
echo "Vous pouvez maintenant:"
echo "1. Lancer l'app SyncDir sur votre téléphone"
echo "2. Ajouter un serveur avec les infos du script CLI"
echo "3. Tester la navigation et le téléchargement de fichiers"
echo ""
echo "Logs en temps réel:"
echo "  adb logcat | grep -E 'SyncDir|AgeCrypto'"
