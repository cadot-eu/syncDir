# syncDir - Synchronisation SSH Bidirectionnelle Chiffrée

Script bash pour synchroniser automatiquement des répertoires locaux avec un serveur distant via SSH, avec chiffrement transparent des données.

## 🚀 Fonctionnalités

- ✅ **Synchronisation bidirectionnelle** rapide avec rclone bisync
- 🔒 **Chiffrement transparent** des données avant envoi sur le serveur distant
- 🔄 **Synchronisation incrémentale** - ne transfère que les modifications
- 🛡️ **Sécurisé** - Authentification par clés SSH, pas de mots de passe en clair
- ⚡ **Rapide** - Détection intelligente des changements avec cache local
- 🔧 **Facile** - Configuration interactive au premier lancement
- 📊 **Logs détaillés** - Suivi complet de toutes les opérations
- 🔁 **Reprise automatique** - Gestion des synchronisations interrompues
- 🗂️ **Multi-répertoires** - Synchronisez plusieurs répertoires indépendamment

## 📋 Prérequis

- **Linux** (Ubuntu, Debian, etc.)
- **Bash** 4.0+
- **Accès SSH root** au serveur distant (pour la configuration initiale)
- **rclone** (installé automatiquement par le script si absent)

## 📦 Installation

```bash
# Cloner le projet
git clone https://github.com/votre-username/syncDir.git
cd syncDir

# Rendre le script exécutable
chmod +x syncDir

# Copier dans votre PATH (optionnel)
sudo cp syncDir /usr/local/bin/
```

## ⚙️ Configuration

Au premier lancement, le script vous demandera :

1. **Adresse IP ou nom d'hôte** du serveur distant
2. **Utilisateur root** distant (défaut: root)
3. **Répertoire de base** distant (défaut: /home)
4. **Mot de passe de chiffrement** pour protéger vos données

La configuration est sauvegardée dans `~/.syncdir.conf` (protégé en chmod 600).

## 🎯 Usage

### Synchronisation simple

```bash
# Synchroniser le répertoire ~/Documents/projet
./syncDir Documents/projet

# Ou avec un chemin absolu
./syncDir /home/michael/Documents/projet
```

### Modes avancés

```bash
# Mode MAITRE - Priorité au local en cas de conflit
./syncDir Documents/projet --maitre

# Mode RESET - Effacer et resynchroniser complètement
./syncDir Documents/projet --reset

# Mode DELETE DISTANT - Supprimer toutes les données distantes
./syncDir Documents/projet --deleteDistant

# Ou sans spécifier de répertoire
./syncDir --deleteDistant
```

### Exemples concrets

```bash
# Synchroniser votre dossier cloud
./syncDir cloud

# Synchroniser vos photos
./syncDir Photos

# Synchroniser un projet spécifique
./syncDir git/mon-projet
```

## 🔄 Automatisation avec Cron

Pour synchroniser automatiquement :

```bash
crontab -e
```

Ajoutez :

```cron
# Synchronisation toutes les 15 minutes
*/15 * * * * /home/michael/syncDir cloud >> ~/.local/log/syncDir/cron.log 2>&1

# Synchronisation toutes les heures en mode maitre
0 * * * * /home/michael/syncDir Documents --maitre >> ~/.local/log/syncDir/cron.log 2>&1
```

## 🔐 Sécurité

### Ce qui est chiffré
- ✅ Tous les fichiers sont chiffrés avec rclone crypt avant envoi
- ✅ Les noms de fichiers sont chiffrés
- ✅ Les noms de répertoires sont chiffrés
- ✅ Chiffrement AES-256

### Ce qui est stocké localement (NON commité)
- `~/.syncdir.conf` - Configuration (IP serveur, mots de passe obscurcis)
- `~/.config/rclone/rclone.conf` - Configuration rclone
- `~/.ssh/id_rsa` - Clé SSH privée
- `~/.cache/rclone/bisync/` - Cache de synchronisation

**⚠️ Ne JAMAIS commiter ces fichiers !**

## 📁 Structure du projet

```
syncDir/
├── syncDir              # Script principal
├── README.md           # Documentation
├── LICENSE             # Licence
├── .gitignore          # Fichiers à ignorer
└── examples/           # Exemples de configuration
```

## 🐛 Dépannage

### La synchronisation est lente

Le script utilise rclone bisync qui est très rapide. Si c'est lent :
- Première synchro = initialisation (normal)
- Synchros suivantes = quelques secondes

### Erreur "unknown flag"

Votre version de rclone est trop ancienne. Le script met automatiquement à jour rclone vers la dernière version.

### Erreur de connexion SSH

```bash
# Vérifier la connexion
ssh root@votre-serveur

# Régénérer les clés si nécessaire
rm ~/.ssh/id_rsa*
./syncDir cloud
```

### Réinitialiser complètement

```bash
# Supprimer la configuration locale
rm ~/.syncdir.conf
rm -rf ~/.config/rclone/
rm -rf ~/.cache/rclone/bisync/

# Supprimer les données distantes
./syncDir --deleteDistant

# Recommencer
./syncDir cloud
```

## 📊 Logs

Les logs sont stockés dans `~/.local/log/syncDir/syncDir.log`

```bash
# Voir les logs en temps réel
tail -f ~/.local/log/syncDir/syncDir.log

# Rechercher les erreurs
grep ERROR ~/.local/log/syncDir/syncDir.log

# Logs d'un répertoire spécifique
grep "cloud" ~/.local/log/syncDir/syncDir.log
```

## 🤝 Contribution

Les contributions sont les bienvenues !

1. Fork le projet
2. Créez une branche (`git checkout -b feature/amelioration`)
3. Committez vos changements (`git commit -am 'Ajout fonctionnalité'`)
4. Push vers la branche (`git push origin feature/amelioration`)
5. Ouvrez une Pull Request

## 📄 Licence

MIT License - voir le fichier [LICENSE](LICENSE)

## 👤 Auteur

**Michael**

## 🙏 Remerciements

- [rclone](https://rclone.org/) - Outil de synchronisation cloud
- Communauté open source

## 📝 Notes de version

### v1.0.0 (2025-10-25)
- ✨ Synchronisation bidirectionnelle avec rclone bisync
- 🔒 Chiffrement transparent des données
- ⚡ Optimisations de performance
- 🛠️ Mise à jour automatique de rclone
- 📖 Documentation complète
# syncDir
