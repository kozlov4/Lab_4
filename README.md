# 📱 Social Network SQLite — Android App (Lab 4)

Android-застосунок із реляційною БД SQLite для соціальної мережі.

## Реалізовано

**Рівень 1** — 5 таблиць: Users, Posts, Comments, Likes, Friends із FK і нормалізацією  
**Рівень 2** — CRUD через ContentValues + Cursor (ORM-підхід)  
**Рівень 3** — Пошук і фільтрація постів за ключовим словом (LIKE)  
**Рівень 4** — Захист від SQL-ін'єкцій через параметризовані запити `query()`

## Запуск

Відкрити у **Android Studio** → `Run > Run 'app'`  
Мінімальна версія: **API 24 (Android 7.0)**

## Структура

```
app/src/main/
├── java/.../MainActivity.kt      # Activity + SocialNetworkDatabaseHelper
└── res/layout/
    └── activity_main.xml         # ScrollView: CRUD + пошук + лог
```
