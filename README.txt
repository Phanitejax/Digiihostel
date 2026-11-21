DIGIIHOSTEL - Android Studio project (Java, no external libraries)

1. Android Studio > File > Open > select this "Digiihostel" folder. Let Gradle sync (needs internet once).
2. Run on an emulator/phone, or Build > Build Bundle(s) / APK(s) > Build APK(s).
   The APK appears in app/build/outputs/apk/debug/app-debug.apk
Test logins: admin/admin123, student/student123, warden/warden123, food/food123

--- v3 changes ---
Test logins unchanged. Test student enrollment is now 202301010001 (12 digits).
* Admin > Student Details: left bar 1st-4th year, right pane students (Edit / Delete), "+ New student", and a "Requests" button in the top-right corner.
* Login screen > Register now sends a REQUEST to the admin; admin approves (login auto-created) or rejects.
* Student form: 12-digit enrollment, 10-digit phone/parent, number keyboards, valid e-mail, scroll date picker for DOB,
  School dropdown -> Branch dropdown (opens with that school's courses), hostel list filtered by gender.
* No spaces allowed in any username/password (login, admin edit, account manager).
* Admin > Account Manager: Admin / Warden / Food Incharge (details, edit, add accounts).
* Warden > Room Register (Edit): long-press a hostel > choose Boys / Girls (who may book it).
* New bubble look + opening animations.
