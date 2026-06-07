## 项目简介

本项目是一个使用 Java 编写的图形界面棋类游戏程序，采用 Maven 构建，支持 JavaFX 和终端两种 UI 模式。默认使用 JavaFX 图形界面，支持鼠标操作；同时保留终端模式以兼容无图形界面的环境。

当前项目包含以下游戏模式（启动后按顺序加载）：

- Peace 模式（和平模式）
- Reversi（黑白棋）
- Minesweeper（扫雷）
- Chess（国际象棋）

## 环境要求

- JDK 21
- Maven 3.x
- JavaFX（使用默认 JavaFX UI 时需要）
- Windows 环境下可直接运行图形窗口

### 在终端中检查环境

在 Windows 中打开 `cmd`，执行以下命令检查 Java 和 Maven 是否已经安装：

```cmd
java -version
mvn -version
```

如果 `java -version` 输出的版本不是 21，或提示 `java` 不是内部或外部命令，请安装 JDK 21：

```cmd
winget install EclipseAdoptium.Temurin.21.JDK
```

如果 `mvn -version` 没有输出 Maven 版本，或提示 `mvn` 不是内部或外部命令，请安装 Maven：

```cmd
winget install Apache.Maven
```

安装完成后，重新打开 `cmd`，再次执行 `java -version` 和 `mvn -version`。确认 Java 版本为 21，Maven 版本为 3.x 后，再运行本项目。

如果电脑没有 `winget`，可以手动下载安装：

- JDK 21：https://adoptium.net/temurin/releases/?version=21
- Maven：https://maven.apache.org/download.cgi

### 检查 JavaFX

默认使用 JavaFX UI，需要 JDK 包含 JavaFX 模块。可通过以下命令检查：

```cmd
jlink --list-plugins 2>nul || java --list-modules 2>nul | findstr javafx
```

如果输出中不包含 `javafx`，说明当前 JDK 没有 JavaFX。建议安装包含 JavaFX 的 JDK（如 Liberica FX 或 Oracle JDK），或在无图形界面的环境中使用 `--ui=terminal` 启动。

## 运行方式

程序默认以 JavaFX UI 启动（需要图形界面环境），可通过 `--ui` 参数切换 UI 模式。

可用的 UI 模式：

| 参数 | 说明 | 操作方式 |
|------|------|----------|
| `--ui=javafx`（默认） | JavaFX 图形界面 | 鼠标点击 |
| `--ui=terminal` | 终端界面（Swing 窗口） | 键盘输入 |
| `--ui=swing` | Swing 模式（stub） | 弹窗提示 |

### 方式一：使用 Maven 直接运行

在项目根目录下执行：

```bash
# 默认 JavaFX UI（需图形界面环境）


# 终端 UI（Swing 窗口，适用于无 JavaFX 环境）
# CMD:
mvn clean compile exec:java -Dexec.args="--ui=terminal"
# PowerShell:
mvn clean compile exec:java "-Dexec.args=--ui=terminal"

# Swing 模式
# CMD:
mvn clean compile exec:java -Dexec.args="--ui=swing"
# PowerShell:
mvn clean compile exec:java "-Dexec.args=--ui=swing"
```

### 方式二：先打包再运行

先生成可执行 jar：

```bash
mvn clean package
```

打包完成后，在 target 目录下可以找到带依赖的 jar 文件，随后执行：

```bash
# 默认 JavaFX UI
java -jar target/reversi-1.0-SNAPSHOT-jar-with-dependencies.jar

# 终端 UI
java -jar target/reversi-1.0-SNAPSHOT-jar-with-dependencies.jar --ui=terminal

# Swing 模式
java -jar target/reversi-1.0-SNAPSHOT-jar-with-dependencies.jar --ui=swing

# 查看可用的 UI 插件
java -jar target/reversi-1.0-SNAPSHOT-jar-with-dependencies.jar --list-ui
```

如果你的实际打包文件名与上面略有差异，以 target 目录中的最终文件名为准。

## 操作说明

程序启动后弹出游戏窗口（JavaFX 模式）或终端界面（Terminal 模式）。

### JavaFX UI（默认模式，鼠标操作）

- **落子/翻开**：左键点击棋盘格子
- **扫雷标记**：右键点击格子插旗
- **国际象棋**：左键先后点击棋子与目标格完成行棋
- **输入框**：也可在底部输入框输入命令后点击 Send
- **按钮**：Hint（扫雷提示）、Undo（悔棋）、New Games（新建游戏）
- **切换游戏**：底部输入框输入 1-5 或点击右侧面板的游戏编号

### Terminal UI（键盘操作）

- 输入 Q：退出程序
- 输入 1-5：切换到对应编号的游戏实例
- 输入 U：悔棋
- 输入 H：扫雷提示
- 输入 A1-H8：落子/操作（非国际象棋）
- 输入 m A1 A2：国际象棋行棋

### 和平模式

- 无特殊功能，当棋盘全部被棋子占据时自动结束
- 点击（或输入）A1-H8 落子

### 黑白棋

- 支持合法落子、自动翻转棋子、计分显示
- 棋盘上会显示可落子位置提示（`+`）
- 点击（或输入）A1-H8 落子

### 扫雷

- 左键点击翻开格子，右键点击插旗标记
- 点击 Hint 按钮（或输入 H）使用提示
- 可通过输入框输入 FLAG A1 进行标记

### 国际象棋

- 先点击选中棋子，再点击目标格完成移动
- 也可在输入框输入 `m A1 A2` 行棋
- 通过吃掉对方王赢得游戏

## 项目功能

- 支持 Peace、Reversi、Minesweeper、Chess 四种游戏
- 支持多个游戏实例切换与新建
- 支持悔棋（Undo）操作
- 支持重置当前游戏
- 支持扫雷提示与标记
- 支持 JavaFX 图形界面（鼠标操作）和 Terminal 界面（键盘操作）
- 支持游戏 Demo 演示与 Debugger 记录

## 代码结构说明

### 入口与主控
- `Reversi.java`：程序入口，委托 GameLauncher 启动
- `GameLauncher.java`：UI 插件加载与启动管理器，支持 `--ui` 参数
- `TerminalUI.java`：游戏会话与状态管理中枢

### UI 层（common.ui）
- `GameUiPlugin.java`：UI 插件接口
- `JavaFxUiPlugin.java` / `JavaFxApp.java` / `JavaFxRenderer.java`：JavaFX 图形界面
- `TerminalUiPlugin.java` / `TerminalUiRenderer.java`：终端界面
- `GameUiBridge.java` / `GameUiState.java`：UI 与逻辑之间的桥梁与状态快照

### 游戏实现
- `GameSession.java`：游戏会话接口
- `GameDefinition.java` / `GameRegistry.java`：游戏类型注册
- `reversi/`：黑白棋（Board.java, ReversiGame.java, ReversiBoardView.java）
- `peace/`：和平模式（PeaceGame.java）
- `minesweeper/`：扫雷（MinesweeperGame.java, MinesweeperBoard.java, MinesweeperBoardAdapter.java, MinesweeperBoardView.java）
- `chess/`：国际象棋（ChessGame.java, ChessBoard.java, ChessMoveRules.java 及各棋子规则）

### 工具与辅助
- `ProcessInput.java`：输入解析
- `TurnResult.java`：操作结果枚举
- `GameAction.java`：动作按钮定义
- `StringConstructor.java`：字符串拼接辅助类
- `BoardView.java`：棋盘统一显示接口

## 说明

- 本项目的入口类配置在 pom.xml 中，默认主类为 `common.Reversi`。
- 默认启动 JavaFX UI，Swing/Terminal 模式均需要图形界面环境。
- 所有棋盘大小均为固定 8x8。
- 如果 JavaFX 窗口显示异常，请确保 JDK 包含 JavaFX 模块（推荐使用包含 JavaFX 的 JDK 发行版如 Liberica FX），或在 Windows 环境下运行。
