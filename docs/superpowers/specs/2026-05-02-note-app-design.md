# Note App — Design Spec

## Overview

A modern Android note-taking application built with Jetpack Compose and Material Design 3. Features rich text editing, folder/tag organization, waterfall layout, and local backup/export.

- **Package**: `com.faster.note`
- **GitHub**: `dac114514/android-note`
- **Architecture**: Lightweight MVVM (Room + ViewModel + Navigation Compose)
- **Min SDK**: 24, Target SDK: 35
- **UI**: Jetpack Compose + Material Design 3

---

## Routes & Navigation

| Route            | Screen            | Description                    |
|------------------|--------------------|--------------------------------|
| `notes`          | NoteListScreen     | Home — masonry waterfall list  |
| `folders`        | FolderScreen       | Manage folders and tags        |
| `note/{id}`      | NoteEditScreen     | Edit a note (read/write)       |
| `note/new`       | NoteEditScreen     | Create a new note              |
| `search`         | SearchScreen       | Full-text search               |
| `settings`       | SettingsScreen     | Backup, export, theme          |

Bottom navigation bar with three tabs: Notes, Folders, Settings.

---

## Data Model

### Note
| Field       | Type         | Description              |
|-------------|--------------|--------------------------|
| id          | Long (PK)    | Auto-generate            |
| title       | String       | Note title               |
| content     | String       | Rich text (HTML format)  |
| folderId    | Long? (FK)   | Belongs to folder        |
| createdAt   | Long         | Creation timestamp       |
| updatedAt   | Long         | Last update timestamp    |
| isFavorite  | Boolean      | Starred flag             |
| color       | Int?         | Cover color (argb)       |

### Folder
| Field  | Type       | Description          |
|--------|------------|----------------------|
| id     | Long (PK)  | Auto-generate        |
| name   | String     | Folder name          |
| color  | Int        | Display color        |

### Tag
| Field  | Type         | Description   |
|--------|--------------|---------------|
| id     | Long (PK)    | Auto-generate |
| name   | String (UQ)  | Tag name      |

### NoteTagCrossRef
| Field  | Type   |
|--------|--------|
| noteId | Long (FK → Note) |
| tagId  | Long (FK → Tag)  |

---

## UI Design

### Note List (Home)
- **Layout**: Staggered waterfall grid (masonry) — two columns with height-varying cards
- **Card content**: Title, content preview (2 lines), folder label, timestamp, tag chips
- **Card actions**: Tap to edit, long-press for context menu (delete, favorite, move)
- **FAB**: Create new note
- **Filter bar**: Filter by folder, tag, or search

### Note Edit
- **Title field**: Top of screen, large text, transparent background
- **Rich text toolbar**: Floating toolbar above keyboard — bold, italic, underline, strikethrough, heading (H1/H2/H3), bullet list, ordered list, alignment
- **Content area**: Full-screen scrollable rich text editor
- **Metadata panel**: Below editor — folder selector, tag selector, color picker, delete button
- **Auto-save**: Debounced save on content change (500ms)
- **Back navigation**: Save and return to list

### Folder Screen
- **Folder list**: Cards with folder name, note count, color indicator
- **Tag cloud**: Below folders, scrollable tag chips
- **Actions**: Create/edit/delete folder, rename tags

### Settings
- **Export backup**: Export all or per-folder notes as ZIP
- **Import backup**: Select ZIP file to restore
- **Theme toggle**: Light / Dark / System
- **About**: Version info

---

## Rich Text Editor Implementation

**Technology**: Custom Compose implementation using `AnnotatedString` / `SpanStyle` / `ParagraphStyle`.

### Storage format
- **Internal**: Custom Span model (open for extension)
- **Persistence**: HTML string stored in Room
- **Persistence logic**: In-memory spans ← [save →] HTML → Room
  Room ← [load →] HTML → [parse →] Spans → render

### Supported formats (v1.0)
- Bold, Italic, Underline, Strikethrough
- Headings H1/H2/H3
- Ordered & unordered lists
- Left/Center/Right alignment

### Toolbar
- Row of icon buttons above keyboard
- Toggle state reflects current selection format
- Each format button highlights when active

---

## Data Layer

### Room Database
- `NoteDao`: CRUD, filter by folder, filter by tag (via cross-ref join), FTS search, sort by time/favorite
- `FolderDao`: CRUD, count notes per folder
- `TagDao`: CRUD, note-tag association management
- `NoteFts` (FTS4 virtual table): auto-sync with Note for full-text search

### Backup / Export
- **Format**: ZIP file containing `notes.json` (metadata array) and `notes/` directory with individual `.html` files
- **Output path**: `Documents/NoteApp/Backups/`
- **Import**: File picker → parse ZIP → upsert into Room (with merge/overwrite option)

---

## Package Structure

```
com.faster.note/
├── MainActivity.kt
├── NoteApp.kt                    (Application class)
├── data/
│   ├── db/
│   │   ├── NoteDatabase.kt
│   │   ├── dao/
│   │   │   ├── NoteDao.kt
│   │   │   ├── FolderDao.kt
│   │   │   └── TagDao.kt
│   │   ├── entity/
│   │   │   ├── NoteEntity.kt
│   │   │   ├── FolderEntity.kt
│   │   │   ├── TagEntity.kt
│   │   │   └── NoteTagCrossRef.kt
│   │   └── converter/
│   │       └── DateConverters.kt
│   ├── model/
│   │   ├── Note.kt
│   │   ├── Folder.kt
│   │   └── Tag.kt
│   └── repository/
│       ├── NoteRepository.kt
│       ├── FolderRepository.kt
│       └── TagRepository.kt
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── notes/
│   │   ├── NoteListScreen.kt
│   │   ├── NoteListViewModel.kt
│   │   ├── components/
│   │   │   ├── NoteCard.kt
│   │   │   └── NoteFilterBar.kt
│   ├── editor/
│   │   ├── NoteEditScreen.kt
│   │   ├── NoteEditViewModel.kt
│   │   ├── RichTextEditor.kt
│   │   ├── FormatToolbar.kt
│   │   └── model/
│   │       ├── SpanModel.kt
│   │       └── HtmlConverter.kt
│   ├── folders/
│   │   ├── FolderScreen.kt
│   │   ├── FolderViewModel.kt
│   │   └── components/
│   │       ├── FolderCard.kt
│   │       └── TagCloud.kt
│   ├── search/
│   │   ├── SearchScreen.kt
│   │   └── SearchViewModel.kt
│   └── settings/
│       ├── SettingsScreen.kt
│       └── SettingsViewModel.kt
└── util/
    ├── BackupManager.kt
    └── DateUtils.kt
```
