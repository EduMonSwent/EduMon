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


### Second Epic : Social Study Features (Study Together)


### Objective
Build a social accountability system that allows students to see their friends' study activity and location, fostering motivation through community and turning studying from an isolated activity into a shared experience.

### Completed Features (Issues)

* **`Social Presence & Location Sharing (Prototype)` (S4)**
    * **What it is:** Initial prototype allowing students to share their study status (studying, break, idle) and general location with friends.
    * **Why it matters:** This validates the core concept - can social accountability actually motivate students? The prototype tested whether students would engage with this feature and demonstrated the technical feasibility of real-time status sharing.

* **`StudyTogether v1 (Social System)` (S5)**
    * **What it is:** Full implementation of the social study map, showing friends' avatars on a campus map with real-time status updates powered by Firestore listeners.
    * **Why it matters:** This brings the feature to life with a visual, engaging interface. Students can literally see who's studying, creating a virtual study hall atmosphere even when physically apart. It transforms studying from a solitary activity into a shared experience.

* **`On campus detection` (S6)**
    * **What it is:** Geofencing logic that detects when a student is on campus using GPS coordinates and displays a visual indicator.
    * **Why it matters:** Context matters for motivation. Seeing friends studying on campus when you're also there creates stronger accountability than seeing remote studiers. This feature makes the social element more relevant and encourages spontaneous in-person study sessions.

* **`Real-time friend activity notifications` (S9)**
    * **What it is:** Optional push notifications alerting students when their friends start studying, creating timely social nudges.
    * **Why it matters:** Social features only work if users know when their friends are active. These notifications leverage FOMO (fear of missing out) and social proof to encourage study sessions - "If my friend is studying now, maybe I should too."
 
* **`Enhance Study Together UI` (S10)**
    * **What it is:** UI polish including better EduMon avatars on the map, clearer status indicators (green for studying, yellow for break, gray for idle), and smoother map interactions.
    * **Why it matters:** Social features live or die on engagement. A polished, delightful interface encourages students to check in on friends regularly, creating the network effects that make the feature valuable. Poor UI would turn this from a motivational tool into a chore.

### Alignment with Project Goal
By completing this epic, our application transforms from a solo productivity tool into a social study platform. It directly aligns with our goal of being an all-in-one student companion by:

1. **Addressing Isolation:** Many students struggle with motivation when studying alone. Study Together creates a sense of community and shared purpose, making studying feel less lonely.

2. **Creating Accountability:** When friends can see your study status, there's positive peer pressure to actually study. This accountability loop helps students follow through on their intentions.

3. **Building Network Effects:** The more friends a student has on EduMon, the more valuable the app becomes. This creates natural growth through word-of-mouth as students invite friends to join them.

4. **Enabling Spontaneous Collaboration:** Campus detection enables real-world meetups. Students can see "Oh, Sarah's studying in the library right now" and join her, transforming the app from purely digital to facilitating real connections.

5. **Laying Foundation for Future Features:** The social infrastructure (friend system, location sharing, real-time sync) enables future features like shared flashcard decks, group study challenges, or collaborative note-taking.

---

## 📱Screens

### 🏠 Home Screen

- Displays your **EduMon** with its current **level** and environment
- Overview of **your stats**: streak, points, study today, weekly goal
- **Affirmation card** with a motivational quote
- Quick access to **Open Schedule** and **Daily Reflection**
- **To-dos preview** (up to 3 pending, with empty state)
- **Quick Actions**: Study 30m, Take Break, Flashcards, Social Time
- **Bottom navigation bar**: Home, Study, Calendar, Games, Profile
- **Top-left menu icon** for fast screen navigation

---

### ✅ Todo / Planner / Calendar
- Add, edit, and delete tasks
- **Planner:** short-term academic tasks (labs, exercises, exams)
- **Calendar:** monthly view showing all tasks, exams and events
- Visual progress indicators for each task

---

### 👤 Profile
- Shows your EduMon, level, and progress bars
- User info: avatar, name, email, university (EPFL), level & points
- Progress toward next level
- Personal stats: streak, points, coins, study time, daily goal
- Access to settings and logout


---

### 📊 Stats & Progress
- Weekly overview of your study activity
- Key metrics: **total study time**, **completed goals**, **weekly goal**
- Breakdown of **time per subject** (current week)
- **7-day progression chart** to visualize daily study effort

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
- Always shows your campus status chip (On/Off campus) with an EPFL indicator
- Friends list includes each friend’s study status (Studying / Break / Idle) and a Find action to center the map on them
- Includes a “Go back to my location” chip to quickly re-center on your avatar

---

### 🧠 Daily Reflection
- Log your daily mood using a simple emoji scale
- Add a short personal note (up to 140 characters)
- View your mood history over the past 7 days
- Track mood trends over time (weekly / monthly view)

---

### 🛒 Shop
- Spend earned coins to customize your EduMon
- Browse cosmetic items (hats, accessories, effects)
- Displays your current coin balance
- Online shop with unlockable visual upgrades

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
