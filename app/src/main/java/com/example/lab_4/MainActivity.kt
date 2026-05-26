package com.example.lab_4

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    data class User(val id: Long, val username: String, val email: String)
    data class Post(val id: Long, val userId: Long, val content: String, val timestamp: String)
    data class Comment(val id: Long, val postId: Long, val userId: Long, val text: String)

    private lateinit var dbHelper: SocialNetworkDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = SocialNetworkDatabaseHelper(this)
        seedInitialData() // Заповнюємо БД початковими сутностями

        val etPostContent = findViewById<EditText>(R.id.etPostContent)
        val etCommentContent = findViewById<EditText>(R.id.etCommentContent)
        val etSearchQuery = findViewById<EditText>(R.id.etSearchQuery)
        val tvConsoleLogs = findViewById<TextView>(R.id.tvConsoleLogs)

        findViewById<Button>(R.id.btnAddPost).setOnClickListener {
            val content = etPostContent.text.toString()
            if (content.isNotBlank()) {
                val newPostId = dbHelper.insertPost(1, content) // імітуємо пост від користувача з ID 1
                etPostContent.text.clear()
                Toast.makeText(this, "Пост успішно створено!", Toast.LENGTH_SHORT).show()
                renderDatabaseState(tvConsoleLogs)
            }
        }

        findViewById<Button>(R.id.btnAddComment).setOnClickListener {
            val text = etCommentContent.text.toString()
            val lastPostId = dbHelper.getLastPostId()
            if (text.isNotBlank() && lastPostId != -1L) {
                dbHelper.insertComment(lastPostId, 2, text) // імітуємо комент від користувача з ID 2
                etCommentContent.text.clear()
                Toast.makeText(this, "Коментар додано!", Toast.LENGTH_SHORT).show()
                renderDatabaseState(tvConsoleLogs)
            } else {
                Toast.makeText(this, "Спочатку створіть хоча б один пост", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val query = etSearchQuery.text.toString()
            val filteredPosts = dbHelper.searchPostsSecure(query)

            val sb = StringBuilder("=== РЕЗУЛЬТАТИ БЕЗПЕЧНОГО ПОШУКУ ===\n")
            if (filteredPosts.isEmpty()) {
                sb.append("Нічого не знайдено.")
            } else {
                filteredPosts.forEach { sb.append("[ID: ${it.id}] Пост: ${it.content}\n") }
            }
            tvConsoleLogs.text = sb.toString()
        }

        findViewById<Button>(R.id.btnShowAll).setOnClickListener {
            renderDatabaseState(tvConsoleLogs)
        }
    }

    private fun seedInitialData() {
        if (dbHelper.getUsersCount() == 0) {
            dbHelper.insertUser("serge_dev", "serge@nure.ua")
            dbHelper.insertUser("alex_professor", "alex@nure.ua")
            dbHelper.insertPost(1, "Привіт усім! Це мій перший пост у новій базі даних фреймворку.")
        }
    }

    private fun renderDatabaseState(tv: TextView) {
        val posts = dbHelper.getAllPosts()
        val sb = StringBuilder("=== СТАН БАЗИ ДАНИХ (5 ТАБЛИЦЬ) ===\n\n")
        sb.append("Таблиці проекту: Users, Posts, Comments, Likes, Friends\n")
        sb.append("Кількість користувачів у системі: ${dbHelper.getUsersCount()}\n\n")
        sb.append("--- ПУБЛІКАЦІЇ ТА КОМЕНТАРІ ---\n")

        for (post in posts) {
            sb.append("[ПОСТ ID: ${post.id}] Автор ID: ${post.userId}\n• Вміст: ${post.content}\n")
            val comments = dbHelper.getCommentsForPost(post.id)
            if (comments.isNotEmpty()) {
                sb.append("  Коментарі:\n")
                comments.forEach { sb.append("  -> [Комент ID: ${it.id}] Користувач ${it.userId}: ${it.text}\n") }
            }
            sb.append("\n")
        }
        tv.text = sb.toString()
    }

    class SocialNetworkDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        companion object {
            private const val DATABASE_NAME = "social_network.db"
            private const val DATABASE_VERSION = 1
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, email TEXT);")
            db.execSQL("CREATE TABLE posts (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, content TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user_id) REFERENCES users(id));")
            db.execSQL("CREATE TABLE comments (id INTEGER PRIMARY KEY AUTOINCREMENT, post_id INTEGER, user_id INTEGER, text TEXT, FOREIGN KEY(post_id) REFERENCES posts(id), FOREIGN KEY(user_id) REFERENCES users(id));")
            db.execSQL("CREATE TABLE likes (user_id INTEGER, post_id INTEGER, PRIMARY KEY(user_id, post_id), FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(post_id) REFERENCES posts(id));")
            db.execSQL("CREATE TABLE friends (user_id INTEGER, friend_id INTEGER, PRIMARY KEY(user_id, friend_id), FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(friend_id) REFERENCES users(id));")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS friends"); db.execSQL("DROP TABLE IF EXISTS likes")
            db.execSQL("DROP TABLE IF EXISTS comments"); db.execSQL("DROP TABLE IF EXISTS posts")
            db.execSQL("DROP TABLE IF EXISTS users"); onCreate(db)
        }

        fun insertUser(username: String, email: String): Long {
            val values = ContentValues().apply { put("username", username); put("email", email) }
            return writableDatabase.insert("users", null, values)
        }

        fun insertPost(userId: Long, content: String): Long {
            val values = ContentValues().apply { put("user_id", userId); put("content", content) }
            return writableDatabase.insert("posts", null, values)
        }

        fun insertComment(postId: Long, userId: Long, text: String): Long {
            val values = ContentValues().apply { put("post_id", postId); put("user_id", userId); put("text", text) }
            return writableDatabase.insert("comments", null, values)
        }

        fun getUsersCount(): Int {
            val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM users", null)
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            return count
        }

        fun getLastPostId(): Long {
            val cursor = readableDatabase.rawQuery("SELECT id FROM posts ORDER BY id DESC LIMIT 1", null)
            var id = -1L
            if (cursor.moveToFirst()) id = cursor.getLong(0)
            cursor.close()
            return id
        }

        fun getAllPosts(): List<Post> {
            val list = mutableListOf<Post>()
            val cursor = readableDatabase.rawQuery("SELECT id, user_id, content, timestamp FROM posts", null)
            while (cursor.moveToNext()) {
                list.add(Post(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3)))
            }
            cursor.close()
            return list
        }

        fun getCommentsForPost(postId: Long): List<Comment> {
            val list = mutableListOf<Comment>()
            val cursor = readableDatabase.rawQuery("SELECT id, post_id, user_id, text FROM comments WHERE post_id = ?", arrayOf(postId.toString()))
            while (cursor.moveToNext()) {
                list.add(Comment(cursor.getLong(0), cursor.getLong(1), cursor.getLong(2), cursor.getString(3)))
            }
            cursor.close()
            return list
        }

        fun searchPostsSecure(query: String): List<Post> {
            val list = mutableListOf<Post>()
            val selection = "content LIKE ?"
            val selectionArgs = arrayOf("%$query%")

            val cursor = readableDatabase.query(
                "posts",
                arrayOf("id", "user_id", "content", "timestamp"),
                selection,
                selectionArgs,
                null, null, null
            )

            while (cursor.moveToNext()) {
                list.add(Post(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3)))
            }
            cursor.close()
            return list
        }
    }
}