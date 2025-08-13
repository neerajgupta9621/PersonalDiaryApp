# Personal Diary App (Java Swing)

A polished, single-file Java Swing diary application that saves each day's entry as a text file under a `diary/` folder.
Includes find (Ctrl/Cmd+F), word-count, previous/next day navigation, save (Ctrl/Cmd+S), and a clean UI suitable for interviews/demo.

## Features
- Per-day entry saved as `diary/YYYY-MM-DD.txt`
- Keyboard shortcuts: Save (Ctrl/Cmd+S), Find (Ctrl/Cmd+F)
- Previous / Next day navigation
- Word count & character count
- Confirm unsaved changes on date change or exit
- Opens the diary folder from the menu

## Run
```bash
javac src/PersonalDiaryApp.java
java -cp src PersonalDiaryApp
```

> Tested with Java 17+. Should work with Java 8+.

## Project Structure
```
personal-diary-app/
├─ src/
│  └─ PersonalDiaryApp.java
├─ .gitignore
├─ LICENSE
└─ README.md
```

## License
MIT
