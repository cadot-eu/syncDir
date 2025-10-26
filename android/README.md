# SyncDir Android

Application Android pour accéder et visualiser les fichiers synchronisés par syncDir.

## 📱 Fonctionnalités

✅ **Multi-utilisateurs** - Gérez plusieurs serveurs en favoris
✅ **Décryptage transparent** - Compatible avec le chiffrement rclone
✅ **Navigation intuitive** - Parcourez vos dossiers chiffrés
✅ **Connexion SSH** - Authentification par clé privée
✅ **Compatible API 21+** - Android 5.0 et supérieur (94% des appareils)

## 🔧 Prérequis

- Android SDK 34
- Gradle 8.0+ (ou utiliser gradlew)
- Java 8+
- Appareil Android avec débogage USB activé

## 📦 Structure du projet

```
syncDirAndroid/
├── app/
│   ├── src/main/
│   │   ├── java/com/syncdir/android/
│   │   │   ├── ui/           # Activities et Adapters
│   │   │   ├── data/         # Room Database (User, DAO)
│   │   │   ├── network/      # SSH/SFTP (JSch)
│   │   │   └── crypto/       # Décryptage rclone
│   │   ├── res/              # Layouts et ressources
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## 🏗️ Build

### Avec Android Studio
1. Ouvrir le projet dans Android Studio
2. Sync Gradle
3. Build > Make Project
4. Run sur l'appareil

### En ligne de commande

```bash
cd /home/michael/sites/syncDirAndroid

# Configurer le SDK
export ANDROID_HOME=/home/michael/Android/Sdk

# Build
./gradlew assembleDebug

# Installer sur appareil connecté
./gradlew installDebug

# Ou directement avec ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📲 Installation et test

### 1. Autoriser le téléphone en ADB

Sur votre téléphone, acceptez l'autorisation de débogage USB.

### 2. Vérifier la connexion

```bash
adb devices
```

Devrait afficher votre appareil avec "device" (pas "unauthorized").

### 3. Build et installer

```bash
cd /home/michael/sites/syncDirAndroid
export ANDROID_HOME=/home/michael/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Build
./gradlew assembleDebug

# Installer
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🎯 Utilisation

### Premier lancement

1. Appuyez sur le bouton **+** pour ajouter un serveur
2. Remplissez les informations :
   - **Nom du serveur** : Un nom de votre choix (ex: "Mon serveur")
   - **Adresse** : IP ou hostname (ex: 192.168.1.100)
   - **Username** : Utilisateur distant (ex: michaelSync)
   - **Mot de passe** : Mot de passe de décryptage rclone
   - **Clé SSH** : Contenu de votre fichier `~/.ssh/id_rsa`

### Accéder aux fichiers

1. Cliquez sur un serveur dans la liste
2. L'application se connecte et liste les dossiers
3. Naviguez dans les dossiers décryptés
4. Cliquez sur un fichier pour le télécharger/visualiser

### Gérer les serveurs

- **Click** : Ouvrir le navigateur de fichiers
- **Long click** : Modifier le serveur
- **Bouton Supprimer** : Supprimer le serveur

## 🔐 Sécurité

- Mots de passe stockés dans Room Database
- Clés SSH stockées localement
- Connexion SSH sécurisée (port 22)
- Décryptage AES-256 compatible rclone

## 🐛 Dépannage

### Erreur de connexion SSH

Vérifiez :
- L'adresse IP/hostname est correcte
- Le serveur SSH est démarré
- Le port 22 est accessible
- La clé SSH est correcte (fichier id_rsa complet)

### Erreur de décryptage

Vérifiez :
- Le mot de passe rclone est correct
- Le fichier est bien chiffré par rclone avec le même mot de passe

### L'application crash au démarrage

```bash
# Voir les logs
adb logcat | grep SyncDir
```

## 📝 TODO / Améliorations

- [ ] Implémenter FileViewerActivity (images, PDF, texte)
- [ ] Ajouter cache local des fichiers téléchargés
- [ ] Support du téléchargement en arrière-plan (WorkManager)
- [ ] Support de l'upload de fichiers
- [ ] Améliorer le décryptage rclone (EME mode)
- [ ] Ajouter recherche de fichiers
- [ ] Support des thèmes sombre/clair
- [ ] Chiffrement de la base de données (SQLCipher)

## 📄 Licence

MIT

## 👤 Auteur

Michael
