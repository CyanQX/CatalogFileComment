# Catalog File Comments — Plugin Module

Gradle project of the IntelliJ Platform plugin. Full documentation lives in the [root README](../README.md).

## Module Architecture

```text
┌─────────────────────────────────────────┐
│           IntelliJ IDEA UI              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐ │
│  │ Project │  │ Comment │  │ Manager │ │
│  │  View   │  │ Dialog  │  │  Panel  │ │
│  └────┬────┘  └────┬────┘  └────┬────┘ │
│       └─────────────┴─────────────┘     │
│                   │                     │
│       ┌───────────┴───────────┐         │
│       │   FileCommentConfig   │         │
│       │       Service         │         │
│       └───────────┬───────────┘         │
│                   │                     │
│       ┌───────────┴───────────┐         │
│       │   CloudRuleService    │         │
│       │  (Memory→Disk→Cloud)  │         │
│       └───────────────────────┘         │
└─────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

- **Kotlin 2.1** — primary language
- **IntelliJ Platform SDK** (IC 2024.3, `sinceBuild 243`) — plugin framework
- **PSI (Program Structure Interface)** — code structure analysis
- **Gson** — JSON parsing
- **GitHub Gist** — cloud rule storage

---

## 🚧 Build

```bash
./gradlew runIde       # start a sandbox IDE with the plugin
./gradlew buildPlugin  # build the distributable ZIP
```

---

## 📄 License

[MIT](../LICENSE)

---

<p align="center">
  <i>Let every filename carry its mission. Happy Coding! 🎉</i>
</p>