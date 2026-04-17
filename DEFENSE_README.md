# pincraft defense guide

## 1. project overview

pincraft is a java swing desktop application for designing printable button pins.

the system allows a user to:
- register and log in
- save projects per account
- add text and photo layers
- edit size, rotation, stretch, bend, transparency, and position
- arrange printable items on paper
- export the layout as a pdf for printing

## 2. main purpose of the system

the goal of the system is to help users create button pin artwork and prepare it for real printing in one application.

instead of using one app for design and another app for print layout, pincraft combines:
- design editing
- account-based saving
- print layout preview
- pdf export

## 3. main technologies used

### java swing

used for the desktop user interface.

why it was used:
- built into java
- good for desktop applications
- works well for forms, buttons, dialogs, panels, and custom drawing

### java awt

used together with swing for:
- colors
- fonts
- graphics
- mouse handling
- drawing the button preview and print preview

### sqlite with jdbc

used for local database storage.

why it was used:
- lightweight
- file-based database
- easy to distribute in a desktop project
- no separate database server needed

### pdf export

used to generate printable files for the final output.

### smtp email sending

used for password reset codes.

## 4. important folders and files

### core app files

- `src/com/pinbuttonmaker/Main.java`
  - entry point of the application
- `src/com/pinbuttonmaker/AppFrame.java`
  - main window of the system
- `src/com/pinbuttonmaker/AppRouter.java`
  - handles page navigation
- `src/com/pinbuttonmaker/AppState.java`
  - stores shared app state like current user, current project, and saved projects

### page files

- `src/com/pinbuttonmaker/pages/LoginPage.java`
  - login, register, and password reset user interface
- `src/com/pinbuttonmaker/pages/HomePage.java`
  - home dashboard and recent saved designs
- `src/com/pinbuttonmaker/pages/EditorPage.java`
  - main editor where the user designs the pin
- `src/com/pinbuttonmaker/pages/PrintPage.java`
  - print layout page and pdf export

### data model files

- `src/com/pinbuttonmaker/data/ProjectData.java`
  - project-level data like project name, size, background color, and layers
- `src/com/pinbuttonmaker/data/LayerData.java`
  - one text layer or photo layer and its properties

### database files

- `src/com/pinbuttonmaker/db/DatabaseManager.java`
  - creates the sqlite database and tables
- `src/com/pinbuttonmaker/db/ProjectStorageService.java`
  - saves, loads, and deletes projects
- `src/com/pinbuttonmaker/db/UserAuthService.java`
  - handles register, login, password reset, and password change

### ui component files

- `src/com/pinbuttonmaker/ui/components/ButtonPreviewPanel.java`
  - draws the editable circular button preview
- `src/com/pinbuttonmaker/ui/components/PaperPreviewPanel.java`
  - draws the printable paper layout
- `src/com/pinbuttonmaker/ui/components/CustomButton.java`
  - shared styled button component

### utility files

- `src/com/pinbuttonmaker/util/PdfExporter.java`
  - exports the print layout to pdf
- `src/com/pinbuttonmaker/util/Utils.java`
  - common helper methods

### database file

- `data/pincraft.db`
  - sqlite database file created by the system

## 5. important imports explained

### `javax.swing.*`

used for:
- `JFrame`
- `JPanel`
- `JButton`
- `JLabel`
- `JTextField`
- `JPasswordField`
- `JScrollPane`
- `JOptionPane`

short explanation of each:

- `jframe`
  - the main application window
- `jpanel`
  - a container used to group and arrange components
- `jbutton`
  - a clickable button for actions like save, load, or print
- `jlabel`
  - used to show text or captions on the screen
- `jtextfield`
  - used for one-line text input like email or project name
- `jpasswordfield`
  - used for password input while hiding the typed characters
- `jscrollpane`
  - adds scrolling when a panel or content is bigger than the visible area
- `joptionpane`
  - shows built-in message boxes, warnings, and confirm dialogs

purpose:
- builds the visible user interface of the app

### `java.awt.*`

used for:
- `Color`
- `Font`
- `Graphics2D`
- `BorderLayout`
- `GridBagLayout`
- `Dimension`

short explanation of each:

- `color`
  - used to set the colors of text, buttons, backgrounds, and shapes
- `font`
  - used to set the style and size of text
- `graphics2d`
  - used for custom drawing like previews, guides, and shapes
- `borderlayout`
  - arranges components by top, bottom, left, right, and center
- `gridbaglayout`
  - a flexible layout manager for arranging components in rows and columns
- `dimension`
  - used to set the size of components like panels and buttons

purpose:
- handles layout, drawing, font styling, and custom rendering

### `java.awt.event.*`

used for:
- button clicks
- mouse dragging
- resizing
- page interaction

purpose:
- lets the user interact with the editor and print layout

### `java.sql.*`

used for:
- `Connection`
- `PreparedStatement`
- `ResultSet`
- `Statement`

short explanation of each:

- `connection`
  - represents the active connection between the app and the database
- `preparedstatement`
  - used to run sql queries with values safely inserted into them
- `resultset`
  - stores the rows returned by a database query
- `statement`
  - used to run simple sql commands directly

purpose:
- connects java code to sqlite
- runs sql queries safely

### `java.nio.file.*`

used in database setup for:
- creating the `data` folder
- resolving the database file path

### `java.time.*`

used for:
- timestamps
- password reset expiration time

### `javax.imageio.ImageIO`

used for:
- reading and writing images
- storing photo layer images as bytes in the database

### `org.sqlite.JDBC`

used as the sqlite driver.

note:
- this comes from the external jar file in `lib/sqlite-jdbc-3.51.0.0.jar`

## 6. database explanation

the project uses sqlite as a local database.

### database file

- `data/pincraft.db`

### tables

#### `users`

stores:
- user id
- email
- hashed password
- creation time

#### `password_reset_codes`

stores:
- reset code hash
- expiration time
- related user id

purpose:
- allows forgot password using a temporary reset code

#### `projects`

stores:
- project id
- user id
- project name
- button diameter
- background color
- timestamps

#### `project_layers`

stores:
- layer order
- layer type
- visibility
- text content and style
- photo image bytes
- offsets
- scale
- rotation
- stretch
- transparency
- bend
- crop values

purpose:
- saves the exact editor state of every project

## 7. how saving works

1. user clicks save in the editor
2. the current `ProjectData` object is copied
3. `AppState` sends it to `ProjectStorageService`
4. the project header is stored in the `projects` table
5. all layers are stored in the `project_layers` table
6. saved projects are refreshed and shown on the home page

## 8. how loading works

1. user clicks load or open
2. the project id is passed to `ProjectStorageService`
3. the project row is loaded
4. related layers are loaded in order
5. a new `ProjectData` object is rebuilt
6. that project becomes the current project in the editor

## 9. how login and reset password work

### register

1. user enters email and password
2. email is normalized
3. password is hashed with sha-256
4. account is inserted into the `users` table

### login

1. user enters email and password
2. input is normalized
3. entered password is hashed
4. stored hash is compared
5. if matched, user is authenticated

### forgot password

1. user requests password reset
2. a 6-digit reset code is generated
3. the code is hashed and stored with expiration time
4. the code is sent through smtp email
5. user enters the code and new password
6. if valid and not expired, the password is updated

## 10. how the editor works

the editor works around a `ProjectData` object that contains multiple `LayerData` objects.

### text layers

text layers support:
- text content
- font family
- font size
- color
- bend
- rotation
- size
- stretch
- transparency
- drag positioning

### photo layers

photo layers support:
- upload image
- crop image
- resize
- move
- stretch
- rotation
- transparency

### preview panel

`ButtonPreviewPanel` is responsible for:
- drawing the circular pin
- drawing text and photo layers
- hit detection
- drag movement
- resize handles
- selection guides

## 11. how the print page works

the print page uses `PaperPreviewPanel`.

it handles:
- paper size
- button size
- layout spacing
- cut line visibility
- drag and drop of saved items into print slots
- removing assigned items
- export snapshot generation

## 12. how pdf export works

1. the print page builds an export snapshot
2. the snapshot contains paper settings, slot positions, and preview images
3. `PdfExporter` writes the output to pdf
4. the user can print the file at actual size

## 13. why sqlite was chosen

possible defense answer:

sqlite was chosen because the project is a desktop application and does not need a separate server. it is lightweight, fast for local storage, and easier to deploy for school and prototype use.

## 14. why java swing was chosen

possible defense answer:

java swing was chosen because it is stable for desktop applications, easy to run in a school environment, and supports custom drawing for the circular button preview and print layout.

## 15. security notes

- passwords are not stored as plain text
- passwords are hashed before saving
- reset codes are also hashed
- prepared statements are used for sql queries
- reset codes expire after a time window

note:
- for a production-level system, stronger password hashing such as bcrypt or argon2 would be better than plain sha-256

## 16. limitations you can mention honestly

- the project is a desktop system, not a cloud web system
- the database is local to the machine
- the current security design is enough for a school project, but production security could still be improved
- smtp reset depends on valid email configuration in `.env`
- print accuracy still depends on printer settings using actual size or 100% scaling

## 17. possible defense q and a

### q1. what is pincraft?

answer:
pincraft is a desktop application for designing custom button pins, saving projects per user account, arranging them on printable paper layouts, and exporting them as pdf.

### q2. what problem does your system solve?

answer:
it solves the problem of using separate tools for designing a button pin and preparing it for print. our system combines both in one workflow.

### q3. why did you use java swing?

answer:
we used java swing because it is suitable for desktop-based systems and it allowed us to build custom previews, forms, dialogs, and drag interactions in one application.

### q4. why did you use sqlite?

answer:
we used sqlite because the project is local and desktop-based. sqlite is lightweight, easy to set up, and does not require a separate database server.

### q5. what is jdbc in your project?

answer:
jdbc is the bridge between our java code and the sqlite database. it lets the system open database connections and run sql queries.

### q6. what is the difference between `projects` and `project_layers`?

answer:
the `projects` table stores one record per project, while `project_layers` stores all the individual text and photo layers that belong to that project.

### q7. how does the system save the design exactly?

answer:
the system saves the main project information first, then saves every layer with its order, type, text or image data, transformations, and visibility settings.

### q8. how do you handle password security?

answer:
the system hashes passwords before storing them, so plain text passwords are not saved in the database.

### q9. how does forgot password work?

answer:
the system generates a temporary reset code, stores its hash with an expiration time, sends the code by email, then verifies the code before changing the password.

### q10. what happens if the database is not available?

answer:
the system checks database availability first and returns a clear error message if the database or jdbc driver is not ready.

### q11. why do you use prepared statements?

answer:
prepared statements help keep sql queries safer and cleaner by separating the sql structure from user input.

### q12. what is `AppState` used for?

answer:
`AppState` stores the shared runtime data of the application such as the current user, current project, saved projects, and service objects.

### q13. what is `AppRouter` used for?

answer:
`AppRouter` is responsible for switching between pages like login, home, editor, and print using a card layout.

### q14. what is the role of `ButtonPreviewPanel`?

answer:
it is the custom drawing panel that shows the live circular button preview in the editor and handles layer selection, movement, and resizing.

### q15. what is the role of `PaperPreviewPanel`?

answer:
it is the custom drawing panel that shows the printable paper layout and manages where button designs are placed before export.

### q16. how do images get stored in the database?

answer:
photo layers are converted into byte arrays and stored as blob data in the `project_layers` table.

### q17. what are the strengths of your system?

answer:
the system has one complete workflow from login to design to print, uses persistent storage, supports multiple layer types, and has direct pdf export.

### q18. what are the limitations of your system?

answer:
it is mainly intended for local desktop use, and some production-level concerns like stronger password hashing and cloud sync are outside the current scope.

### q19. how is responsiveness handled in the ui?

answer:
the system uses java swing layouts and responsive size adjustments so the cards, panels, and controls adapt when the window size changes.

### q20. what would you improve in the future?

answer:
future improvements could include stronger password hashing, cloud backup, more export options, more pin templates, and more advanced image editing.

## 18. short demo flow for defense

you can present the system in this order:

1. open the app
2. show login or register
3. log in
4. show recent designs in home page
5. open editor
6. add text layer
7. add photo layer
8. edit size, bend, stretch, and position
9. save the project
10. open print page
11. drag saved items into print slots
12. export pdf

## 19. short explanation you can say to the panel

"our system is a java swing desktop application that helps users design button pins, save them per user account using sqlite through jdbc, and prepare print-ready pdf layouts in one workflow. the main strengths of the system are local persistence, editable text and photo layers, and built-in print preparation."
