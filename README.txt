== בניית ה-jar ==
1. התקן JDK 21 (למשל Temurin 21) ו-Gradle.
2. בתיקייה הזו הרץ פעם אחת:   gradle wrapper
3. ואז:                       ./gradlew build        (ב-Windows: gradlew.bat build)
4. הקובץ המוכן: build/libs/tiktok-zoo-1.0.0.jar  (לא את ה-sources.jar)
5. שים אותו בתיקיית mods יחד עם Fabric API.

== פקודות ==
/tiktok connect <שם_משתמש>   התחברות ללייב (בלי @)
/tiktok disconnect           ניתוק
/test follow [שם]            מדמה עוקב חדש
/test gift <מטבעות> [שם]     מדמה מתנה
