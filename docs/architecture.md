# 🏗️ EduMon — Application Architecture

## 🎯 Overview

**EduMon**  is a gamified productivity app designed to help first-year students balance their study habits, well-being, and motivation by taking care of a virtual companion that evolves with their academic progress.

The app integrates Firebase for backend services, AI-based recommendations via Cloud Functions, and Jetpack Compose on Android for a modern, reactive UI.

---

## 🧩 High-Level Architecture

```mermaid
flowchart LR
subgraph User["👤 BA1 Student"]
UX["Android App (Jetpack Compose)"]
end

    UX -->|Google Sign-In| AUTH[(Firebase Auth)]
    UX <-->|Realtime + Sync| FS[(Firebase Firestore)]
    UX -->|Media (pet assets, avatars)| ST[(Firebase Storage)]
    UX -->|Trigger / Heavy compute (AI)| CF[(Cloud Functions)]
    CF -->|AI Study Plan, Insights| FS

    UX <-->|Feature flags| RC[(Remote Config)]
    FCM[(Firebase Cloud Messaging)] --> UX
    GA[(Firebase Analytics)] --> FS
    GA --> CF

    subgraph Device["📱 On-device"]
      VM["ViewModel + UseCases (MVVM)"]
      ROOM[("Room (Offline cache)\nFlashcards, StudyLogs, Local Points")]
      PREF[("DataStore/Prefs\n(reminders, focus settings)")]
      WORK["WorkManager\n(offline sync, reminders)"]
    end

    UX --> VM
    VM --> ROOM
    VM --> PREF
    VM --> FS
    WORK --> FS

    UX -.->|Location + Focus Mode| GPS[("Android GPS + Sensors")]
    UX -.->|Notifications| NOTIF[("Local Notifications")]

style UX fill:#14163f,stroke:#7c3aed,stroke-width:2px,color:#fff
style FS fill:#ffcc00,stroke:#ff9900,stroke-width:1px
style AUTH fill:#ff6666,stroke:#cc0000,stroke-width:1px
style ST fill:#00bcd4,stroke:#0097a7,stroke-width:1px
style CF fill:#7c3aed,stroke:#5b21b6,stroke-width:1px,color:#fff
style RC fill:#66bb6a,stroke:#388e3c,stroke-width:1px,color:#fff
style ROOM fill:#2e2f5b,stroke:#7c3aed,stroke-width:1px,color:#fff
style PREF fill:#2e2f5b,stroke:#7c3aed,stroke-width:1px,color:#fff
style WORK fill:#2e2f5b,stroke:#7c3aed,stroke-width:1px,color:#fff

## 📡 Data Flow

Ce diagramme montre comment les interactions utilisateur traversent les couches MVVM,
du composant UI jusqu’à Firebase et les caches locaux :

```mermaid
sequenceDiagram
    participant U as User (Student)
    participant UI as Compose UI
    participant VM as ViewModel
    participant REPO as Repository
    participant FB as Firebase (Firestore/Auth)
    participant LOCAL as Room/DataStore

    U->>UI: Interacts with pet / logs study
    UI->>VM: Emits user actions (study, streak, quiz)
    VM->>REPO: Updates user data
    REPO->>FB: Syncs with Firestore
    REPO->>LOCAL: Saves offline copy
    FB-->>VM: Sends updates (pet evolution, stats)
    LOCAL-->>VM: Returns cached data if offline
    VM-->>UI: Updates pet, stats, and notifications
