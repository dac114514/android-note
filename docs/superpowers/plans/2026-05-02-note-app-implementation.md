# Note App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a fully functional Android note app with rich text editing, folder/tag organization, waterfall layout, and local backup/export.

**Architecture:** Lightweight MVVM with Room persistence, Jetpack Navigation Compose, and custom rich text editor. Single-module app organized by feature packages under `com.faster.note`.

**Tech Stack:** Jetpack Compose + M3, Room, Navigation Compose, Kotlin Coroutines/Flow, ZIP I/O for backup.

---

### Task 1: Project Setup — Repo, Package, Dependencies

**Files:**
- Create: `CLAUDE.md`
- Modify: `settings.gradle.kts` (rootProject.name)
- Modify: `app/build.gradle.kts` (namespace, applicationId, dependencies)
- Modify: `gradle/libs.versions.toml` (add Room, Navigation versions)
- Modify: `app/src/main/res/values/strings.xml` (app_name)
- Modify: `.gitignore` (finalize)

- [ ] **Step 1: Create CLAUDE.md**

```markdown
# Note App

Android note-taking app with Jetpack Compose + Material Design 3.

## Tech
- Kotlin 100%, Jetpack Compose, Material Design 3
- Room for local persistence
- Jetpack Navigation Compose
- MVVM architecture

## Conventions
- Package: com.faster.note
- Commit style: conventional commits (feat:, fix:, refactor:, docs:, chore:)
- All UI in Compose, no XML layouts
- ViewModel per screen, Repository per entity group
- Format on save before commit
```

- [ ] **Step 2: Update settings.gradle.kts**

Change `rootProject.name = "My Application"` to `rootProject.name = "NoteApp"`.

- [ ] **Step 3: Update app/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {
    namespace = "com.faster.note"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.faster.note"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.android.tools.build" && requested.name == "aapt2") {
            useTarget("com.android.tools.build:aapt2:${requested.version}:linux-aarch64")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

- [ ] **Step 4: Add to gradle/libs.versions.toml**

In `[versions]`:
```toml
navigationCompose = "2.8.5"
lifecycleViewmodelCompose = "2.8.7"
room = "2.6.1"
materialIconsExtended = "1.7.8"
```

In `[libraries]`:
```toml
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended", version.ref = "materialIconsExtended" }
```

- [ ] **Step 5: Update strings.xml**

```xml
<resources>
    <string name="app_name">极简笔记</string>
</resources>
```

- [ ] **Step 6: Delete old template files**

Delete `app/src/main/java/com/java/myapplication/MainActivity.kt` and the entire `ui/theme/` directory under old package path. We will recreate them under `com.faster.note`.

- [ ] **Step 7: Set up GitHub remote and push**

```bash
git remote add origin https://github.com/dac114514/android-note.git
git push -u origin master
git checkout -b main
git push -u origin main
```

- [ ] **Step 8: Build verification**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 2: Room Entities

**Files:**
- Create: `app/src/main/java/com/faster/note/data/db/entity/NoteEntity.kt`
- Create: `app/src/main/java/com/faster/note/data/db/entity/FolderEntity.kt`
- Create: `app/src/main/java/com/faster/note/data/db/entity/TagEntity.kt`
- Create: `app/src/main/java/com/faster/note/data/db/entity/NoteTagCrossRef.kt`

- [ ] **Step 1: Create NoteEntity.kt**

```kotlin
package com.faster.note.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val color: Int? = null
)
```

- [ ] **Step 2: Create FolderEntity.kt**

```kotlin
package com.faster.note.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int = 0xFF6C63FF.toInt()
)
```

- [ ] **Step 3: Create TagEntity.kt**

```kotlin
package com.faster.note.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
```

- [ ] **Step 4: Create NoteTagCrossRef.kt**

```kotlin
package com.faster.note.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]
)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagId: Long
)
```

---

### Task 3: DAOs

**Files:**
- Create: `app/src/main/java/com/faster/note/data/db/dao/NoteDao.kt`
- Create: `app/src/main/java/com/faster/note/data/db/dao/FolderDao.kt`
- Create: `app/src/main/java/com/faster/note/data/db/dao/TagDao.kt`

- [ ] **Step 1: Create NoteDao.kt**

```kotlin
package com.faster.note.data.db.dao

import androidx.room.*
import com.faster.note.data.db.entity.NoteEntity
import com.faster.note.data.db.entity.NoteTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>>

    @Query("""
        SELECT n.* FROM notes n 
        INNER JOIN note_tag_cross_ref c ON n.id = c.noteId 
        WHERE c.tagId = :tagId 
        ORDER BY n.updatedAt DESC
    """)
    fun getNotesByTag(tagId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    // Tag associations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToNote(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun removeAllTagsFromNote(noteId: Long)

    @Query("SELECT tagId FROM note_tag_cross_ref WHERE noteId = :noteId")
    fun getTagIdsForNote(noteId: Long): Flow<List<Long>>
}
```

- [ ] **Step 2: Create FolderDao.kt**

```kotlin
package com.faster.note.data.db.dao

import androidx.room.*
import com.faster.note.data.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?

    @Insert
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}
```

- [ ] **Step 3: Create TagDao.kt**

```kotlin
package com.faster.note.data.db.dao

import androidx.room.*
import com.faster.note.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("SELECT t.* FROM tags t INNER JOIN note_tag_cross_ref c ON t.id = c.tagId WHERE c.noteId = :noteId")
    fun getTagsForNote(noteId: Long): Flow<List<TagEntity>>
}
```

---

### Task 4: Database, Relations, Converters

**Files:**
- Create: `app/src/main/java/com/faster/note/data/db/NoteDatabase.kt`
- Create: `app/src/main/java/com/faster/note/data/db/entity/NoteWithTags.kt`
- Create: `app/src/main/java/com/faster/note/data/db/entity/FolderWithNotes.kt`

- [ ] **Step 1: Create NoteWithTags.kt**

```kotlin
package com.faster.note.data.db.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class NoteWithTags(
    @Embedded val note: NoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList()
)
```

- [ ] **Step 2: Create FolderWithNotes.kt**

```kotlin
package com.faster.note.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class FolderWithNotes(
    @Embedded val folder: FolderEntity,
    @Relation(parentColumn = "id", entityColumn = "folderId")
    val notes: List<NoteEntity> = emptyList()
)
```

- [ ] **Step 3: Create NoteDatabase.kt**

```kotlin
package com.faster.note.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.faster.note.data.db.dao.FolderDao
import com.faster.note.data.db.dao.NoteDao
import com.faster.note.data.db.dao.TagDao
import com.faster.note.data.db.entity.*

@Database(
    entities = [NoteEntity::class, FolderEntity::class, TagEntity::class, NoteTagCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile private var INSTANCE: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_app.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

---

### Task 5: Repositories

**Files:**
- Create: `app/src/main/java/com/faster/note/data/repository/NoteRepository.kt`
- Create: `app/src/main/java/com/faster/note/data/repository/FolderRepository.kt`
- Create: `app/src/main/java/com/faster/note/data/repository/TagRepository.kt`

- [ ] **Step 1: Create NoteRepository.kt**

```kotlin
package com.faster.note.data.repository

import com.faster.note.data.db.dao.NoteDao
import com.faster.note.data.db.entity.NoteEntity
import com.faster.note.data.db.entity.NoteTagCrossRef
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNotesByFolder(folderId: Long) = noteDao.getNotesByFolder(folderId)

    fun getNotesByTag(tagId: Long) = noteDao.getNotesByTag(tagId)

    fun getFavoriteNotes() = noteDao.getFavoriteNotes()

    suspend fun getNoteById(id: Long) = noteDao.getNoteById(id)

    suspend fun saveNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)

    suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>) {
        noteDao.removeAllTagsFromNote(noteId)
        tagIds.forEach { tagId ->
            noteDao.addTagToNote(NoteTagCrossRef(noteId, tagId))
        }
    }

    fun getTagIdsForNote(noteId: Long) = noteDao.getTagIdsForNote(noteId)
}
```

- [ ] **Step 2: Create FolderRepository.kt**

```kotlin
package com.faster.note.data.repository

import com.faster.note.data.db.dao.FolderDao
import com.faster.note.data.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

class FolderRepository(private val folderDao: FolderDao) {

    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()

    suspend fun getFolderById(id: Long) = folderDao.getFolderById(id)

    suspend fun saveFolder(folder: FolderEntity): Long = folderDao.insertFolder(folder)

    suspend fun updateFolder(folder: FolderEntity) = folderDao.updateFolder(folder)

    suspend fun deleteFolder(folder: FolderEntity) = folderDao.deleteFolder(folder)
}
```

- [ ] **Step 3: Create TagRepository.kt**

```kotlin
package com.faster.note.data.repository

import com.faster.note.data.db.dao.TagDao
import com.faster.note.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

class TagRepository(private val tagDao: TagDao) {

    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getTagsForNote(noteId: Long) = tagDao.getTagsForNote(noteId)

    suspend fun saveTag(tag: TagEntity): Long = tagDao.insertTag(tag)

    suspend fun deleteTag(tag: TagEntity) = tagDao.deleteTag(tag)
}
```

---

### Task 6: Theme & Application Class

**Files:**
- Create: `app/src/main/java/com/faster/note/NoteApp.kt`
- Create: `app/src/main/java/com/faster/note/ui/theme/Color.kt`
- Create: `app/src/main/java/com/faster/note/ui/theme/Type.kt`
- Create: `app/src/main/java/com/faster/note/ui/theme/Theme.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create NoteApp.kt**

```kotlin
package com.faster.note

import android.app.Application
import com.faster.note.data.db.NoteDatabase
import com.faster.note.data.repository.FolderRepository
import com.faster.note.data.repository.NoteRepository
import com.faster.note.data.repository.TagRepository

class NoteApp : Application() {
    val database by lazy { NoteDatabase.getInstance(this) }
    val noteRepository by lazy { NoteRepository(database.noteDao()) }
    val folderRepository by lazy { FolderRepository(database.folderDao()) }
    val tagRepository by lazy { TagRepository(database.tagDao()) }
}
```

- [ ] **Step 2: Update AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".NoteApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyApplication"
        tools:targetApi="31">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.MyApplication">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 3: Create Color.kt** (update for note app palette)

```kotlin
package com.faster.note.ui.theme

import androidx.compose.ui.graphics.Color

val Blue80 = Color(0xFFB3D4FF)
val BlueGrey80 = Color(0xFFB0BEC5)
val Teal80 = Color(0xFF80CBC4)

val Blue40 = Color(0xFF1565C0)
val BlueGrey40 = Color(0xFF546E7A)
val Teal40 = Color(0xFF00897B)

// Note card colors
val NoteColors = listOf(
    Color(0xFFFFF3E0), Color(0xFFE8F5E9), Color(0xFFE3F2FD),
    Color(0xFFFCE4EC), Color(0xFFF3E5F5), Color(0xFFE0F7FA),
    Color(0xFFFFFDE7), Color(0xFFFBE9E7)
)
```

- [ ] **Step 4: Create Type.kt** (keep as-is with better typography)

```kotlin
package com.faster.note.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)
```

- [ ] **Step 5: Create Theme.kt**

```kotlin
package com.faster.note.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue80, secondary = BlueGrey80, tertiary = Teal80
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40, secondary = BlueGrey40, tertiary = Teal40
)

@Composable
fun NoteAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

---

### Task 7: Navigation Graph

**Files:**
- Create: `app/src/main/java/com/faster/note/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create NavGraph.kt**

```kotlin
package com.faster.note.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.faster.note.ui.editor.NoteEditScreen
import com.faster.note.ui.folders.FolderScreen
import com.faster.note.ui.notes.NoteListScreen
import com.faster.note.ui.search.SearchScreen
import com.faster.note.ui.settings.SettingsScreen

object Routes {
    const val NOTES = "notes"
    const val FOLDERS = "folders"
    const val NOTE_EDIT = "note/{noteId}"
    const val NOTE_NEW = "note/new"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    fun noteEdit(id: Long) = "note/$id"
}

@Composable
fun NoteNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.NOTES) {
        composable(Routes.NOTES) {
            NoteListScreen(
                onOpenNote = { id -> navController.navigate(Routes.noteEdit(id)) },
                onNewNote = { navController.navigate(Routes.NOTE_NEW) },
                onOpenFolders = { navController.navigate(Routes.FOLDERS) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.NOTE_EDIT,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            NoteEditScreen(noteId = noteId, onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTE_NEW) {
            NoteEditScreen(noteId = null, onBack = { navController.popBackStack() })
        }
        composable(Routes.FOLDERS) {
            FolderScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SEARCH) {
            SearchScreen(onOpenNote = { id -> navController.navigate(Routes.noteEdit(id)) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
```

---

### Task 8: MainActivity with Bottom Navigation

**Files:**
- Create: `app/src/main/java/com/faster/note/MainActivity.kt`

- [ ] **Step 1: Create MainActivity.kt**

```kotlin
package com.faster.note

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.faster.note.ui.navigation.NoteNavHost
import com.faster.note.ui.navigation.Routes
import com.faster.note.ui.theme.NoteAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteAppTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val bottomBarVisible = currentDestination?.route in listOf(
                    Routes.NOTES, Routes.FOLDERS, Routes.SETTINGS
                )

                Scaffold(
                    bottomBar = {
                        if (bottomBarVisible) {
                            NavigationBar {
                                val items = listOf(
                                    Triple(Routes.NOTES, "笔记", Icons.Filled.Home, Icons.Outlined.Home),
                                    Triple(Routes.FOLDERS, "文件夹", Icons.Filled.CreateNewFolder, Icons.Outlined.CreateNewFolder),
                                    Triple(Routes.SETTINGS, "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
                                )
                                items.forEach { (route, label, selectedIcon, unselectedIcon) ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(if (selected) selectedIcon else unselectedIcon, contentDescription = label) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NoteNavHost(navController = navController)
                    }
                }
            }
        }
    }
}
```

---

### Task 9: NoteListScreen — Waterfall Layout

**Files:**
- Create: `app/src/main/java/com/faster/note/ui/notes/NoteListViewModel.kt`
- Create: `app/src/main/java/com/faster/note/ui/notes/NoteListScreen.kt`
- Create: `app/src/main/java/com/faster/note/ui/notes/components/NoteCard.kt`

- [ ] **Step 1: Create NoteListViewModel.kt**

```kotlin
package com.faster.note.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.NoteApp
import com.faster.note.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteListViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as NoteApp).noteRepository

    private val _filter by lazy { MutableStateFlow<NoteFilter>(NoteFilter.All) }
    val notes: StateFlow<List<NoteEntity>> = _filter.flatMapLatest { filter ->
        when (filter) {
            is NoteFilter.All -> repo.allNotes
            is NoteFilter.Folder -> repo.getNotesByFolder(filter.folderId)
            is NoteFilter.Tag -> repo.getNotesByTag(filter.tagId)
            is NoteFilter.Favorite -> repo.getFavoriteNotes()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: NoteFilter) { _filter.value = filter }

    fun deleteNote(id: Long) = viewModelScope.launch { repo.deleteNote(id) }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch { repo.updateNote(note.copy(isFavorite = !note.isFavorite)) }
    }
}

sealed class NoteFilter {
    data object All : NoteFilter()
    data class Folder(val folderId: Long) : NoteFilter()
    data class Tag(val tagId: Long) : NoteFilter()
    data object Favorite : NoteFilter()
}
```

- [ ] **Step 2: Create NoteCard.kt**

```kotlin
package com.faster.note.ui.notes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.faster.note.data.db.entity.NoteEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = note.color?.let { androidx.compose.ui.graphics.Color(it) }
                ?: MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content.replace(Regex("<[^>]*>"), "").take(150),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(note.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
```

- [ ] **Step 3: Create NoteListScreen.kt** (waterfall layout with staggered grid)

This screen uses a custom staggered grid approach (two columns, masonry style):

```kotlin
package com.faster.note.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.note.ui.notes.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: NoteListViewModel = viewModel()
) {
    val notes by viewModel.notes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("极简笔记") },
                actions = {
                    IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, "搜索") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewNote) {
                Icon(Icons.Default.Add, "新建笔记")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("点击 + 创建第一条笔记", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // Two-column staggered grid via LazyColumn
            // Split items into left and right columns
            val leftNotes = notes.filterIndexed { i, _ -> i % 2 == 0 }
            val rightNotes = notes.filterIndexed { i, _ -> i % 2 == 1 }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(leftNotes.size) { index ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Left card
                        NoteCard(
                            note = leftNotes[index],
                            onClick = { onOpenNote(leftNotes[index].id) },
                            modifier = Modifier.weight(1f).padding(4.dp)
                        )
                        // Right card (if exists)
                        if (index < rightNotes.size) {
                            NoteCard(
                                note = rightNotes[index],
                                onClick = { onOpenNote(rightNotes[index].id) },
                                modifier = Modifier.weight(1f).padding(4.dp)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
```

---

### Task 10: Rich Text Editor — Span Model & HtmlConverter

**Files:**
- Create: `app/src/main/java/com/faster/note/ui/editor/model/SpanModel.kt`
- Create: `app/src/main/java/com/faster/note/ui/editor/model/HtmlConverter.kt`

- [ ] **Step 1: Create SpanModel.kt**

```kotlin
package com.faster.note.ui.editor.model

enum class SpanFormat {
    BOLD, ITALIC, UNDERLINE, STRIKETHROUGH
}

enum class HeadingLevel { H1, H2, H3 }

data class SpanStyle(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false
)

data class SpanNode(
    val text: String,
    val style: SpanStyle = SpanStyle()
) {
    val isEmpty get() = text.isEmpty()
}

data class ParagraphData(
    val type: ParagraphType = ParagraphType.NORMAL,
    val alignment: AlignmentType = AlignmentType.LEFT,
    val spans: List<SpanNode> = emptyList()
)

enum class ParagraphType { NORMAL, H1, H2, H3, BULLET_LIST, ORDERED_LIST }

enum class AlignmentType { LEFT, CENTER, RIGHT }

data class RichTextDocument(
    val paragraphs: List<ParagraphData> = listOf(ParagraphData())
)
```

- [ ] **Step 2: Create HtmlConverter.kt**

```kotlin
package com.faster.note.ui.editor.model

object HtmlConverter {

    fun toHtml(doc: RichTextDocument): String {
        val sb = StringBuilder()
        doc.paragraphs.forEach { para ->
            when (para.type) {
                ParagraphType.H1 -> sb.append("<h1>")
                ParagraphType.H2 -> sb.append("<h2>")
                ParagraphType.H3 -> sb.append("<h3>")
                ParagraphType.BULLET_LIST -> sb.append("<li>")
                ParagraphType.ORDERED_LIST -> sb.append("<li>")
                ParagraphType.NORMAL -> {
                    when (para.alignment) {
                        AlignmentType.CENTER -> sb.append("<p style=\"text-align:center\">")
                        AlignmentType.RIGHT -> sb.append("<p style=\"text-align:right\">")
                        AlignmentType.LEFT -> sb.append("<p>")
                    }
                }
            }
            para.spans.forEach { span ->
                var text = span.text
                if (span.style.isBold) text = "<b>$text</b>"
                if (span.style.isItalic) text = "<i>$text</i>"
                if (span.style.isUnderline) text = "<u>$text</u>"
                if (span.style.isStrikethrough) text = "<s>$text</s>"
                sb.append(text)
            }
            when (para.type) {
                ParagraphType.H1 -> sb.appendLine("</h1>")
                ParagraphType.H2 -> sb.appendLine("</h2>")
                ParagraphType.H3 -> sb.appendLine("</h3>")
                ParagraphType.BULLET_LIST, ParagraphType.ORDERED_LIST -> sb.appendLine("</li>")
                ParagraphType.NORMAL -> sb.appendLine("</p>")
            }
        }
        return sb.toString()
    }

    fun fromHtml(html: String): RichTextDocument {
        if (html.isBlank()) return RichTextDocument()

        val paragraphs = mutableListOf<ParagraphData>()
        val regex = Regex("<(h[1-3]|p|li)([^>]*)>([\\s\\S]*?)</\\1>")
        regex.findAll(html).forEach { match ->
            val tag = match.groupValues[1]
            val attrs = match.groupValues[2]
            val inner = match.groupValues[3]
            val alignment = when {
                "text-align:center" in attrs -> AlignmentType.CENTER
                "text-align:right" in attrs -> AlignmentType.RIGHT
                else -> AlignmentType.LEFT
            }
            val type = when (tag) {
                "h1" -> ParagraphType.H1
                "h2" -> ParagraphType.H2
                "h3" -> ParagraphType.H3
                "li" -> ParagraphType.BULLET_LIST
                else -> ParagraphType.NORMAL
            }
            val spans = parseSpans(inner)
            paragraphs.add(ParagraphData(type = type, alignment = alignment, spans = spans))
        }
        if (paragraphs.isEmpty()) paragraphs.add(ParagraphData())
        return RichTextDocument(paragraphs)
    }

    private fun parseSpans(html: String): List<SpanNode> {
        val spans = mutableListOf<SpanNode>()
        // Simple inline tag parser
        val regex = Regex("<([biustr/]+)>([^<]*)</\\1>")
        var lastEnd = 0
        regex.findAll(html).forEach { m ->
            val before = html.substring(lastEnd, m.range.first)
            if (before.isNotBlank()) spans.add(SpanNode(before))
            val tag = m.groupValues[1]
            val text = m.groupValues[2]
            val style = SpanStyle(
                isBold = tag == "b",
                isItalic = tag == "i",
                isUnderline = tag == "u",
                isStrikethrough = tag == "s"
            )
            spans.add(SpanNode(text, style))
            lastEnd = m.range.last + 1
        }
        val remaining = html.substring(lastEnd)
        if (remaining.isNotBlank()) spans.add(SpanNode(remaining))
        if (spans.isEmpty() && html.isNotBlank()) spans.add(SpanNode(html))
        return spans
    }
}
```

---

### Task 11: Rich Text Editor — UI Components

**Files:**
- Create: `app/src/main/java/com/faster/note/ui/editor/FormatToolbar.kt`
- Create: `app/src/main/java/com/faster/note/ui/editor/RichTextEditor.kt`

- [ ] **Step 1: Create FormatToolbar.kt**

```kotlin
package com.faster.note.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.faster.note.ui.editor.model.AlignmentType
import com.faster.note.ui.editor.model.ParagraphType
import com.faster.note.ui.editor.model.SpanStyle

data class EditorState(
    val spanStyle: SpanStyle = SpanStyle(),
    val paragraphType: ParagraphType = ParagraphType.NORMAL,
    val alignment: AlignmentType = AlignmentType.LEFT
)

@Composable
fun FormatToolbar(
    state: EditorState,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onHeadingClick: (ParagraphType) -> Unit,
    onAlignmentClick: (AlignmentType) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Bold
            FilterChip(
                selected = state.spanStyle.isBold,
                onClick = onToggleBold,
                label = { Text("B", style = MaterialTheme.typography.titleSmall) },
                leadingIcon = null
            )
            // Italic
            FilterChip(
                selected = state.spanStyle.isItalic,
                onClick = onToggleItalic,
                label = { Text("I", style = MaterialTheme.typography.titleSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) }
            )
            // Underline
            FilterChip(
                selected = state.spanStyle.isUnderline,
                onClick = onToggleUnderline,
                label = { Text("U", style = MaterialTheme.typography.titleSmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) }
            )
            // Strikethrough
            FilterChip(
                selected = state.spanStyle.isStrikethrough,
                onClick = onToggleStrikethrough,
                label = { Text("S", style = MaterialTheme.typography.titleSmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) }
            )

            Divider(Modifier.height(24.dp).padding(horizontal = 4.dp))

            // Headings
            listOf(ParagraphType.H1 to "H1", ParagraphType.H2 to "H2", ParagraphType.H3 to "H3").forEach { (type, label) ->
                FilterChip(
                    selected = state.paragraphType == type,
                    onClick = { onHeadingClick(if (state.paragraphType == type) ParagraphType.NORMAL else type) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                )
            }

            Divider(Modifier.height(24.dp).padding(horizontal = 4.dp))

            // Alignment
            listOf(
                AlignmentType.LEFT to FormatAlignLeft,
                AlignmentType.CENTER to FormatAlignCenter,
                AlignmentType.RIGHT to FormatAlignRight
            ).forEach { (type, icon) ->
                IconButton(onClick = { onAlignmentClick(type) }) {
                    Icon(
                        icon,
                        contentDescription = type.name,
                        tint = if (state.alignment == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create RichTextEditor.kt**

```kotlin
package com.faster.note.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.faster.note.ui.editor.model.AlignmentType
import com.faster.note.ui.editor.model.ParagraphType

@Composable
fun RichTextEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    val textStyle = TextStyle(
        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
        fontWeight = if (editorState.spanStyle.isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (editorState.spanStyle.isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when {
            editorState.spanStyle.isUnderline && editorState.spanStyle.isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
            editorState.spanStyle.isUnderline -> TextDecoration.Underline
            editorState.spanStyle.isStrikethrough -> TextDecoration.LineThrough
            else -> TextDecoration.None
        },
        textAlign = when (editorState.alignment) {
            AlignmentType.CENTER -> TextAlign.Center
            AlignmentType.RIGHT -> TextAlign.Right
            AlignmentType.LEFT -> TextAlign.Start
        }
    )

    BasicTextField(
        value = textFieldValue,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (textFieldValue.text.isEmpty()) {
                Text(
                    text = "开始写点什么...",
                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
            }
            innerTextField()
        }
    )
}
```

---

### Task 12: NoteEditScreen (Editor)

**Files:**
- Create: `app/src/main/java/com/faster/note/ui/editor/NoteEditViewModel.kt`
- Create: `app/src/main/java/com/faster/note/ui/editor/NoteEditScreen.kt`

- [ ] **Step 1: Create NoteEditViewModel.kt**

```kotlin
package com.faster.note.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.NoteApp
import com.faster.note.data.db.entity.NoteEntity
import com.faster.note.data.db.entity.TagEntity
import com.faster.note.ui.editor.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteEditViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as NoteApp).noteRepository
    private val tagRepo = (application as NoteApp).tagRepository

    private var _noteId: Long? = null
    val noteId: Long? get() = _noteId

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _htmlContent = MutableStateFlow("")
    val htmlContent: StateFlow<String> = _htmlContent.asStateFlow()

    private val _folderId = MutableStateFlow<Long?>(null)
    val folderId: StateFlow<Long?> = _folderId.asStateFlow()

    private val _noteColor = MutableStateFlow<Int?>(null)
    val noteColor: StateFlow<Int?> = _noteColor.asStateFlow()

    val tags: StateFlow<List<TagEntity>> = tagRepo.allTags.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val selectedTagIds: StateFlow<Set<Long>> = _noteId?.let { id ->
        repo.getTagIdsForNote(id).map { it.toSet() }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet()
        )
    } ?: MutableStateFlow(emptySet())

    private var saveJob: Job? = null

    fun loadNote(id: Long) {
        _noteId = id
        viewModelScope.launch {
            val note = repo.getNoteById(id) ?: return@launch
            _title.value = note.title
            _htmlContent.value = note.content
            _folderId.value = note.folderId
            _noteColor.value = note.color
        }
    }

    fun updateTitle(t: String) { _title.value = t; scheduleSave() }
    fun updateContent(c: String) { _htmlContent.value = c; scheduleSave() }
    fun updateFolder(id: Long?) { _folderId.value = id; scheduleSave() }
    fun updateColor(c: Int?) { _noteColor.value = c; scheduleSave() }

    fun toggleTag(tagId: Long) {
        val current = selectedTagIds.value.toMutableSet()
        if (tagId in current) current.remove(tagId) else current.add(tagId)
        (selectedTagIds as MutableStateFlow).value = current
        scheduleSave()
    }

    fun createTag(name: String) = viewModelScope.launch {
        tagRepo.saveTag(TagEntity(name = name))
    }

    fun saveNow() = viewModelScope.launch { performSave() }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            performSave()
        }
    }

    private suspend fun performSave() {
        val id = _noteId
        val note = NoteEntity(
            id = id ?: 0,
            title = _title.value,
            content = _htmlContent.value,
            folderId = _folderId.value,
            color = _noteColor.value,
            updatedAt = System.currentTimeMillis()
        )
        val savedId = if (id != null) {
            repo.updateNote(note)
            id
        } else {
            val newId = repo.saveNote(note)
            _noteId = newId
            newId
        }
        if (selectedTagIds.value.isNotEmpty()) {
            repo.setTagsForNote(savedId, selectedTagIds.value.toList())
        }
    }
}
```

- [ ] **Step 2: Create NoteEditScreen.kt**

```kotlin
package com.faster.note.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.note.ui.editor.model.AlignmentType
import com.faster.note.ui.editor.model.ParagraphType
import com.faster.note.ui.theme.NoteColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long?,
    onBack: () -> Unit,
    viewModel: NoteEditViewModel = viewModel()
) {
    val title by viewModel.title.collectAsState()
    val htmlContent by viewModel.htmlContent.collectAsState()
    val folderId by viewModel.folderId.collectAsState()
    val noteColor by viewModel.noteColor.collectAsState()
    val allTags by viewModel.tags.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()

    LaunchedEffect(noteId) {
        if (noteId != null) viewModel.loadNote(noteId)
    }

    // Convert HTML content to TextFieldValue on first load
    var textFieldValue by remember(htmlContent) {
        mutableStateOf(TextFieldValue(htmlContent))
    }

    // Track editor state
    var editorState by remember { mutableStateOf(EditorState()) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNow()
                        onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                title = {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.updateTitle(it) },
                        placeholder = { Text("标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.updateColor(
                            if (noteColor == null) NoteColors.random().value.toInt() else null
                        )
                    }) { Icon(Icons.Default.Palette, "颜色") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Format toolbar
            FormatToolbar(
                state = editorState,
                onToggleBold = { editorState = editorState.copy(spanStyle = editorState.spanStyle.copy(isBold = !editorState.spanStyle.isBold)) },
                onToggleItalic = { editorState = editorState.copy(spanStyle = editorState.spanStyle.copy(isItalic = !editorState.spanStyle.isItalic)) },
                onToggleUnderline = { editorState = editorState.copy(spanStyle = editorState.spanStyle.copy(isUnderline = !editorState.spanStyle.isUnderline)) },
                onToggleStrikethrough = { editorState = editorState.copy(spanStyle = editorState.spanStyle.copy(isStrikethrough = !editorState.spanStyle.isStrikethrough)) },
                onHeadingClick = { editorState = editorState.copy(paragraphType = it) },
                onAlignmentClick = { editorState = editorState.copy(alignment = it) }
            )

            // Editor
            RichTextEditor(
                textFieldValue = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    viewModel.updateContent(it.text)
                },
                editorState = editorState,
                modifier = Modifier.weight(1f)
            )

            // Metadata panel
            if (noteColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(noteColor!!))
                )
            }
        }
    }
}
```

Add import for `background` at the top of `NoteEditScreen.kt`.

---

### Task 13: Folder & Tag Management Screen

**Files:**
- Create: `app/src/main/java/com/faster/note/ui/folders/FolderViewModel.kt`
- Create: `app/src/main/java/com/faster/note/ui/folders/FolderScreen.kt`
- Create: `app/src/main/java/com/faster/note/ui/folders/components/FolderCard.kt`
- Create: `app/src/main/java/com/faster/note/ui/folders/components/TagCloud.kt`

- [ ] **Step 1: Create FolderViewModel.kt**

```kotlin
package com.faster.note.ui.folders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.NoteApp
import com.faster.note.data.db.entity.FolderEntity
import com.faster.note.data.db.entity.TagEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FolderViewModel(application: Application) : AndroidViewModel(application) {
    private val folderRepo = (application as NoteApp).folderRepository
    private val tagRepo = (application as NoteApp).tagRepository

    val folders: StateFlow<List<FolderEntity>> = folderRepo.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = tagRepo.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFolder(name: String, color: Int) = viewModelScope.launch {
        folderRepo.saveFolder(FolderEntity(name = name, color = color))
    }

    fun deleteFolder(folder: FolderEntity) = viewModelScope.launch {
        folderRepo.deleteFolder(folder)
    }

    fun createTag(name: String) = viewModelScope.launch {
        tagRepo.saveTag(TagEntity(name = name))
    }

    fun deleteTag(tag: TagEntity) = viewModelScope.launch {
        tagRepo.deleteTag(tag)
    }
}
```

- [ ] **Step 2: Create FolderCard.kt**

```kotlin
package com.faster.note.ui.folders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.faster.note.data.db.entity.FolderEntity

@Composable
fun FolderCard(
    folder: FolderEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(folder.color)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = folder.name.take(1).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
```

- [ ] **Step 3: Create TagCloud.kt**

```kotlin
package com.faster.note.ui.folders.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.faster.note.data.db.entity.TagEntity

@Composable
fun TagCloud(
    tags: List<TagEntity>,
    onDeleteTag: (TagEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            InputChip(
                selected = false,
                onClick = {},
                label = { Text(tag.name) },
                trailingIcon = {
                    IconButton(onClick = { onDeleteTag(tag) }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Close, "删除标签", modifier = Modifier.size(14.dp))
                    }
                }
            )
        }
    }
}
```

- [ ] **Step 4: Create FolderScreen.kt**

```kotlin
package com.faster.note.ui.folders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.note.ui.folders.components.FolderCard
import com.faster.note.ui.folders.components.TagCloud

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    onBack: () -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val folders by viewModel.folders.collectAsState()
    val tags by viewModel.tags.collectAsState()
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewTagDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                title = { Text("文件夹与标签") }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Folders section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("文件夹", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(Icons.Default.Add, "新建文件夹")
                    }
                }
            }
            items(folders) { folder ->
                FolderCard(folder = folder, onClick = { onBack() })
            }

            // Tags section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("标签", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showNewTagDialog = true }) {
                        Icon(Icons.Default.Add, "新建标签")
                    }
                }
            }
            item {
                TagCloud(tags = tags, onDeleteTag = { viewModel.deleteTag(it) })
            }
        }
    }

    // Dialogs
    if (showNewFolderDialog) {
        NewFolderDialog(
            onConfirm = { name, color ->
                viewModel.createFolder(name, color)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }
    if (showNewTagDialog) {
        NewTagDialog(
            onConfirm = { name ->
                viewModel.createTag(name)
                showNewTagDialog = false
            },
            onDismiss = { showNewTagDialog = false }
        )
    }
}

@Composable
private fun NewFolderDialog(onConfirm: (String, Int) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, 0xFF6C63FF.toInt()) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun NewTagDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建标签") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
```

---

### Task 14: Search Screen

**Files:**
- Create: `app/src/main/java/com/faster/note/ui/search/SearchViewModel.kt`
- Create: `app/src/main/java/com/faster/note/ui/search/SearchScreen.kt`

- [ ] **Step 1: Create SearchViewModel.kt**

```kotlin
package com.faster.note.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.NoteApp
import com.faster.note.data.db.entity.NoteEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as NoteApp).noteRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Use SQL LIKE for search since we don't have FTS4 setup in v1
    private val _results = MutableStateFlow<List<NoteEntity>>(emptyList())
    val results: StateFlow<List<NoteEntity>> = _results.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (q.isBlank()) {
                _results.value = emptyList()
                return@launch
            }
            repo.allNotes.first().let { all ->
                _results.value = all.filter {
                    it.title.contains(q, ignoreCase = true) ||
                    it.content.contains(q, ignoreCase = true)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create SearchScreen.kt**

```kotlin
package com.faster.note.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.note.data.db.entity.NoteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenNote: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { /* handled by back handler */ }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.updateQuery(it) },
                        placeholder = { Text("搜索笔记...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent, focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(results) { note ->
                SearchResultItem(note = note, onClick = { onOpenNote(note.id) })
            }
            if (query.isNotBlank() && results.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp)) {
                        Text("未找到相关笔记", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(note: NoteEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(note.title.ifBlank { "无标题" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(note.content.replace(Regex("<[^>]*>"), "").take(100), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

---

### Task 15: Settings & Backup

**Files:**
- Create: `app/src/main/java/com/faster/note/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/faster/note/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/faster/note/util/BackupManager.kt`

- [ ] **Step 1: Create BackupManager.kt**

```kotlin
package com.faster.note.util

import android.content.Context
import com.faster.note.data.db.entity.NoteEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupData(
    val notes: List<NoteEntity>
)

class BackupManager(private val context: Context) {

    private val backupDir = File(context.getExternalFilesDir(null), "NoteApp/Backups")

    init { backupDir.mkdirs() }

    fun exportBackup(notes: List<NoteEntity>): File {
        val timestamp = System.currentTimeMillis()
        val zipFile = File(backupDir, "note_backup_$timestamp.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            // Metadata JSON
            val meta = JSONObject().apply {
                put("version", 1)
                put("exportedAt", timestamp)
                put("noteCount", notes.size)
            }
            zos.putNextEntry(ZipEntry("notes.json"))
            zos.write(meta.toString(2).toByteArray())
            zos.closeEntry()

            // Individual HTML files
            notes.forEach { note ->
                val fileName = "notes/${note.id}.html"
                zos.putNextEntry(ZipEntry(fileName))
                val content = """
                    <html><head><title>${note.title}</title></head>
                    <body>${note.content}</body></html>
                """.trimIndent()
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return zipFile
    }

    fun getBackupFiles(): List<File> {
        return backupDir.listFiles { f -> f.extension == "zip" }?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
```

- [ ] **Step 2: Create SettingsViewModel.kt**

```kotlin
package com.faster.note.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.NoteApp
import com.faster.note.util.BackupManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as NoteApp).noteRepository
    val backupManager = BackupManager(application)

    private val _backupFiles = MutableStateFlow<List<File>>(emptyList())
    val backupFiles: StateFlow<List<File>> = _backupFiles.asStateFlow()

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    fun refreshBackupFiles() {
        _backupFiles.value = backupManager.getBackupFiles()
    }

    fun exportAllNotes() {
        viewModelScope.launch {
            _exporting.value = true
            repo.allNotes.first().let { notes ->
                backupManager.exportBackup(notes)
            }
            refreshBackupFiles()
            _exporting.value = false
        }
    }
}
```

- [ ] **Step 3: Create SettingsScreen.kt**

```kotlin
package com.faster.note.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val exporting by viewModel.exporting.collectAsState()
    val backupFiles by viewModel.backupFiles.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshBackupFiles() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                title = { Text("设置") }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Backup section
            item {
                Text("备份与导出", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
            item {
                ListItem(
                    headlineContent = { Text("导出全部笔记") },
                    supportingContent = { Text("将所有笔记导出为 ZIP 备份文件") },
                    trailingContent = {
                        if (exporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            IconButton(onClick = { viewModel.exportAllNotes() }) {
                                Icon(Icons.Default.Backup, "导出")
                            }
                        }
                    }
                )
            }
            item { HorizontalDivider() }

            // Backup files list
            item {
                Text("历史备份", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
            if (backupFiles.isEmpty()) {
                item {
                    Text("暂无备份文件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                }
            } else {
                items(backupFiles) { file ->
                    ListItem(
                        headlineContent = { Text(file.name) },
                        supportingContent = { Text("${file.length() / 1024} KB") }
                    )
                }
            }

            // Theme section
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("外观", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
            item {
                ListItem(
                    headlineContent = { Text("深色模式") },
                    supportingContent = { Text("跟随系统设置") },
                    trailingContent = { Switch(checked = false, onCheckedChange = {}) }
                )
            }
        }
    }
}
```

---

### Task 16: Delete Old Template Files & Build Verification

**Files:**
- Delete: `app/src/main/java/com/java/myapplication/` (entire directory tree)
- Modify: `app/src/main/res/values/themes.xml` (update app theme name)

- [ ] **Step 1: Delete old package tree**

```bash
rm -rf app/src/main/java/com/java/myapplication/
rm -rf app/src/androidTest/java/com/java/myapplication/
rm -rf app/src/test/java/com/java/myapplication/
```

- [ ] **Step 2: Update themes.xml**

```xml
<resources>
    <style name="Theme.MyApplication" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 3: Build**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit and push**

```bash
git add -A
git commit -m "feat: implement note app with rich text editor and backup"
git push -u origin main
```
