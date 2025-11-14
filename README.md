# 🧠 EduMon — Gamified Study Companion for Students

EduMon is a gamified productivity app designed to help first-year university students stay consistent, organized, and motivated throughout their studies.  
By taking care of a virtual creature that mirrors your study habits, you are encouraged to maintain balance between learning and well-being. 🎮📚

---

## 🌟 Vision

> “Helping BA1 students succeed without burning out.”

EduMon turns daily academic and personal tasks into a rewarding game.  
The more you study and stay consistent, the happier and healthier your EduMon becomes.

---

## 🏗️ Architecture (MVVM)

The app follows a **Model-View-ViewModel (MVVM)** architecture for clean state management and modularity.

- **UI Layer (Jetpack Compose)** — interactive and modern composable screens
- **ViewModel Layer** — handles logic, updates the UI, connects to repositories
- **Data Layer (Firebase, Firestore)** — stores user progress, pet state, and planner data
- **External Services Layer**

## 🚀 Epics completed:

### First epic : Advanced Flashcard System

### Objective
The primary goal of this epic was to develop a robust, persistent, and fully integrated flashcard study system. This work focused on evolving the flashcards from a simple, standalone tool into a core, data-driven component of the student's study ecosystem, enhancing both functionality and user experience.

### Completed Features (Issues)
This epic was completed by implementing the following key tasks across Sprints 4 and 5:

* **`Flashcards: Migrate to DataStore persistence and enhance user's experience` (S4)**
    * **What it is:** This task moved the flashcard data to be stored locally using Android's DataStore.
    * **Why it matters:** It ensures that a user's flashcards are saved persistently on their device. This prevents data loss when the app is closed, allows for offline access, and significantly improves the overall user experience and performance.

* **`Link the flashcard screen to the todo list` (S4)**
    * **What it is:** This feature created a direct connection between the to-do list and the flashcard module.
    * **Why it matters:** This is a crucial integration. It allows a student to create a task like "Review Chapter 5" and link it directly to the relevant flashcard deck. It connects "planning" (the to-do list) with "doing" (the study tool), creating a seamless workflow.

* **`Create a firestore repository for the flashcards feature` (S5)**
    * **What it is:** This task implemented a backend repository using Google's Firestore.
    * **Why it matters:** This is the final step in making the feature robust. By storing flashcards in the cloud (Firestore) instead of just locally (DataStore), the user's data is now backed up, secure, and can be synchronized across multiple devices (e.g., a phone and a tablet) in the future.

### Alignment with Project Goal
By completing this epic, our application now provides a powerful, end-to-end study loop. It directly aligns with our goal of being an all-in-one student companion by:

1.  **Connecting Workflows:** Students no longer have to jump between a "to-do" app and a "flashcard" app.
2.  **Ensuring Data Safety:** Migrating to DataStore and Firestore means the user's hard work (creating flashcards) is never lost.
3.  **Building a Scalable Foundation:** The Firestore backend allows for future "v2" features, such as sharing flashcard decks with friends (part of the "StudyTogether" social system).

---

## 📱Screens

### 🏠 Home Screen
- Displays your EduMon and an overview of your statictics and your edumon statictics
- Overview of your top 3 Todo lists
- Quick action buttons (Focus Mode, Pomodoro timer, etc.)
- Bottom navigation bar (Home, Study session, Calendar, Games, Profile)
- App icon at the top left for quick screen navigation

---

### ✅ Todo / Planner / Calendar
- Add, edit, and delete tasks
- **Planner:** short-term academic tasks (labs, exercises, exams)
- **Calendar:** monthly view showing all tasks, exams and events
- Visual progress indicators for each task

---

### 👤 Profile
- Displays your creature and personal stats
- Basic user information (profile pic, university, level, ...)
- Options to enable notifications, location, and focus mode
- Customize your EduMon’s look and accessories
- Logout button

---

### 📊 Stats & Progress
- Weekly and semester tracking of your study activity
- Visual charts showing your progress toward goals
- Displays study streaks, completed tasks, and consistency level
- Highlights growth of your EduMon alongside your productivity

---

### 🎮 Games
- Small, simple games integrated in the app
- Earn points or rewards that help care for your EduMon
- Fun way to relax while staying connected to your study journey

---

### 📍Location
- Map screen displaying streets, building and campus
- Displays your location with your avatar
- Displays your friends locations with their avatar

---

### 🎟️ Flashcards
- Flashcard menu allowing to create decks
- Flashcards with questions on one side and answer on the other
- Way to learn your key words using the flash cards

## 🛠️ Tech Stack

| Component | Technology |
|------------|-------------|
| **Frontend** | Kotlin + Jetpack Compose |
| **Architecture** | MVVM |
| **Backend** | Firebase (Auth, Firestore) |

| **Authentication** | Google Sign-In |



---

## ✨ Design & Style

- **Glassy and glowing cards** with a semi-transparent look
- Soothing colors and soft animations
- The creature and UI evolve as your semester progresses

---

> 🧩 *EduMon — Study smart, care for your mind, grow your companion.*

An A.I assistant help has been used to generate parts of this file