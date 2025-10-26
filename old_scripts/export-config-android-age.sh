#!/bin/bash
# Script d'export de configuration syncDir (age) pour Android

set -e

CONFIG_FILE="$HOME/.syncdir_age.conf"
SSH_KEY_FILE="$HOME/.ssh/id_rsa"
AGE_KEY_FILE="$HOME/.config/syncdir/age_key.txt"
OUTPUT_FILE="$HOME/syncdir-android-config.json"

echo "=========================================="
echo "  Export Configuration SyncDir (age)"
echo "=========================================="
echo ""

# Vérifier les fichiers
if [ ! -f "$CONFIG_FILE" ]; then
    echo "❌ Fichier de configuration introuvable: $CONFIG_FILE"
    exit 1
fi

if [ ! -f "$SSH_KEY_FILE" ]; then
    echo "❌ Clé SSH introuvable: $SSH_KEY_FILE"
    exit 1
fi

if [ ! -f "$AGE_KEY_FILE" ]; then
    echo "❌ Clé age introuvable: $AGE_KEY_FILE"
    exit 1
fi

# Charger la config
source "$CONFIG_FILE"

# Lire la clé SSH et l'échapper en JSON
SSH_KEY=$(cat "$SSH_KEY_FILE" | jq -Rs .)

# Extraire la clé publique age
AGE_PUBLIC_KEY=$(grep "# public key:" "$AGE_KEY_FILE" | awk '{print $4}')

echo "✓ Configuration chargée"
echo "✓ Clé publique age: $AGE_PUBLIC_KEY"

# Déterminer le répertoire chiffré sur le serveur
echo ""
echo "Récupération des répertoires sur le serveur..."
ENCRYPTED_DIRS=$(ssh "${REMOTE_ROOT}@${REMOTE_HOST}" "ls /home/$(whoami)Sync/encrypted/ 2>/dev/null" || echo "")

if [ -z "$ENCRYPTED_DIRS" ]; then
    echo "⚠️  Aucun répertoire trouvé, utilisation de 'test_age' par défaut"
    ENCRYPTED_DIR="test_age"
else
    # Prendre le premier répertoire
    ENCRYPTED_DIR=$(echo "$ENCRYPTED_DIRS" | head -1)
    echo "✓ Répertoire trouvé: $ENCRYPTED_DIR"
fi

# Créer le JSON avec la clé publique age
cat > "$OUTPUT_FILE" << JSONEOF
{
  "version": "2.0-age",
  "exported_at": "$(date -Iseconds)",
  "server": {
    "name": "$REMOTE_HOST",
    "hostname": "$REMOTE_HOST",
    "port": 22,
    "ssh_key": ${SSH_KEY}
  },
  "users": [
    {
      "name": "$(whoami) - Main",
      "username": "$(whoami)Sync",
      "remote_directory": "$ENCRYPTED_DIR",
      "password": "$AGE_PUBLIC_KEY"
    }
  ]
}
JSONEOF

# Valider le JSON
if jq . "$OUTPUT_FILE" > /dev/null 2>&1; then
    echo ""
    echo "✅ Configuration exportée vers: $OUTPUT_FILE"
else
    echo "❌ Erreur: JSON invalide"
    exit 1
fi

# Envoyer sur le téléphone
echo ""
echo "📱 Envoi sur le téléphone..."
if adb push "$OUTPUT_FILE" /sdcard/Download/syncdir_config.json 2>&1; then
    echo "✅ Fichier envoyé sur le téléphone!"
    echo ""
    echo "📲 Dans l'app SyncDir:"
    echo "   1. Menu (⋮) → Importer configuration"
    echo "   2. Sélectionner 'syncdir_config.json' depuis Downloads"
    echo ""
else
    echo ""
    echo "⚠️  Transfert ADB échoué. Transférez manuellement:"
    echo "   adb push $OUTPUT_FILE /sdcard/Download/syncdir_config.json"
    echo ""
fi

echo "🔒 ATTENTION: Ce fichier contient:"
echo "   - Clé SSH privée"
echo "   - Clé publique age"
echo "   Supprimez-le du téléphone après import!"
echo ""
