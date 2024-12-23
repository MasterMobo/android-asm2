COSC2657 | Android Development
Assignment 2 - Blood Donation Application

Author: Bui Dang Khoa (s3978482)

Features:
- Interactive map that displays donation sites. Click on each site for details or find routes from your current location.
- Robust donation site filter/search features to find sites that suit your needs.
- Easy and secure authentication, allowing for effortless login and sign-up.
- Real-time notifications about site changes and events.
- For Donors:
    + Register to donate at specific sites.
- For Site Managers:
    + Register new donation sites by specifying information like name, address, availability, and opening hours.
    + View and download reports of current sites.
    + Manage donation drives.
    + Volunteer at other donation sites.
- For Super User:
    + View all sites and donation drives in the system.
    + Generate reports on donation drives


Technologies Used:
- Android Studio Ladybug | 2024.2.1 Patch 2
    + Primary development environment
- Firebase Firestore
    + Used to store system data (e.g, user info, site info, etc.)
- Firebase Authentication
    + Used for login and sign-up features. Managing user accounts.
- Google Maps API
    + Displaying the map View
- Google Play Services Location API
    + Getting the current user location
- OkHttp
    + HTTP client for making requests to the Google Maps Directions services for rendering routes
- iText PDF
    + Library for generating PDF reports
