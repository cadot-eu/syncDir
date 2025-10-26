# syncDir

Synchronisation bidirectionnelle chiffrée de répertoires via SSH.

## Fonctionnalités

- Synchronisation bidirectionnelle rapide (rclone bisync)
- Chiffrement transparent AES-256 (noms de fichiers inclus)
- Accès isolé par répertoire avec utilisateurs SSH dédiés
- Partage sécurisé avec fichiers `.syn` chiffrés
- Application Android pour accès mobile

## Prérequis

- Linux (Ubuntu, Debian, etc.)
- Accès SSH root au serveur distant
- rclone (installé automatiquement si absent)

## Installation

```bash
git clone https://github.com/cadot-eu/syncDir.git
cd syncDir
chmod +x syncDir syncDir-share update
./update
```

Le script `update` installe `syncDir` et `syncDir-share` dans `/usr/local/bin/`.

## Configuration

Au premier lancement :

```bash
syncDir <nom-repertoire>
```

Le script demande :
1. Adresse IP ou hostname du serveur distant
2. Utilisateur root distant (défaut: root)
3. Répertoire de base distant (défaut: /home)
4. Mot de passe de chiffrement

Configuration sauvegardée dans `~/.syncdir.conf` (chmod 600).

## Usage

### Synchronisation

```bash
syncDir <repertoire>           # Synchronisation bidirectionnelle
syncDir <repertoire> --maitre  # Priorité au local en cas de conflit
syncDir <repertoire> --reset   # Resynchronisation complète
```

### Partage sécurisé

```bash
syncDir-share <repertoire>
```

Crée un fichier `<repertoire>.syn` dans le répertoire courant contenant :
- Clé SSH dédiée pour ce répertoire
- Identifiants de connexion (utilisateur SSH isolé)
- Configuration chiffrée

Partager ce fichier via WhatsApp, email, etc. + communiquer le mot de passe séparément.

### Multi-utilisateurs

```bash
# Utilisateur 1 (maître)
syncDir dossier --maitre

# Utilisateur 2 (accès partagé)
mkdir ~/dossier
syncDir dossier --user utilisateur1
```

## Automatisation

```bash
crontab -e
```

```cron
*/15 * * * * syncDir cloud >> ~/syncDir_log/cron.log 2>&1
```

## Sécurité

### Chiffrement
- Fichiers : AES-256 (rclone crypt)
- Noms de fichiers et répertoires : chiffrés
- Transport : SSH

### Isolation par répertoire
Chaque répertoire partagé utilise :
- Un utilisateur SSH dédié (ex: `michaelSync_domo`)
- Une clé SSH unique
- Accès limité à ce répertoire uniquement (pas d'accès aux autres répertoires)

### Fichiers locaux sensibles
- `~/.syncdir.conf` - Configuration
- `~/.config/rclone/rclone.conf` - Config rclone
- `~/.ssh/syncdir/` - Clés SSH par répertoire
- `~/.cache/rclone/bisync/` - Cache

**Ne jamais commiter ces fichiers.**

## Application Android

L'app Android permet :
- Import de fichiers `.syn` (ajout automatique du répertoire partagé)
- Navigation dans les fichiers distants
- Téléchargement de fichiers
- Gestion de plusieurs répertoires partagés

### Installation

Télécharger l'APK depuis `android/releases/syncdir-v1.0.0.apk` et installer sur votre téléphone.

### Développement

```bash
cd android

# Build debug
./build-debug

# Build production
./build-prod
```

Code source : `android/`

## Dépannage

### Réinitialiser

```bash
rm ~/.syncdir.conf
rm -rf ~/.config/rclone/
rm -rf ~/.cache/rclone/bisync/
syncDir --deleteDistant
```

## Logs

```bash
tail -f ~/.local/log/syncDir/syncDir.log
grep ERROR ~/.local/log/syncDir/syncDir.log
```

## Licence

MIT License - voir [LICENSE](LICENSE)
