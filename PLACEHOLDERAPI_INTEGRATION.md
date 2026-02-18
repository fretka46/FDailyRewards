# PlaceholderAPI Integration Guide

## Changes Made

This guide explains the changes made to integrate PlaceholderAPI (PAPI) into the FDailyRewards plugin.

### 1. Updated `build.gradle`

Added the PlaceholderAPI repository and dependency:

```gradle
repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = "https://repo.papermc.io/repository/maven-public/"
    }
    maven {
        name = "placeholderapi"
        url = "https://repo.extendedclip.com/content/repositories/placeholderapi/"
    }
    maven {
        name = "jitpack"
        url = "https://jitpack.io"  // Fallback repository
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
}
```

**Note:** PlaceholderAPI is added as `compileOnly` because it's a runtime dependency provided by the server. It should NOT be shadowed into your plugin JAR.

### 2. Updated `plugin.yml`

Added PlaceholderAPI as a soft dependency:

```yaml
softdepend: [PlaceholderAPI]
```

This ensures that if PlaceholderAPI is present on the server, your plugin will load after it, allowing proper integration.

### 3. Created `PlaceholderAPIIntegration.java`

A utility class for working with PlaceholderAPI has been created at:
`src/main/java/com/fretka46/fDailyRewards/Utils/PlaceholderAPIIntegration.java`

This class provides:
- Runtime detection of PlaceholderAPI availability
- Helper method to parse placeholders in messages
- Safe integration that won't break if PlaceholderAPI is not installed

## What You Need to Do

### Step 1: Reload Gradle Project in Your IDE

**This is the most important step!** The IDE needs to re-index the project with the new dependencies.

#### IntelliJ IDEA:
1. Open the Gradle tool window (View → Tool Windows → Gradle)
2. Click the "Reload All Gradle Projects" button (🔄 icon)
3. Wait for the sync to complete

#### Eclipse:
1. Right-click on the project
2. Select "Gradle" → "Refresh Gradle Project"
3. Wait for the sync to complete

#### VS Code:
1. Open the Command Palette (Ctrl+Shift+P or Cmd+Shift+P)
2. Type "Java: Clean Java Language Server Workspace"
3. Reload the window

### Step 2: Verify Import Works

After reloading, you should be able to import PlaceholderAPI classes:

```java
import me.clip.placeholderapi.PlaceholderAPI;
```

The IDE should no longer show this import in red.

### Step 3: Initialize PlaceholderAPI Support

In your main plugin class (`FDailyRewards.java`), add the initialization call in `onEnable()`:

```java
@Override
public void onEnable() {
    // ... existing code ...
    
    // Initialize PlaceholderAPI integration
    PlaceholderAPIIntegration.initialize();
}
```

### Step 4: Use Placeholders in Messages

When you need to parse placeholders in messages, use:

```java
String message = "Your balance: %vault_eco_balance%";
String parsed = PlaceholderAPIIntegration.parsePlaceholders(player, message);
```

## Common Issues and Solutions

### Issue: "Could not resolve placeholderapi"

**Solution:** Make sure you have internet access and the repository URL is reachable. Try:
```bash
./gradlew build --refresh-dependencies
```

### Issue: Import still shows as red after Gradle sync

**Solution:**
1. Try invalidating caches: File → Invalidate Caches / Restart
2. Delete the `.gradle` and `.idea` folders, then reimport the project
3. Verify that the dependency was actually downloaded by checking the External Libraries in your IDE

### Issue: "No cached version available for offline mode"

**Solution:** Make sure you're online and run:
```bash
./gradlew build --no-daemon
```

## Verification

To verify everything is set up correctly:

1. Build the project: `./gradlew build`
2. Check that the build succeeds without errors
3. Deploy the plugin to a test server with PlaceholderAPI installed
4. Check the server logs for: "PlaceholderAPI found! Placeholder support enabled."

## Additional Resources

- [PlaceholderAPI Wiki](https://github.com/PlaceholderAPI/PlaceholderAPI/wiki)
- [PlaceholderAPI Maven Repository](https://repo.extendedclip.com/content/repositories/placeholderapi/)
- [List of Available Placeholders](https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/Placeholders)
