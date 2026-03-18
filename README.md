# Code Review Annotator

IntelliJ plugin for annotating code diffs with review comments. 
Comments are ready to paste into an AI agent.

Inspired by [tuicr](https://github.com/agavra/tuicr).

<video src="https://github.com/user-attachments/assets/a2ba847d-1220-45ed-91dc-4010ac04e092" controls width="600"></video>

## Install

### From source

Requires [Nix](https://nixos.org/) with flakes enabled, or JDK 21+ and Gradle.

```sh
# With Nix (recommended)
direnv allow   # or: nix develop
./gradlew buildPlugin

# Without Nix
./gradlew buildPlugin
```

The plugin zip is generated at `build/distributions/code-review-annotator-0.1.0.zip`.

In GoLand/IntelliJ: **Settings → Plugins → ⚙️ → Install Plugin from Disk...** → select the zip.

### Development sandbox

```sh
./gradlew runIde
```

This launches a sandboxed GoLand instance with the plugin loaded.

## Usage

### 1. Add comments

There are three scopes for comments:

| Scope      | How to add                                                                                 |
|------------|--------------------------------------------------------------------------------------------|
| **Review** | **Code Review** tool window (bottom panel) → toolbar → **Add Review Comment**              |
| **File**   | Right-click a file in the **Commit** view → **Add File Comment...**                        |
| **Line**   | Right-click in the editor → **Add Review Comment Here...**, or <kbd>Ctrl+Shift+Alt+C</kbd> |

Each comment has a type:

- **ISSUE**: a problem that needs to be fixed
- **SUGGESTION**: an improvement to consider
- **NOTE**: an observation

### 2. Browse comments

Open the **Code Review** tool window (bottom panel). All comments are listed, sorted by scope → file → line.

Double-click a comment to navigate to its file and line in the editor.

### 3. Export to clipboard

Click **Export Comments to Clipboard** in the tool window toolbar, or press <kbd>Ctrl+Shift+Alt+E</kbd>.

This copies all comments as numbered markdown:

```
I reviewed your code and have the following comments. Please address them.

Comment types: ISSUE (problems to fix), SUGGESTION (improvements), NOTE (observations)

1. **[SUGGESTION]** `src/auth.go:42` - Consider adding unit tests
2. **[ISSUE]** `src/handler.go:10-15` - Race condition on shared map
3. **[NOTE]** `Review Comment` - Overall structure looks clean
```

### 4. Clear session

Click **Clear Session** in the tool window toolbar. This removes all comments and gutter icons.

Comments persist across IDE restarts (stored in `.idea/workspace.xml`).
