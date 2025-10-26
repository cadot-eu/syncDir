#!/bin/bash
# Export configuration pour Android (OpenSSL AES)

set -e

CONFIG_FILE="$HOME/.syncdir.conf"
SSH_KEY_FILE="$HOME/.ssh/id_rsa"
OUTPUT_FILE="$HOME/syncdir-android-config.json"

echo "=========================================="
echo "  Export Configuration SyncDir (OpenSSL)"
echo "=========================================="
echo ""

if [ ! -f "$CONFIG_FILE" ]; then
    echo "❌ Configuration introuvable: $CONFIG_FILE"
    exit 1
fi

if [ ! -f "$SSH_KEY_FILE" ]; then
    echo "❌ Clé SSH introuvable: $SSH_KEY_FILE"
    exit 1
fi

source "$CONFIG_FILE"

# Lire clé SSH
SSH_KEY=$(cat "$SSH_KEY_FILE" | jq -Rs .)

echo "✓ Configuration chargée"
echo "✓ Mot de passe: $CRYPT_PASSWORD"

# Récupérer répertoires sur serveur
echo ""
echo "Récupération répertoires..."
ENCRYPTED_DIRS=$(ssh "${REMOTE_ROOT}@${REMOTE_HOST}" "ls -1 /home/$(whoami)Sync/encrypted/ 2>/dev/null" || echo "")

if [ -z "$ENCRYPTED_DIRS" ]; then
    echo "⚠️  Aucun répertoire trouvé"
    ENCRYPTED_DIRS="domo"
fi

# Compter et afficher
DIR_COUNT=$(echo "$ENCRYPTED_DIRS" | wc -l)
echo "✓ Répertoires trouvés: $DIR_COUNT"
echo "$ENCRYPTED_DIRS" | sed 's/^/  - /'

# Créer JSON avec jq directement
cat > "${OUTPUT_FILE}.tmp" << 'JSONEOF'
{
  "version": "3.0-openssl",
  "server": {
    "name": "",
    "hostname": "",
    "port": 22
  },
  "users": []
}
JSONEOF

# Construire JSON avec jq
jq -n \
  --arg version "3.0-openssl" \
  --arg exported_at "$(date -Iseconds)" \
  --arg hostname "$REMOTE_HOST" \
  --arg ssh_key "$(cat "$SSH_KEY_FILE")" \
  --arg username "$(whoami)Sync" \
  --arg password "$CRYPT_PASSWORD" \
  '{
    version: $version,
    exported_at: $exported_at,
    server: {
      name: $hostname,
      hostname: $hostname,
      port: 22,
      ssh_key: $ssh_key
    },
    users: []
  }' > "${OUTPUT_FILE}.tmp"

# Ajouter les utilisateurs un par un avec jq
while IFS= read -r dir; do
    [ -z "$dir" ] && continue
    
    jq --arg name "$(whoami) - $dir" \
       --arg username "$(whoami)Sync" \
       --arg remote_dir "$dir" \
       --arg password "$CRYPT_PASSWORD" \
       '.users += [{
         name: $name,
         username: $username,
         remote_directory: $remote_dir,
         password: $password
       }]' "${OUTPUT_FILE}.tmp" > "${OUTPUT_FILE}.tmp2"
    mv "${OUTPUT_FILE}.tmp2" "${OUTPUT_FILE}.tmp"
done <<< "$ENCRYPTED_DIRS"

mv "${OUTPUT_FILE}.tmp" "$OUTPUT_FILE"

# Valider JSON
if jq . "$OUTPUT_FILE" > /dev/null 2>&1; then
    echo ""
    echo "✅ Configuration exportée: $OUTPUT_FILE"
else
    echo "❌ JSON invalide"
    exit 1
fi

# Envoyer sur téléphone
echo ""
echo "📱 Envoi sur téléphone..."
if adb push "$OUTPUT_FILE" /sdcard/Download/syncdir_config.json 2>&1; then
    echo "✅ Fichier envoyé!"
    echo ""
    echo "📲 Dans l'app:"
    echo "   1. Menu (⋮) → Importer configuration"
    echo "   2. Sélectionner syncdir_config.json"
    echo ""
else
    echo ""
    echo "⚠️  Transfert ADB échoué. Transférez manuellement:"
    echo "   adb push $OUTPUT_FILE /sdcard/Download/syncdir_config.json"
    echo ""
fi

echo "🔒 ATTENTION: Ce fichier contient:"
echo "   - Clé SSH privée"
echo "   - Mot de passe chiffrement"
echo "   Supprimez-le après import!"
