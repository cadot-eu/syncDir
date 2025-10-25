#!/bin/bash
# Script d'export de configuration syncDir pour Android

set -e

CONFIG_FILE="$HOME/.syncdir.conf"
SSH_KEY_FILE="$HOME/.ssh/id_rsa"
OUTPUT_FILE="$HOME/syncdir-android-config.json"

echo "=========================================="
echo "  Export Configuration SyncDir Android"
echo "=========================================="
echo ""

# Vérifier les fichiers
if [ ! -f "$CONFIG_FILE" ]; then
    echo "❌ Fichier de configuration introuvable: $CONFIG_FILE"
    exit 1
fi

if [ ! -f "$SSH_KEY_FILE" ]; then
    echo "⚠️  Clé SSH RSA introuvable: $SSH_KEY_FILE"
    exit 1
fi

# Charger la config
source "$CONFIG_FILE"

# Lire la clé SSH et l'échapper correctement en JSON
SSH_KEY=$(cat "$SSH_KEY_FILE" | jq -Rs .)

# Décoder le mot de passe obscurci par rclone
DECODED_PASSWORD=$(rclone reveal "$CRYPT_PASSWORD")

# Déterminer le nom du répertoire chiffré sur le serveur
# En se connectant via SSH et en listant /home/{user}Sync/encrypted/
echo "Récupération du nom de répertoire chiffré sur le serveur..."
ENCRYPTED_DIR=$(ssh "${REMOTE_ROOT}@${REMOTE_HOST}" "ls /home/$(whoami)Sync/encrypted/ 2>/dev/null | head -1")

if [ -z "$ENCRYPTED_DIR" ]; then
    echo "⚠️  Aucun répertoire chiffré trouvé, utilisation de 'cloud' par défaut"
    ENCRYPTED_DIR="cloud"
else
    echo "✓ Répertoire chiffré trouvé: $ENCRYPTED_DIR"
fi

# Créer le JSON avec le bon utilisateur (michaelSync) et le mot de passe décodé
cat > "$OUTPUT_FILE" << JSONEOF
{
  "version": "1.0",
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
      "password": "$DECODED_PASSWORD"
    }
  ]
}
JSONEOF

# Valider le JSON
if jq . "$OUTPUT_FILE" > /dev/null 2>&1; then
    echo "✅ Configuration exportée vers: $OUTPUT_FILE"
else
    echo "❌ Erreur: JSON invalide généré"
    exit 1
fi

echo ""
echo "📱 Pour importer dans l'application Android:"
echo "   1. Transférez le fichier sur votre téléphone:"
echo "      adb push $OUTPUT_FILE /sdcard/Download/syncdir_config.json"
echo ""
echo "   2. Dans l'app, utilisez le menu (3 points) > \"Importer JSON\""
echo ""
echo "🔒 ATTENTION: Ce fichier contient des données sensibles!"
echo "   - Clé SSH privée"
echo "   - Mot de passe de chiffrement EN CLAIR"
echo "   Supprimez-le après import!"
echo ""
echo "Fichier généré avec succès!"
