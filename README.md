# pincraft

## Accounts And Saved Designs

The app now includes a JDBC-backed SQLite database for:

- login and register
- password reset codes
- saved designs scoped to the signed-in account

- JDBC driver: `lib/sqlite-jdbc-3.51.0.0.jar`
- Database file: `data/pincraft.db`
- Main tables: `users`, `password_reset_codes`, `projects`, `project_layers`

Run with the SQLite driver on the classpath:

```bash
java -cp "out/build;lib/*" com.pinbuttonmaker.Main
```

## Forgot Password Email Setup

`Forgot password?` now sends a reset code to the account email, but you need to configure SMTP first.

You can use either environment variables or JVM properties:

- `PINCRAFT_SMTP_HOST` or `-Dpincraft.smtp.host=...`
- `PINCRAFT_SMTP_PORT` or `-Dpincraft.smtp.port=...`
- `PINCRAFT_SMTP_SECURITY` or `-Dpincraft.smtp.security=ssl|starttls|none`
- `PINCRAFT_SMTP_USERNAME` or `-Dpincraft.smtp.username=...`
- `PINCRAFT_SMTP_PASSWORD` or `-Dpincraft.smtp.password=...`
- `PINCRAFT_SMTP_FROM` or `-Dpincraft.smtp.from=...`
- `PINCRAFT_SMTP_FROM_NAME` or `-Dpincraft.smtp.fromName=PinCraft`

Example with Gmail app-password SMTP:

```bash
java ^
  -Dpincraft.smtp.host=smtp.gmail.com ^
  -Dpincraft.smtp.port=465 ^
  -Dpincraft.smtp.security=ssl ^
  -Dpincraft.smtp.username=your-email@gmail.com ^
  -Dpincraft.smtp.password=your-app-password ^
  -Dpincraft.smtp.from=your-email@gmail.com ^
  -Dpincraft.smtp.fromName=PinCraft ^
  -cp "out/build;lib/*" com.pinbuttonmaker.Main
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
