# LukePetzer-ST10299850-PROG7313-Part3

# SIK - Smart Income & Expense Tracker

## Table of Contents

1. [Introduction](#introduction)
2. [Features](#features)
3. [Installation](#installation)
4. [Usage](#usage)
5. [Technologies Used](#technologies-used)
6. [Project Improvements](#project-improvements)
7. [Resources & References](#resources--references)
8. [Contributing](#contributing)
9. [License](#license)

## Introduction

SIK (Smart Income & Expense Tracker) is an Android application designed to help users manage their personal finances effectively. It provides a user-friendly interface for tracking income, expenses, and budgeting across various categories.

The goal of this application is to empower users with real-time insights into their spending habits while promoting responsible financial management.

## Features

### 1. Transaction Management

* Add, edit, and delete income and expense transactions.
* Categorize transactions for better organization.
* Attach receipt images to transactions.

### 2. Budget Goals

* Set short-term and long-term budget goals.
* Visual representation of progress towards goals.

### 3. Category Management

* Create and manage custom transaction categories.
* Set budget limits for each category.

### 4. Data Visualization

* Bar chart showing category-wise spending.
* Progress bars for category budget utilization.

### 5. Time-based Filtering

* Filter transactions by day, week, month, or custom date range.

### 6. Financial Insights

* View remaining budget.
* Track monthly expenses.
* Analyze spending patterns over time.

### 7. User Profile

* Personalized user profiles.
* Track achievement badges.

### 8. Data Export

* Export financial data for backup or analysis.

### 9. Settings

* Customize app preferences.
* Toggle notifications.

## Installation

1. Clone the repository:

```bash
git clone https://github.com/your-repository-url.git
```

2. Open the project in Android Studio.

3. Build and run the application on an emulator or physical device.

## Usage

1. Sign up or log in to your account.
2. Set up your initial budget goals and categories.
3. Start adding your income and expenses.
4. Use the home screen to get an overview of your financial status.
5. Explore different charts and filters to analyze your spending habits.
6. Adjust your budget goals as needed.

## Technologies Used

* Kotlin
* Android Jetpack (ViewModel, LiveData, Navigation)
* Firebase (Authentication, Firestore)
* MPAndroidChart for data visualization
* Material Design components

## Project Improvements

Throughout the development of this project, several key improvements and design changes were made to enhance the user experience and align with modern usability standards:

### Design Revisions

* The overall layout was restructured to offer a more practical and intuitive user interface.
* Screens were simplified and redesigned to reduce clutter and improve accessibility.
* User flows were optimized for quicker navigation and smoother transitions between key functions.

### Firebase Integration

* Originally, the application utilized local storage which limited accessibility to a single device.
* Firebase Authentication and Firestore were integrated to allow users to log in to a single account from multiple devices, enabling real-time synchronization of their financial data.
* This improvement supports scalability, better security, and cloud-based persistence.

### Motivation for Changes

* The original design, while functional, was limited in scalability and flexibility.
* The improvements made the app significantly more user-friendly and accessible across devices, which enhances its practicality for everyday financial management.
* Firebase provided robust user management, easy real-time updates, and the potential for future scalability.

## Resources & References

The development of this project was supported by several key resources and libraries, including but not limited to:

* Android Developer Documentation: [https://developer.android.com/docs](https://developer.android.com/docs)
* Firebase Documentation: [https://firebase.google.com/docs](https://firebase.google.com/docs)
* MPAndroidChart GitHub Repository: [https://github.com/PhilJay/MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
* Material Design Guidelines: [https://material.io/design](https://material.io/design)

Additional Learning Resources:

* Udacity Android Kotlin Developer Nanodegree
* Google Codelabs: Firebase for Android
* YouTube tutorials on Android Jetpack and Firebase integration

## Demonstration Video

You can watch a full walkthrough of the SIK application here:
[SIK Demo Video](https://youtu.be/K6yf99fC8pY)

## Screenshots

*(Add your screenshots here)*
## References
References

Android Developers. (2023). Android API reference. Google. https://developer.android.com/reference

Aggarwal, R. (2021). Android application development with Kotlin: Build robust and scalable mobile apps with Kotlin. Packt Publishing.

Bose, A. (2022). Mastering Kotlin for Android development. Apress.

Deitel, P., & Deitel, H. (2021). Android for programmers: An app-driven approach (4th ed.). Pearson.

Firebase. (2023). Firebase documentation. Google. https://firebase.google.com/docs

Google. (2023). Material Design. https://material.io/design

Horton, J. (2021). Android programming with Kotlin for beginners. Packt Publishing.

Jemerov, D., & Isakova, S. (2017). Kotlin in action. Manning Publications.

Komatineni, S., & MacLean, D. (2021). Pro Android 5 (5th ed.). Apress.

Kotlinlang.org. (2023). Kotlin programming language. JetBrains. https://kotlinlang.org/docs/home.html

Leiva, A. (2019). Kotlin for Android developers: Learn Kotlin the easy way while developing an Android app. Independently published.

MPAndroidChart. (2023). MPAndroidChart documentation. GitHub. https://github.com/PhilJay/MPAndroidChart

Phillips, B., Stewart, C., & Marsicano, K. (2022). Android programming: The big nerd ranch guide (5th ed.). Big Nerd Ranch Guides.

Skeet, J. (2019). C# in depth (4th ed.). Manning Publications.

Smyth, N. (2020). Android Studio 4.0 development essentials - Kotlin edition. Payload Media.

Sommerville, I. (2020). Software engineering (10th ed.). Pearson.

Tyson, E. (2022). Personal finance for dummies (9th ed.). For Dummies.

Vogel, L. (2023). Android development tutorial. Vogella. https://www.vogella.com/tutorials/android.html

## Contributing

Contributions to improve SIK are welcome. Please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

## License

Distributed under the MIT License. See `LICENSE` for more information.
