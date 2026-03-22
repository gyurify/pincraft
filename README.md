# pincraft

## JDBC Database

this system includes a JDBC SQLite database for login and register.

- JDBC driver: `lib/sqlite-jdbc-3.51.0.0.jar`
- Database file: `data/pincraft.db`
- Auth table: `users`

Run with the SQLite driver on the classpath:

```bash
java -cp "out/build;lib/*" com.pinbuttonmaker.Main
```

## PDF Export Dependency (Apache PDFBox)

PDF export uses Apache PDFBox.

1. Download these jars and place them in `lib/`:
- `pdfbox-3.0.3.jar`
- `pdfbox-io-3.0.3.jar`
- `fontbox-3.0.3.jar`

2. Compile with classpath:

```bash
javac -cp "lib/*" -d out/build @.sources.list
```

3. Run with classpath:

```bash
java -cp "out/build;lib/*" com.pinbuttonmaker.Main
```
