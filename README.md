# PinCraft

PinCraft is a Java Swing desktop application for designing custom button pins, saving projects per user account, arranging them on printable paper layouts, and exporting them as PDF files.

It combines three main parts in one system:
- a pin editor for text and photo layers
- an account-based save/load system using SQLite
- a print layout page for preparing real-world button sheets

## Features

- user registration and login
- forgot password with email reset code
- account-based saved projects
- text layers with color, font, size, bend, stretch, rotate, transparency, and drag controls
- photo layers with upload, crop, resize, stretch, rotate, transparency, and drag controls
- button preview with visible cut and bleed guides
- print page with paper size and button size options
- drag-and-drop printable item placement
- double-click a printable item to fill all circles in the current paper preview
- PDF export for printing

## Built With

- Java
- Java Swing / AWT
- SQLite
- JDBC
- SMTP email sending

## System Description

PinCraft is designed for users who want to create printable button pin artwork in one workflow.

Instead of using one tool to design and another tool to prepare a print sheet, this application lets the user:

1. sign in or register
2. create or open a pin design
3. edit text and photo layers
4. save the design in the local database
5. place saved designs onto a print layout
6. export the final layout as a PDF

## Database

The system uses a local SQLite database.

- database file: `data/pincraft.db`
- jdbc driver: `lib/sqlite-jdbc-3.51.0.0.jar`

The database is created automatically on first run. You do not need to manually create the database file or tables.

### Tables created automatically

- `users`
  - stores registered user accounts
- `password_reset_codes`
  - stores temporary reset codes for forgot password
- `projects`
  - stores project-level information
- `project_layers`
  - stores each text/photo layer that belongs to a project

### Important note

If you delete `data/pincraft.db`, the application will create a fresh empty database the next time it starts.

## Requirements

Before running the project, make sure you have:

- JDK 17 or newer installed
- the SQLite JDBC jar inside `lib/`

Optional:
- SMTP credentials in `.env` if you want forgot password email reset to work
- Apache PDFBox jars in `lib/` if you want to use PDFBox explicitly, although the app also contains an internal PDF writer fallback

## Project Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-username/pincraft.git
cd pincraft
```

### 2. Check the library folder

Make sure this file exists:

- `lib/sqlite-jdbc-3.51.0.0.jar`

Optional PDFBox jars:

- `lib/pdfbox-3.0.3.jar`
- `lib/pdfbox-io-3.0.3.jar`
- `lib/fontbox-3.0.3.jar`

## SMTP / Email Setup

Password reset uses SMTP settings from a local `.env` file or environment variables.

Create a `.env` file in the project root like this:

```env
PINCRAFT_SMTP_HOST=smtp.gmail.com
PINCRAFT_SMTP_PORT=465
PINCRAFT_SMTP_USERNAME=your_email@gmail.com
PINCRAFT_SMTP_PASSWORD=your_app_password
PINCRAFT_SMTP_FROM=your_email@gmail.com
PINCRAFT_SMTP_FROM_NAME=PinCraft
PINCRAFT_SMTP_SECURITY=ssl
```

### Supported smtp settings

- `PINCRAFT_SMTP_HOST`
- `PINCRAFT_SMTP_PORT`
- `PINCRAFT_SMTP_USERNAME`
- `PINCRAFT_SMTP_PASSWORD`
- `PINCRAFT_SMTP_FROM`
- `PINCRAFT_SMTP_FROM_NAME`
- `PINCRAFT_SMTP_SECURITY`

### Security mode values

- `ssl`
- `starttls`
- `none`

### Important note

If SMTP is not configured:
- login and register can still work
- forgot password email reset will not work properly

## Compile the Project

### Windows

```bash
javac -cp "lib/*" -d out/build @.sources.list
```

### macOS / Linux

```bash
javac -cp "lib/*" -d out/build @.sources.list
```

## Run the Project

### Windows

```bash
java -cp "out/build;lib/*" com.pinbuttonmaker.Main
```

### macOS / Linux

```bash
java -cp "out/build:lib/*" com.pinbuttonmaker.Main
```

## First Run Behavior

On the first run, the application will:

1. create the `data/` folder if it does not exist
2. create `data/pincraft.db`
3. create the required database tables
4. open the login page

## How the Main Parts Work

### Login Page

- user login
- user registration
- password reset request
- reset code verification

### Home Page

- start a new project
- view recent saved designs
- open a saved project
- remove a saved project

### Editor Page

- add text layers
- add photo layers
- move and resize layers
- edit text appearance
- change button background
- save or load projects

### Print Page

- choose paper size
- choose button size
- drag saved designs into print slots
- double-click a saved design card to fill all visible circles in the current layout
- toggle cut lines
- export printable PDF

## Important Files

### Core

- `src/com/pinbuttonmaker/Main.java`
- `src/com/pinbuttonmaker/AppFrame.java`
- `src/com/pinbuttonmaker/AppRouter.java`
- `src/com/pinbuttonmaker/AppState.java`

### Pages

- `src/com/pinbuttonmaker/pages/LoginPage.java`
- `src/com/pinbuttonmaker/pages/HomePage.java`
- `src/com/pinbuttonmaker/pages/EditorPage.java`
- `src/com/pinbuttonmaker/pages/PrintPage.java`

### Database

- `src/com/pinbuttonmaker/db/DatabaseManager.java`
- `src/com/pinbuttonmaker/db/ProjectStorageService.java`
- `src/com/pinbuttonmaker/db/UserAuthService.java`

### UI Components

- `src/com/pinbuttonmaker/ui/components/ButtonPreviewPanel.java`
- `src/com/pinbuttonmaker/ui/components/PaperPreviewPanel.java`
- `src/com/pinbuttonmaker/ui/components/CustomButton.java`

### Utilities

- `src/com/pinbuttonmaker/util/PdfExporter.java`
- `src/com/pinbuttonmaker/util/Utils.java`

## Notes for GitHub Visitors

- this project is a desktop application, not a web application
- the database is local and file-based
- saved projects are tied to the local sqlite database
- forgot password depends on valid smtp credentials
- for correct print size, print exported PDFs at actual size / 100% scale

## Future Improvements

- stronger password hashing for production use
- cloud sync or online storage
- more print templates
- more image editing tools
- more export options
