# EduMon — Gamified Study Companion for Students  
<img width="300" height="300" alt="image" src="https://github.com/user-attachments/assets/2c303ab1-dd07-4099-a276-4ee5bcb8bac1" />

EduMon is a gamified productivity app designed to help first-year university students stay consistent, organized, and motivated throughout their studies.  
By taking care of a virtual creature that mirrors your study habits, you are encouraged to maintain balance between learning and well-being. 

---

## Vision

> “Helping BA1 students succeed without burning out.”

EduMon turns daily academic and personal tasks into a rewarding game.  
The more you study and stay consistent, the happier and healthier your EduMon becomes.

---

## Architecture (MVVM)

The app follows a **Model-View-ViewModel (MVVM)** architecture for clean state management and modularity.

- **UI Layer (Jetpack Compose)** — interactive and modern composable screens  
- **ViewModel Layer** — handles logic, updates the UI, connects to repositories  
- **Data Layer (Firebase, Firestore)** — stores user progress, pet state, and planner data  
- **External Services Layer**



---

## Screens

###  Home Screen
- Displays your EduMon and an overview of your statictics and your edumon statictics  
- Overview of your top 3 Todo lists  
- Quick action buttons (Focus Mode, Pomodoro timer, etc.)  
- Bottom navigation bar (Home, Study session, Calendar, Games, Profile)  
- App icon at the top left for quick screen navigation

---

### Todo / Planner / Calendar
- Add, edit, and delete tasks  
- **Planner:** short-term academic tasks (labs, exercises, exams)  
- **Calendar:** monthly view showing all tasks, exams and events  
- Visual progress indicators for each task  

---

### Profile
- Displays your creature and personal stats  
- Basic user information (profile pic, university, level, ...)  
- Options to enable notifications, location, and focus mode  
- Customize your EduMon’s look and accessories  
- Logout button

---

### Stats & Progress
- Weekly and semester tracking of your study activity  
- Visual charts showing your progress toward goals  
- Displays study streaks, completed tasks, and consistency level  
- Highlights growth of your EduMon alongside your productivity

---

### Games
- Small, simple games integrated in the app  
- Earn points or rewards that help care for your EduMon  
- Fun way to relax while staying connected to your study journey  

---

## Tech Stack

| Component | Technology |
|------------|-------------|
| **Frontend** | Kotlin + Jetpack Compose |
| **Architecture** | MVVM |
| **Backend** | Firebase (Auth, Firestore) |

| **Authentication** | Google Sign-In |



---

## Design & Style

- **Glassy and glowing cards** with a semi-transparent look  
- Soothing colors and soft animations  
- The creature and UI evolve as your semester progresses  

---

>  *EduMon — Study smart, care for your mind, grow your companion.*
