# BozarXD - Java Bytecode Obfuscator with GUI

![alt text](https://i.imgur.com/SmgJbll.png)

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

## Exclusion/Inclusion Syntax

BozarXD supports flexible exclusion and inclusion patterns for classes, packages, and transformers.

### Blacklist Mode (Default)
By default, all classes are obfuscated except those matching exclusion patterns:

```
com.example.Main                    # Exclude specific class
com.example.util.*                  # Exclude direct classes in package
com.example.util.**                 # Exclude package and all subpackages
*ClassName                          # Exclude classes ending with ClassName
**ClassName                         # Exclude classes ending with ClassName (any package)
Rename:com.example.*                # Exclude only from Rename transformer
```

### Whitelist Mode (Inclusion)
When **any** rule starts with `!`, BozarXD switches to whitelist mode where:
- All classes are **excluded by default**
- Only classes matching `!` patterns are **included** (obfuscated)

```
!com.example.core.**                # Only obfuscate com.example.core package
!com.example.Main                   # Only obfuscate Main class
!Rename:com.example.*               # Only rename classes in com.example package
```

### Configuration Example

**Blacklist mode** - Obfuscate everything except utilities:
```json
{
  "exclude": "com.example.util.**\ncom.example.Main"
}
```

**Whitelist mode** - Only obfuscate core package:
```json
{
  "exclude": "!com.example.core.**"
}
```

**Mixed whitelist** - Only obfuscate specific classes:
```json
{
  "exclude": "!com.example.core.Engine\n!com.example.core.Processor\n!com.example.util.Helper"
}
```
