# BozarXD - Java Bytecode Obfuscator with GUI

![alt text](https://i.imgur.com/SmgJbll.png)

## :tada: Exciting News! BozarXD 2.0.0 Coming Soon! :tada:

We are delighted to announce that BozarXD version 2.0.0 is on the horizon, bringing important enhancements to our Java bytecode obfuscation tool:

**Key Features of BozarXD 2.0.0:**

- :rocket: **Simplified User Experience:** BozarXD 2.0.0 is incredibly lightweight and user-friendly. No complex configurations are required. Whether you're a seasoned developer or a newcomer, you can start obfuscating your applications effortlessly.

- :lock: **Advanced Obfuscation Techniques:** BozarXD 2.0.0 introduces new, robust obfuscation methods that bolster the security of your code. Your applications will benefit from enhanced protection.

- :bulb: **Community-Driven Improvements:** We've listened to the valuable feedback and insights from our GitHub community to make BozarXD even better. Your input has been instrumental in shaping this release.

Stay tuned for BozarXD 2.0.0! We look forward to sharing these significant enhancements with you.

To stay updated and participate in discussions, consider joining our [Discord Community](https://discord.gg/Yp3sDQ7y6S) – a hub for sharing knowledge and support as we approach the release of BozarXD 2.0.0.

## Usage
* Download the version you want in [releases](https://github.com/Jeyor1337/BozarXD/releases) for your platform
* Run the executable.
* Done.

Let me know if obfuscator fails. Submit an [issue](https://github.com/Jeyor1337/BozarXD/issues) here.

## Currently Available Options
* Watermark
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
In such a case, use the latest version of maven to fix.
```
git clone https://github.com/Jeyor1337/BozarXD
cd BozarXD
mvn compile javafx:run
```

## Command Line Arguments
| Command | Description |
| --- | --- |
| `-input` | Target file path to obfuscate. |
| `-output` | Output path. |
| `-config` | Configuration path. |
| `-noupdate` | Disable update warnings. |
| `-console` | Application will run without GUI and obfuscation task will start immediately. |

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
