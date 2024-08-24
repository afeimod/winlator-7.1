此仓库为fork，用于尝试添加一些额外功能。
原仓库：https://github.com/brunodev85/winlator

- MT的文件提供器
  - 自带文件提供器，无需再次MT注入。可使用SAF访问rootfs。
- 全局异常捕获
  - 当winlator app闪退时，java端的异常会被记录在 外部存储/Download/Winlator/crash 文件夹中。
- 安卓快捷方式
  - 主界面 - 快捷方式页面，可对某个快捷方式添加到安卓桌面。长按app图标选中并启动。
- 安卓输入法输入
  - 支持完整Unicode（中文）输入（会多输入一个代替keycode原本的keysym以及backspace。）
- logcat日志
  - 主界面 - 设置页面开启。开启后，将logcat输出保存到 外部存储/Download/Winlator/logcat 文件夹中。
- PRoot简易终端
  - 主界面 - 设置页面开启。开启后，启动容器，返回键显示左侧菜单中进入。可查看PRoot进程内的输出或向其输入命令。
  - ~~目前（6.1）WINEDEBUG会被默认设置成-all，想查看wine输出需要容器设置中手动覆盖，如WINEDEBUG=fixme+all,err+all~~ 7.0开始加入了log系统，可以在设置中调整了
  - 不支持快捷键指令
- 屏幕方向旋转
  - 启动容器，返回键显示左侧菜单中点击，可旋转屏幕方向。
- 绝对位置点击
  - 启动容器，返回键显示左侧菜单 - 输入控制 中开启。第一根手指在某一个位置点击后，鼠标会移动到手指位置。
- 画中画模式
  - 启动容器，返回键显示左侧菜单中可点击。可进入画中画模式。（无法发送输入事件，只能看着，即挂在前台。）
- 读取u盘/移动硬盘
  - 主界面 - 设置页面，点击按钮获取管理全部文件权限。思路来自：[coffincolors](https://discord.com/channels/829747132562800700/1134958326908731482/1261854866507042876), [agnostic-apollo](https://github.com/termux/termux-app/commit/9eeb2babd7638f8b2967ebd93020e7e37d19cc2b)
- MIDI支持
  - 主界面 - 设置界面开启。开启后，启动容器时，会将特殊的 midimap.dll 从 apk/assets 解压到 c:\windows\sysywow64 ，并添加环境变量 WINEDLLOVERRIDES=midimap=n 指定wine使用c盘的dll。未开启选项时环境变量为 midimap=b。使用第三方库 [fluidsynth 2.3.6](https://github.com/FluidSynth/fluidsynth)


