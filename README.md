# Wplace Protocol

[Wplace](https://wplace.live)的技术栈、协议及接口的分析。

## 概念

_大多数命名为主观命名，不代表和源码或其他wplace项目中命名一致_

### 地图

> 关键字：`Map / Canvas / World`

地图指Wplace的整体画布。基于[墨卡托投影（Mercator Projection / Web Mercator）](https://en.wikipedia.org/wiki/Mercator_projection)渲染，地图采用[OpenFreeMap](https://openfreemap.org/)的Liberty Style。地图包含`2048x2048`也就是`4,194,304`个[瓦片](#瓦片)，瓦片在前端通过Canvas覆盖在地图之上。

地图的总像素数量为 `4,398,046,511,104`（约 4.4 trillion / 4.4 兆 / 4.4 万亿）。

### 瓦片

> 关键字：`Tile / Chunk`

瓦片是wplace渲染画布的最小单位。每个瓦片在服务端是一张`1024×1024`的PNG图像，包含`1,048,576`个像素。

瓦片对应的数据类型为`Vec2i`，即 `x` 和 `y`。

#### 计算对应经纬度

整个[地图](#地图)在横向与纵向的瓦片数量均为`2048`。通过这个即可计算出`Zoom`值：

```java
int n = 2048; // 瓦片数量
int z = (int) (Math.log(n) / Math.log(2)); // 通过换底公式求出Zoom
```

经过这个公式计算，可以求出zoom**约为**`11`，随后即可使用下列算法计算经纬度：

```java
double n = Math.pow(2.0, 11); // zoom 为 11
double lon = (x + 0.5) / n * 360.0 - 180.0;
double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * (y + 0.5) / n)));
double lat = Math.toDegrees(latRad);
```

其中的`lon`和`lat`即为经纬度的值

> 公式参考自：[Slippy map tilenames](https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames)

### 颜色

> 关键字：`Color / Palette`

Wplace提供了64种颜色，前32种为免费颜色，后32种每个需要`2000`Droplets解锁。

对于颜色是否已经解锁，前端通过位掩码检查 （Bitmask Check）来检查`extraColorsBitmap`，`extraColorsBitmap`为前端获得用户资料接口返回的Json中的一个字段。

其检查逻辑为：

```java
int extraColorsBitmap = 0;
int colorId = 63; // 需要检查的颜色ID
boolean unlocked;

if (colorId < 32) { // 跳过前32因为前32个颜色是免费的
    unlocked = true;
} else {
    int mask = 1 << (colorId - 32);
    unlocked = (extraColorsBitmap & mask) != 0;
}
```

> 免责声明：此代码为笔者根据Wplace中的混淆过的JS代码分析得出的Java代码，而非原始代码。

#### 全部颜色表
| 颜色 | ID | RGB | 是否付费 |
|------|------| ------- | ----- |
| | `0` | 透明 | `true` |
| ![#000000](https://img.shields.io/badge/-%20-000000?style=flat-square) | `1` | `0, 0, 0` | `false` |
| ![#3c3c3c](https://img.shields.io/badge/-%20-3c3c3c?style=flat-square) | `2` | `60, 60, 60` | `false` |
| ![#787878](https://img.shields.io/badge/-%20-787878?style=flat-square) | `3` | `120, 120, 120` | `false` |
| ![#d2d2d2](https://img.shields.io/badge/-%20-d2d2d2?style=flat-square) | `4` | `210, 210, 210` | `false` |
| ![#ffffff](https://img.shields.io/badge/-%20-ffffff?style=flat-square) | `5` | `255, 255, 255` | `false` |
| ![#600018](https://img.shields.io/badge/-%20-600018?style=flat-square) | `6` | `96, 0, 24` | `false` |
| ![#ed1c24](https://img.shields.io/badge/-%20-ed1c24?style=flat-square) | `7` | `237, 28, 36` | `false` |
| ![#ff7f27](https://img.shields.io/badge/-%20-ff7f27?style=flat-square) | `8` | `255, 127, 39` | `false` |
| ![#f6aa09](https://img.shields.io/badge/-%20-f6aa09?style=flat-square) | `9` | `246, 170, 9` | `false` |
| ![#f9dd3b](https://img.shields.io/badge/-%20-f9dd3b?style=flat-square) | `10` | `249, 221, 59` | `false` |
| ![#fffabc](https://img.shields.io/badge/-%20-fffabc?style=flat-square) | `11` | `255, 250, 188` | `false` |
| ![#0eb968](https://img.shields.io/badge/-%20-0eb968?style=flat-square) | `12` | `14, 185, 104` | `false` |
| ![#13e67b](https://img.shields.io/badge/-%20-13e67b?style=flat-square) | `13` | `19, 230, 123` | `false` |
| ![#87ff5e](https://img.shields.io/badge/-%20-87ff5e?style=flat-square) | `14` | `135, 255, 94` | `false` |
| ![#0c816e](https://img.shields.io/badge/-%20-0c816e?style=flat-square) | `15` | `12, 129, 110` | `false` |
| ![#10aea6](https://img.shields.io/badge/-%20-10aea6?style=flat-square) | `16` | `16, 174, 166` | `false` |
| ![#13e1be](https://img.shields.io/badge/-%20-13e1be?style=flat-square) | `17` | `19, 225, 190` | `false` |
| ![#28509e](https://img.shields.io/badge/-%20-28509e?style=flat-square) | `18` | `40, 80, 158` | `false` |
| ![#4093e4](https://img.shields.io/badge/-%20-4093e4?style=flat-square) | `19` | `64, 147, 228` | `false` |
| ![#60f7f2](https://img.shields.io/badge/-%20-60f7f2?style=flat-square) | `20` | `96, 247, 242` | `false` |
| ![#6b50f6](https://img.shields.io/badge/-%20-6b50f6?style=flat-square) | `21` | `107, 80, 246` | `false` |
| ![#99b1fb](https://img.shields.io/badge/-%20-99b1fb?style=flat-square) | `22` | `153, 177, 251` | `false` |
| ![#780c99](https://img.shields.io/badge/-%20-780c99?style=flat-square) | `23` | `120, 12, 153` | `false` |
| ![#aa38b9](https://img.shields.io/badge/-%20-aa38b9?style=flat-square) | `24` | `170, 56, 185` | `false` |
| ![#e09ff9](https://img.shields.io/badge/-%20-e09ff9?style=flat-square) | `25` | `224, 159, 249` | `false` |
| ![#cb007a](https://img.shields.io/badge/-%20-cb007a?style=flat-square) | `26` | `203, 0, 122` | `false` |
| ![#ec1f80](https://img.shields.io/badge/-%20-ec1f80?style=flat-square) | `27` | `236, 31, 128` | `false` |
| ![#f38da9](https://img.shields.io/badge/-%20-f38da9?style=flat-square) | `28` | `243, 141, 169` | `false` |
| ![#684634](https://img.shields.io/badge/-%20-684634?style=flat-square) | `29` | `104, 70, 52` | `false` |
| ![#95682a](https://img.shields.io/badge/-%20-95682a?style=flat-square) | `30` | `149, 104, 42` | `false` |
| ![#f8b277](https://img.shields.io/badge/-%20-f8b277?style=flat-square) | `31` | `248, 178, 119` | `false` |
| ![#aaaaaa](https://img.shields.io/badge/-%20-aaaaaa?style=flat-square) | `32` | `170, 170, 170` | `true` |
| ![#a50e1e](https://img.shields.io/badge/-%20-a50e1e?style=flat-square) | `33` | `165, 14, 30` | `true` |
| ![#fa8072](https://img.shields.io/badge/-%20-fa8072?style=flat-square) | `34` | `250, 128, 114` | `true` |
| ![#e45c1a](https://img.shields.io/badge/-%20-e45c1a?style=flat-square) | `35` | `228, 92, 26` | `true` |
| ![#d6b594](https://img.shields.io/badge/-%20-d6b594?style=flat-square) | `36` | `214, 181, 148` | `true` |
| ![#9c8431](https://img.shields.io/badge/-%20-9c8431?style=flat-square) | `37` | `156, 132, 49` | `true` |
| ![#c5ad31](https://img.shields.io/badge/-%20-c5ad31?style=flat-square) | `38` | `197, 173, 49` | `true` |
| ![#e8d45f](https://img.shields.io/badge/-%20-e8d45f?style=flat-square) | `39` | `232, 212, 95` | `true` |
| ![#4a6b3a](https://img.shields.io/badge/-%20-4a6b3a?style=flat-square) | `40` | `74, 107, 58` | `true` |
| ![#5a944a](https://img.shields.io/badge/-%20-5a944a?style=flat-square) | `41` | `90, 148, 74` | `true` |
| ![#84c573](https://img.shields.io/badge/-%20-84c573?style=flat-square) | `42` | `132, 197, 115` | `true` |
| ![#0f799f](https://img.shields.io/badge/-%20-0f799f?style=flat-square) | `43` | `15, 121, 159` | `true` |
| ![#bbfaf2](https://img.shields.io/badge/-%20-bbfaf2?style=flat-square) | `44` | `187, 250, 242` | `true` |
| ![#7dc7ff](https://img.shields.io/badge/-%20-7dc7ff?style=flat-square) | `45` | `125, 199, 255` | `true` |
| ![#4d31b8](https://img.shields.io/badge/-%20-4d31b8?style=flat-square) | `46` | `77, 49, 184` | `true` |
| ![#4a4284](https://img.shields.io/badge/-%20-4a4284?style=flat-square) | `47` | `74, 66, 132` | `true` |
| ![#7a71c4](https://img.shields.io/badge/-%20-7a71c4?style=flat-square) | `48` | `122, 113, 196` | `true` |
| ![#b5aef1](https://img.shields.io/badge/-%20-b5aef1?style=flat-square) | `49` | `181, 174, 241` | `true` |
| ![#dba463](https://img.shields.io/badge/-%20-dba463?style=flat-square) | `50` | `219, 164, 99` | `true` |
| ![#d18051](https://img.shields.io/badge/-%20-d18051?style=flat-square) | `51` | `209, 128, 81` | `true` |
| ![#ffc5a5](https://img.shields.io/badge/-%20-ffc5a5?style=flat-square) | `52` | `255, 197, 165` | `true` |
| ![#9b5249](https://img.shields.io/badge/-%20-9b5249?style=flat-square) | `53` | `155, 82, 73` | `true` |
| ![#d18078](https://img.shields.io/badge/-%20-d18078?style=flat-square) | `54` | `209, 128, 120` | `true` |
| ![#fab6a4](https://img.shields.io/badge/-%20-fab6a4?style=flat-square) | `55` | `250, 182, 164` | `true` |
| ![#7b6352](https://img.shields.io/badge/-%20-7b6352?style=flat-square) | `56` | `123, 99, 82` | `true` |
| ![#9c846b](https://img.shields.io/badge/-%20-9c846b?style=flat-square) | `57` | `156, 132, 107` | `true` |
| ![#333941](https://img.shields.io/badge/-%20-333941?style=flat-square) | `58` | `51, 57, 65` | `true` |
| ![#6d758d](https://img.shields.io/badge/-%20-6d758d?style=flat-square) | `59` | `109, 117, 141` | `true` |
| ![#b3b9d1](https://img.shields.io/badge/-%20-b3b9d1?style=flat-square) | `60` | `179, 185, 209` | `true` |
| ![#6d643f](https://img.shields.io/badge/-%20-6d643f?style=flat-square) | `61` | `109, 100, 63` | `true` |
| ![#948c6b](https://img.shields.io/badge/-%20-948c6b?style=flat-square) | `62` | `148, 140, 107` | `true` |
| ![#cdc59e](https://img.shields.io/badge/-%20-cdc59e?style=flat-square) | `63` | `205, 197, 158` | `true` |


### 旗帜

### 等级

### 商品

## 协议

如无特殊说明，URL主机为`backend.wplace.live`

### 认证

认证通过Cookie中的字段`j`实现，在登录之后，后端会将[Json Web Token](https://en.wikipedia.org/wiki/JSON_Web_Token)保存到Cookie中，后续请求`wplace.live`和`backend.wplace.live`都会携带这个Cookie

### Pawtect

### Cookie

### <span style="background-color: #008a29ff">GET</span> /me

#### 请求

- 需要`j`完成认证

#### 成功返回

```jsonc
{
    // int: 工会ID
    "allianceId": 1, 
    // enum: 工会权限
    // admin/member
    "allianceRole": "admin",
    // boolean: 是否被封禁
    "banned": false,
    // object: 像素信息
    "charges": {
        // int: 恢复像素的间隔，单位为毫秒，30000毫秒也就是30秒
        "cooldownMs": 30000,
        // float: 剩余的像素
        "count": 35.821833333333586,
        // float: 最高像素数量
        "max": 500
    },
    // string: ISO-3166-1 alpha-2地区代码
    // 参考：https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
    "country": "JP",
    // string: discord用户名
    "discord": "",
    // int: 剩余droplets
    "droplets": 75,
    // int: 装备的旗帜
    "equippedFlag": 0,
    // object: 灰度测试标记，其内部的意义不明确
    // 例如其中的variant值为koala（考拉），不明确其内部意义，仅为一个代号。
    // 但是会跟着请求头传出去，如果2025-09_pawtect的variant是disabled则不会发送pawtect-token
    // 说明部分用户没有被启用新的安全机制
    "experiments": {
        "2025-09_pawtect": {
            "variant": "koala"
        }
    },
    // int: extraColorsBitmap，参阅颜色小节了解其作用。
    "extraColorsBitmap": 0,
    // array: 收藏的位置
    "favoriteLocations": [
        {
            "id": 1,
            "name": "",
            "latitude": 46.797833514893085,
            "longitude": 0.9266305280273432
        }
    ],
    // string: 已解锁的旗帜列表，参阅旗帜小节了解其作用。
    "flagsBitmap": "AA==",
    // enum: 一般不会出现，如果你有权限才会额外显示
    // moderator/global_moderator/admin
    "role": "",
    // int: 用户ID
    "id": 1,
    // boolean: 是否有购买，如果有则会在菜单显示订单列表
    "isCustomer": false,
    // float: 等级
    "level": 94.08496005353335,
    // int: 最大的收藏数量，默认为15，暂时没有发现如何提升
    "maxFavoriteLocations": 15,
    // string: 用户名
    "name": "username",
    // boolean: 是否需要手机号验证，如果是则会在访问时弹出手机号验证窗口
    "needsPhoneVerification": false,
    // string: 头像URL或base64，需要根据前缀判断（例如https://)
    "picture": "",
    // int: 已经绘制的像素数量
    "pixelsPainted": 114514,
    // boolean: 是否在alliance页面展示你最后一次绘制的位置
    "showLastPixel": true,
    // string: 你的解除封禁时间戳，如果是1970年则意味着没有被封禁或者已经被永久封禁。
    "timeoutUntil": "1970-01-01T00:00:00Z"
}
```

#### 错误返回：

```json
{
  "error": "Unauthorized",
  "status": 401
}
```

> 未附带`j`token或者token无效

## 反作弊
