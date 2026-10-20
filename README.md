# مدرستي — Madrassati

> تطبيق تعليمي تفاعلي للمعلم والتلميذ، يعمل دون إنترنت داخل الفصل.
> Application éducationnelle interactive enseignant/élève, fonctionne hors-ligne en classe.

## À propos

**Madrassati** est une application Android pour la salle de classe : l'enseignant gère le contenu et les élèves, les élèves suivent des leçons guidées et des exercices interactifs — le tout **sans Internet**, via un réseau local autonome créé par l'appareil de l'enseignant.

## Fonctionnalités

- **Deux modes** : Enseignant (gestion, contenu, réseau) et Élève (leçons guidées, exercices, scores)
- **3 matières** : العربية (arabe)، الرياضيات (maths)، التربية العلمية والتكنولوجية (sciences) — contenu officiel + exercices personnalisés par l'enseignant
- **Types d'exercices** : leçon théorique, QCM, vrai/faux, appariement, tri, réponse écrite — avec niveaux de difficulté (سهل / متوسط / متقدم)
- **Réseau de classe autonome** : crée son propre point d'accès Wi-Fi ou rejoint le Wi-Fi existant — aucun Internet requis
- **Contrôle à distance** : verrouiller/déverrouiller les appareils élèves, lancer une leçon sur tous les écrans, diffuser image/vidéo
- **Mode Kiosk (Device Owner)** : l'élève ne peut pas quitter l'application ; sortie protégée par code PIN enseignant
- **Suivi des résultats** : score, maîtrise, temps et points de difficulté par élève
- **Compte enseignant sécurisé** : connexion, récupération par PIN, modification du mot de passe
- **Hors-ligne 100 %** : base de données locale (Room), aucune donnée personnelle envoyée sur Internet

## Pile technique

| Couche | Technologies |
|---|---|
| Langage | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | Clean Architecture, MVVM |
| DI | Hilt |
| Données | Room, DataStore |
| Réseau local | Ktor (serveur embarqué), mDNS |
| Sécurité | Device Owner / Kiosk, PIN |
| Build | Gradle Kotlin DSL, version catalog |
| Cibles | Android 11+ (API 30), arm64-v8a & armeabi-v7a, tablettes recommandées (≥ 600 dp) |

## Installation

```bash
git clone https://github.com/Chouaib23x/Madrassati.git
cd Madrassati
./gradlew assembleDebug
```

L'APK généré se trouve dans `app/build/outputs/apk/debug/`.

Ou installer directement via ADB :

```bash
adb install -r app/build/outputs/apk/debug/MADRASATI-debug.apk
```

## Scripts utiles

| Script | Rôle |
|---|---|
| `scripts/install-debug.bat` | Installe l'APK debug via ADB |
| `scripts/set-device-owner.bat` | Définit l'app comme Device Owner (mode Kiosk) |
| `scripts/clear-device-owner.bat` | Révoque le Device Owner |

> **Note Kiosk** : le mode verrouillage nécessite que l'app soit définie comme *Device Owner* — l'appareil ne doit pas avoir de compte Google configuré, ou l'installation doit se faire via ADB avec `dpm set-device-owner`.

## Structure du projet

```
app/src/main/java/dz/madrassati/
├── core/          # Kiosk, réseau, thème, session
├── data/          # Room, repositories, seed de contenu
├── di/            # Modules Hilt
├── domain/        # Modèles, cas d'usage, scoring, runner de leçon
└── ui/            # Écrans Compose (auth, teacher, student, kiosk…)
```

## Licence

Projet privé — tous droits réservés.
