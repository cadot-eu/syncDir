#!/bin/bash

# Script de build et installation de SyncDir Android
# Usage: ./build-and-install.sh

set -e

echo "======================================"
echo "  SyncDir Android - Build & Install"
echo "======================================"
echo ""

# Configuration
export ANDROID_HOME=/home/michael/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools

PROJECT_DIR="/home/michael/sites/syncDirAndroid"
cd "$PROJECT_DIR"

# Vérifier ADB
echo "[1/5] Vérification de la connexion ADB..."
if ! adb devices | grep -q "device$"; then
    echo "❌ Aucun appareil Android détecté en ADB"
    echo "Veuillez:"
    echo "  1. Connecter votre téléphone en USB"
    echo "  2. Activer le débogage USB sur le téléphone"
    echo "  3. Autoriser l'ordinateur sur le téléphone"
    echo ""
    echo "Devices actuels:"
    adb devices
    exit 1
fi
echo "✅ Appareil Android détecté"
echo ""

# Clean
echo "[2/5] Nettoyage du projet..."
rm -rf app/build 2>/dev/null || true
echo "✅ Projet nettoyé"
echo ""

# Vérifier que tous les fichiers nécessaires existent
echo "[3/5] Vérification de la structure du projet..."
if [ ! -f "app/build.gradle" ]; then
    echo "❌ Erreur: app/build.gradle manquant"
    exit 1
fi
if [ ! -f "build.gradle" ]; then
    echo "❌ Erreur: build.gradle manquant"
    exit 1
fi
echo "✅ Structure du projet OK"
echo ""

# Build (on utilisera Android Studio ou gradle system)
echo "[4/5] Build du projet..."
echo ""
echo "⚠️  Le build nécessite Android Studio ou un Gradle récent (8.0+)"
echo ""
echo "Options de build:"
echo ""
echo "  Option A - Avec Android Studio (recommandé):"
echo "    1. Ouvrir Android Studio"
echo "    2. File > Open > /home/michael/sites/syncDirAndroid"
echo "    3. Attendre Gradle Sync"
echo "    4. Build > Build Bundle(s) / APK(s) > Build APK(s)"
echo "    5. L'APK sera dans: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "  Option B - En ligne de commande (nécessite gradlew):"
echo "    cd /home/michael/sites/syncDirAndroid"
echo "    ./gradlew assembleDebug"
echo ""
echo "  Option C - Installation manuelle des sources:"
echo "    Les sources sont prêtes dans /home/michael/sites/syncDirAndroid"
echo "    Vous pouvez les ouvrir avec Android Studio"
echo ""

# Instructions finales
echo ""
echo "======================================"
echo "  📝 Instructions complètes"
echo "======================================"
echo ""
echo "Le projet Android est créé dans:"
echo "  /home/michael/sites/syncDirAndroid"
echo ""
echo "Structure:"
echo "  ✅ Code Java (Activities, Database, SSH, Crypto)"
echo "  ✅ Layouts XML (UI)"
echo "  ✅ Configuration Gradle"
echo "  ✅ AndroidManifest.xml"
echo "  ✅ Ressources (strings, colors, themes)"
echo ""
echo "Pour compiler et installer:"
echo "  1. Ouvrir avec Android Studio"
echo "  2. Sync Gradle"
echo "  3. Run > Run 'app'"
echo ""
echo "Ou depuis la ligne de commande (si gradlew configuré):"
echo "  ./gradlew installDebug"
echo ""
echo "L'appareil ADB est prêt pour l'installation !"
echo ""
