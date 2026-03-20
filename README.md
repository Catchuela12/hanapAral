# HanapAral 📚
### Cloud-Based Integrated Study Group Finder

A mobile application for managing study groups that integrates cloud-based services to support authentication, user profiles, group collaboration, and remote configuration.

---

## 👥 Team Members

| Member | Feature | Branch |
|--------|---------|--------|
| Member A | Authentication + FCM Token Registration | `feature/auth` |
| Member B | Student Profile Management | `feature/profile` |
| Member C | Study Group Creation & Join | `feature/groups` |
| Member D | Cloud Messaging & Notifications | `feature/fcm` |
| Member E | Remote Config & Biometric Superuser | `feature/remote-config` |

---

## 📱 Features

### 1. User Authentication
- Google Sign-In via Firebase Authentication
- Only authenticated users can access main features
- FCM token registered on login

### 2. Student Profile Management
- Create and update student profile after login
- Profile includes name, course/program, and year
- Data stored in Firebase Realtime Database

### 3. Study Group Creation
- Authenticated users can create a study group
- Group creator becomes the administrator
- Group data stored in Firebase Realtime Database

### 4. Join Study Groups
- View all available study groups
- Join a group with member count validation
- Real-time updates via Firebase Realtime Database

### 5. Cloud Messaging (FCM)
- Push notification when a new member joins a group
- Group announcement notifications
- Study reminder notifications
- Custom notification icon and sound

### 6. Remote Configuration
- Firebase Remote Config manages app state externally
- Superuser panel gated behind biometric authentication
- Toggle group creation on/off without app update
- Update announcement header globally
- Set maximum members per group

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Repository Pattern |
| DI | Hilt |
| Authentication | Firebase Authentication |
| Database | Firebase Realtime Database |
| Notifications | Firebase Cloud Messaging (FCM) |
| Remote Config | Firebase Remote Config |
| Navigation | Jetpack Navigation Compose |
| Biometric | AndroidX Biometric |

---

## 🏗️ Project Structure
