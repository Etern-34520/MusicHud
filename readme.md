# Music Hud
![Static Badge](https://img.shields.io/badge/Java-21-red?style=for-the-badge)
![Static Badge](https://img.shields.io/badge/Minecraft-1.21.8-blue?style=for-the-badge)
![Static Badge](https://img.shields.io/badge/Platform-Fabric-green?style=for-the-badge)
![Static Badge](https://img.shields.io/badge/Platform-Neoforge-orange?style=for-the-badge)
![Static Badge](https://img.shields.io/badge/License-LGPLv3-brightgreen?style=for-the-badge)

#### 一个 GUI 化的全服点歌模组

## 相关链接
[ModRinth](https://modrinth.com/mod/music-hud)

[MC 百科](https://www.mcmod.cn/class/23688.html)

![img](https://cdn-alt.modrinth.com/data/7Rnb6oJr/images/96714cbb6621950e3daceee1ab2f7343836e9bbb.png)
![img](https://cdn-alt.modrinth.com/data/7Rnb6oJr/images/49fdedf9f26ec05930035f9f79ba388502f0f62d.png)

## 前置依赖
- ModernUI
- Forge Config API Port
- Architectury API

## 特点
- GUI 化， 提供游戏内操作界面，以及较为美观易配置的 HUD
- 可读取网易云账户歌单，方便点歌
- 服务端不保留用户数据，仅在登录时暂存
- 流式播放，无赘余的客户端缓存

## 功能 
- 搜索音乐
- 通过二维码登录网易云账户，从用户歌单点歌
- 全服同步播放列表
- 配置歌单作为空闲播放源，服务器随机切歌，解放双手（？）
- 在 HUD 和用户界面中展示歌词，点歌玩家（/头像）
- 在用户界面中展示播放列表

## 使用
### 客户端
> 目前不支持在单人游戏中使用

在 mods 文件夹中放入 Architectury API, ModernUI 和 Forge Config API Port (仅Fabric需要) 这几个前置 mod 和 MusicHud 的 jar 文件即可
### 服务端
1. 部署 Netease Cloud Music API Enhanced (https://github.com/neteasecloudmusicapienhanced/api-enhanced)
2. 如果不使用 NCM API Enhanced 的默认端口 ( 3000 ) 或在其他服务器上部署，需要修改配置文件的 serverApiBaseUrl 属性

配置文件位置 `/config/music_hud-server.toml`

配置文件默认内容
```toml
#Server API Base URL configuration
serverApiBaseUrl = "http://localhost:3000"
#Music Pusher's vote additional rate when voting for skip music configuration (0.0 ~ 1.0, total rate larger than or equals to 0.5 means to skip)
# Default: 0.5
# Range: 0.0 ~ 1.0
pusherVoteAdditionalRate = 0.5
#Use random Chinese IP provided by api server
useRandomCnIp = true

```

## 已知问题
- 未登录时，由于使用音源替换，部分音乐可能会出现音频瑕疵以及加载缓慢