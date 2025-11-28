# BozarXD - Java Bytecode Obfuscator with GUI

![alt text](https://i.imgur.com/TrULZWT.png)

## Usage
* Download the version you want in [releases](https://github.com/Jeyor1337/BozarXD/releases) for your platform
* Run the executable.
* Done.

Let me know if obfuscator fails. Submit an [issue](https://github.com/Jeyor1337/BozarXD/issues) here.

## Currently Available Options
* AntiPrompt <sub><sup>(AntiAi rename & deobf)</sup></sub>
* InvokeDynamic <sub><sup>(Greatness needs no words.)</sup></sub>
* Watermark & Bad Annotation <sub><sup>(Some decompilers can be crashed idk why.)</sup></sub>
* Renamer
* Shuffler
* Decompiler crasher
* Control Flow obfuscation
* Constant obfuscation <sub><sup>(String literals and numbers)</sup></sub>
* Line number obfuscation
* Local variable obfuscation
* Inner class remover
* Source file/debug remover  

## Building
Some older maven versions have issues compiling this project.\
In such a case, use the latest version of maven (3.9+) to fix.

**Requirements:** Java 21, Maven 3.9+

```bash
git clone https://github.com/Jeyor1337/BozarXD
cd BozarXD

# Compile and run with GUI
mvn compile javafx:run

# Build JAR with dependencies
mvn clean package assembly:single

# Just compile
mvn compile
```

The built JAR will be located at `target/BozarXD-1.7.0-jar-with-auto-modules.jar`

## Command Line Arguments
| Command | Description |
| --- | --- |
| `-input` | Target JAR file path to obfuscate. |
| `-output` | Output path for obfuscated JAR. |
| `-config` | JSON configuration file path. |
| `-noupdate` | Disable update version warnings. |
| `-console` | Run without GUI, start obfuscation immediately. |
| `-init` | Generate template configuration file in current directory. |

## Command Line Mode (No GUI)

BozarXD can run in pure command line mode without JavaFX, useful for headless environments or Termux:

**1. Generate template configuration:**
```bash
java -jar BozarXD-1.7.0-jar-with-auto-modules.jar -init
```

**2. Edit the generated `bozarConfig.json` file according to your needs**

**3. Run obfuscation using the config:**
```bash
java -jar BozarXD-1.7.0-jar-with-auto-modules.jar -config bozarConfig.json -console
```

**Alternatively, specify parameters directly:**
```bash
java -jar BozarXD-1.7.0-jar-with-auto-modules.jar -input input.jar -output output.jar -console
```

**Note:** If JavaFX is not available, the application will automatically display CLI mode instructions.

## Exclusion/Inclusion Rules

BozarXD supports flexible exclusion and inclusion patterns with a **priority-based rule system**.

> **[Full Documentation](docs/EXCLUSION_RULES.md)** - Complete guide with all patterns, modes, and examples.

### Quick Reference

| Pattern | Description |
|---------|-------------|
| `com.example.Main` | Exact match |
| `com.example.*` | Single-level wildcard |
| `com.example.**` | Multi-level wildcard (includes subpackages) |
| `!com.example.**` | Include rule (whitelist mode) |
| `ClassRenamerTransformer:com.example.*` | Transformer-specific rule |

### Rule Modes

- **Blacklist (default)**: All classes obfuscated except matches
- **Whitelist**: Only `!` prefixed patterns are obfuscated
- **Mixed**: Combine both, most specific rule wins

### Quick Examples

```json
// Blacklist - exclude from obfuscation
{ "exclude": "com.example.api.**" }

// Whitelist - only obfuscate these
{ "exclude": "!com.example.core.**" }

// Mixed - include package, exclude subpackage
{ "exclude": "!com.example.**\ncom.example.util.**" }

// Transformer-specific - keep names but obfuscate code
{ "exclude": "ClassRenamerTransformer:com.example.entities.**" }
```
