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

## Exclusion/Inclusion Syntax

BozarXD supports flexible exclusion and inclusion patterns with a **priority-based rule system** that allows mixing include and exclude rules.

### Pattern Syntax

**Wildcard patterns:**
```
com.example.Main                    # Exact match - specific class
com.example.util.*                  # Single-level wildcard - direct classes in package only
com.example.util.**                 # Multi-level wildcard - package and all subpackages
*ClassName                          # Suffix wildcard - classes ending with ClassName
**ClassName                         # Suffix wildcard - classes ending with ClassName (any package)
```

**Transformer-specific rules:**
```
ClassRenamerTransformer:com.example.*    # Apply rule only to ClassRenamerTransformer
FieldRenamerTransformer:com.example.**   # Apply rule only to FieldRenamerTransformer
```

### Rule Modes

#### 1. Blacklist Mode (Default)
By default, all classes are **obfuscated** except those matching exclusion patterns:

```
com.example.Main                    # Exclude this class from obfuscation
com.example.util.**                 # Exclude entire util package
```

#### 2. Whitelist Mode
When **any** rule starts with `!`, all classes are **excluded by default**, and only `!` patterns are obfuscated:

```
!com.example.core.**                # Only obfuscate core package
!com.example.Main                   # Only obfuscate Main class
```

#### 3. Mixed Mode (New)
You can now **mix include (`!`) and exclude rules** with automatic priority resolution:

```
!com.example.**                     # Include entire example package
com.example.util.**                 # But exclude util subpackage
!com.example.util.CryptoHelper      # But re-include this specific class (exact match wins)
```

### Rule Priority System

When multiple rules match a class, the **most specific rule wins**:

**Priority levels (highest to lowest):**
1. **Exact match** - `com.example.Main` (score: 500 + length)
2. **Single-level wildcard** - `com.example.*` (score: 400 + length)
3. **Multi-level wildcard** - `com.example.**` (score: 300 + length)
4. **Suffix wildcards** - `**Test`, `*Test` (score: 100-200 + length)

**Additional factors:**
- Longer patterns have higher priority (more specific)
- Transformer-specific rules get +1000 bonus score

### Configuration Examples

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

**Mixed mode** - Include package but exclude subpackage:
```json
{
  "exclude": "!com.example.**\ncom.example.util.**"
}
```
Result: All `com.example.*` classes are obfuscated except `com.example.util.*`

**Mixed mode with exact override** - Fine-grained control:
```json
{
  "exclude": "!com.myapp.**\ncom.myapp.debug.**\n!com.myapp.debug.Logger"
}
```
Result:
- `com.myapp.Main` - ✓ Obfuscated (included by `!com.myapp.**`)
- `com.myapp.debug.DebugUtils` - ✗ Not obfuscated (excluded by `com.myapp.debug.**`)
- `com.myapp.debug.Logger` - ✓ Obfuscated (exact match `!` has highest priority)

**Transformer-specific mixed rules**:
```json
{
  "exclude": "!com.example.**\nClassRenamerTransformer:com.example.entities.**"
}
```
Result: Everything in `com.example.**` is obfuscated, but entities are not renamed (other transformers still apply)
