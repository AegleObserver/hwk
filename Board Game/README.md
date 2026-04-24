## 项目简介

本项目是一个使用 Java 编写的终端/窗口式棋类游戏程序，采用 Maven 构建，并使用 Lanterna 提供界面显示。程序启动后会进入统一的游戏界面，支持在同一个窗口中切换多个游戏实例。

当前项目包含以下游戏模式：

- Reversi（黑白棋）
- Peace 模式（Reversi 的无翻转练习模式）
- Minesweeper（扫雷）

## 环境要求

- JDK 21
- Maven 3.x
- Windows 环境下可直接运行图形窗口

## 运行方式

### 方式一：使用 Maven 直接运行

在项目根目录下执行：

```bash
mvn clean compile exec:java
```

### 方式二：先打包再运行

先生成可执行 jar：

```bash
mvn clean package
```

打包完成后，在 target 目录下可以找到带依赖的 jar 文件，随后执行：

```bash
java -jar target/reversi-1.0-SNAPSHOT-jar-with-dependencies.jar
```

如果你的实际打包文件名与上面略有差异，以 target 目录中的最终文件名为准。

## 操作说明

程序启动后会弹出游戏界面，输入框和按钮用于控制游戏。

### 通用操作

- 输入 Q：退出程序
- 输入 1、2、3、4、5：切换到对应编号的游戏实例
- 输入 A1、B2 这样的坐标：在当前棋盘上落子或执行操作

### 黑白棋相关

- 默认进入 Reversi 模式
- 输入 U：撤销上一步
- 支持合法落子、自动翻转棋子、计分显示
- 棋盘上会显示可落子位置提示
- 可使用 Reset 重置当前棋盘
- 可使用 New Games 新建一个游戏实例并切换过去

### 扫雷相关

- 可使用 Flip 按钮或输入对应操作进行翻开
- 可使用 Flag 按钮进行标记
- 可输入 H 使用提示功能
- 扫雷模式下支持撤销上一步

## 项目功能

- 支持黑白棋和扫雷两种游戏
- 支持 Peace 模式下的棋盘练习
- 支持多个游戏实例切换
- 在 Reversi 模式下支持撤销操作
- 支持重置当前游戏
- 支持扫雷提示与标记
- 使用GUI界面显示棋盘、提示信息和操作区

## 代码结构说明

- Reversi.java：程序入口，启动界面
- TerminalUI.java：统一的游戏界面与交互逻辑
- GameSession.java：游戏会话接口
- Board.java：黑白棋棋盘与合法落子逻辑
- ReversiGame.java：黑白棋/Peace 模式的游戏规则实现
- MinesweeperGame.java：扫雷逻辑实现
- MinesweeperBoard.java：扫雷棋盘数据与状态管理
- MinesweeperBoardAdapter.java：扫雷棋盘适配到统一显示接口
- BoardView.java：棋盘统一显示接口
- ReversiBoardView.java：黑白棋棋盘显示接口
- MinesweeperBoardView.java：扫雷棋盘显示接口
- ProcessInput.java：输入解析
- TurnResult.java：操作结果枚举
- StringConstructer.java：字符串拼接辅助类

## 说明

- 本项目的入口类配置在 pom.xml 中，默认主类为 Reversi。
- 黑白棋棋盘大小为固定 8x8。
- 如果运行时窗口显示异常，通常与字体或终端环境有关，建议使用系统默认字体或在 Windows 环境下运行。
