# BozarXD Exclusion Rules Guide

This document provides comprehensive documentation for BozarXD's exclusion/inclusion rule system, allowing fine-grained control over which classes and packages are obfuscated.

## Table of Contents

- [Basic Syntax](#basic-syntax)
- [Pattern Types](#pattern-types)
- [Rule Modes](#rule-modes)
- [Transformer-Specific Rules](#transformer-specific-rules)
- [Priority System](#priority-system)
- [Configuration Examples](#configuration-examples)

---

## Basic Syntax

Rules are defined in the `exclude` field of `bozarConfig.json` as a multi-line string (lines separated by `\n`):

```json
{
  "exclude": "rule1\nrule2\nrule3"
}
```

**Rule format:**
```
[!][TransformerName:]pattern
```

| Component | Description |
|-----------|-------------|
| `!` | Optional. Marks this as an **include** rule (whitelist) |
| `TransformerName:` | Optional. Apply rule only to specific transformer(s) |
| `pattern` | Class/package pattern with optional wildcards |

---

## Pattern Types

### Exact Match
Matches a specific class exactly.

```
com.example.Main
com.example.util.StringHelper
```

### Single-Level Wildcard (`*`)
Matches classes directly in a package (not subpackages).

```
com.example.*          # Matches com.example.Main, com.example.Utils
                       # Does NOT match com.example.util.Helper
```

### Multi-Level Wildcard (`**`)
Matches a package and all its subpackages recursively.

```
com.example.**         # Matches com.example.Main
                       # Matches com.example.util.Helper
                       # Matches com.example.util.sub.Deep
```

### Suffix Wildcards
Matches classes ending with a specific name.

```
*Helper                # Matches com.example.StringHelper (single-level)
**Helper               # Matches any.package.depth.StringHelper (multi-level)
```

---

## Rule Modes

### 1. Blacklist Mode (Default)

When no rules start with `!`, all classes are **obfuscated by default**. Rules define what to **exclude**.

```
com.example.api.**
com.example.Main
```

**Result:** Everything is obfuscated except `com.example.api.**` and `com.example.Main`.

### 2. Whitelist Mode

When **any** rule starts with `!`, all classes are **excluded by default**. Only `!` patterns are obfuscated.

```
!com.example.core.**
!com.example.Main
```

**Result:** Only `com.example.core.**` and `com.example.Main` are obfuscated.

### 3. Mixed Mode

Combine include (`!`) and exclude rules for precise control. The most specific rule wins.

```
!com.example.**
com.example.util.**
!com.example.util.CryptoHelper
```

**Result:**
- `com.example.Main` - Obfuscated (matched by `!com.example.**`)
- `com.example.util.Logger` - NOT obfuscated (matched by `com.example.util.**`)
- `com.example.util.CryptoHelper` - Obfuscated (exact match has highest priority)

---

## Transformer-Specific Rules

Apply rules to specific transformers using the `TransformerName:pattern` syntax.

### Available Transformers

| Group | Transformer Names |
|-------|-------------------|
| **Renamer** | `ClassRenamerTransformer`, `FieldRenamerTransformer`, `MethodRenamerTransformer` |
| **Info Removal** | `LocalVariableTransformer`, `LineNumberTransformer`, `SourceFileTransformer` |
| **Structure** | `InnerClassTransformer`, `ShuffleTransformer`, `BadAnnoTransformer` |
| **Constants** | `ConstantTransformer` |
| **Advanced** | `AntiPromptTransformer`, `ParamObfTransformer`, `InvokeDynamicTransformer` |
| **Control Flow** | `LightControlFlowTransformer`, `HeavyControlFlowTransformer`, `SuperControlFlowTransformer`, `UltraControlFlowTransformer` |
| **Watermark** | `CrasherTransformer`, `DummyClassTransformer`, `TextInsideClassTransformer`, `UnusedStringTransformer`, `ZipCommentTransformer` |

### Transformer-Specific Examples

**Exclude from renaming only:**
```
ClassRenamerTransformer:com.example.entities.**
FieldRenamerTransformer:com.example.entities.**
MethodRenamerTransformer:com.example.entities.**
```

**Result:** Entity classes keep original names, but still get control flow obfuscation, constant encryption, etc.

**Exclude from constant obfuscation:**
```
ConstantTransformer:com.example.config.**
```

**Result:** Config classes keep readable strings, but get renamed and flow-obfuscated.

**Exclude from control flow only:**
```
SuperControlFlowTransformer:com.example.performance.**
HeavyControlFlowTransformer:com.example.performance.**
```

**Result:** Performance-critical classes skip heavy control flow changes.

### Mixed Mode with Transformer Rules

```
!com.example.**
ClassRenamerTransformer:com.example.api.**
```

**Result:**
- All `com.example.**` classes are obfuscated
- But `com.example.api.**` classes are NOT renamed (other transformers still apply)

---

## Priority System

When multiple rules match a class, the **most specific rule wins**.

### Priority Levels

| Priority | Type | Base Score | Example |
|----------|------|------------|---------|
| 1 (Highest) | Exact match | 500 | `com.example.Main` |
| 2 | Single-level wildcard | 400 | `com.example.*` |
| 3 | Multi-level wildcard | 300 | `com.example.**` |
| 4 | Suffix wildcard (`*`) | 200 | `*Helper` |
| 5 (Lowest) | Suffix wildcard (`**`) | 100 | `**Helper` |

### Score Calculation

```
Final Score = Base Score + Pattern Length + Transformer Bonus

Transformer Bonus = +1000 (if transformer-specific)
Pattern Length = characters excluding wildcards
```

### Priority Examples

**Example 1:** Class `com.example.util.StringHelper`

| Rule | Score | Matches |
|------|-------|---------|
| `com.example.util.StringHelper` | 500 + 28 = 528 | Yes (exact) |
| `com.example.util.*` | 400 + 16 = 416 | Yes |
| `com.example.**` | 300 + 11 = 311 | Yes |
| `**Helper` | 100 + 6 = 106 | Yes |

**Winner:** Exact match rule with score 528

**Example 2:** Transformer-specific rule

| Rule | Score |
|------|-------|
| `com.example.**` | 311 |
| `ClassRenamerTransformer:com.example.**` | 1311 |

**Winner:** Transformer-specific rule with +1000 bonus

---

## Configuration Examples

### Basic Blacklist

Obfuscate everything except API and Main:

```json
{
  "exclude": "com.example.api.**\ncom.example.Main"
}
```

### Basic Whitelist

Only obfuscate core package:

```json
{
  "exclude": "!com.example.core.**"
}
```

### Complex Mixed Mode

```json
{
  "exclude": "!com.myapp.**\ncom.myapp.debug.**\ncom.myapp.generated.**\n!com.myapp.debug.SecureLogger"
}
```

**Result:**
| Class | Obfuscated | Reason |
|-------|------------|--------|
| `com.myapp.Main` | Yes | Matched `!com.myapp.**` |
| `com.myapp.core.Engine` | Yes | Matched `!com.myapp.**` |
| `com.myapp.debug.DebugUtils` | No | Matched `com.myapp.debug.**` |
| `com.myapp.debug.SecureLogger` | Yes | Exact match has highest priority |
| `com.myapp.generated.Proto` | No | Matched `com.myapp.generated.**` |

### Selective Transformer Exclusion

Keep API class names but obfuscate everything else:

```json
{
  "exclude": "ClassRenamerTransformer:com.example.api.**\nFieldRenamerTransformer:com.example.api.**\nMethodRenamerTransformer:com.example.api.**"
}
```

### Full Example Config

```json
{
  "input": "input.jar",
  "output": "output.jar",
  "exclude": "!com.myapp.**\ncom.myapp.util.**\nClassRenamerTransformer:com.myapp.entities.**\nConstantTransformer:com.myapp.config.**",
  "libraries": [
    "libs/dependency.jar"
  ],
  "options": {
    "rename": "Alphabet",
    "controlFlowObfuscation": "Heavy",
    "constantObfuscation": "Flow"
  }
}
```

**Behavior:**
- Only `com.myapp.**` is obfuscated (whitelist mode)
- Except `com.myapp.util.**` which is excluded
- Entity classes keep their original names
- Config classes keep readable strings

---

## Tips

1. **Start simple** - Begin with basic blacklist/whitelist, add complexity as needed
2. **Test incrementally** - Verify each rule works before adding more
3. **Use exact matches** - When you need to override a wildcard rule
4. **Transformer-specific rules** - Perfect for keeping reflection-dependent code working
5. **Check priority** - Remember longer patterns and exact matches win
