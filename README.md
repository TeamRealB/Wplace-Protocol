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
| ______ | `0` | 透明 | `true` |
| <span style="background-color: #000000">______</span> | `1` | `0, 0, 0` | `false` |
| <span style="background-color: #3c3c3c">______</span> | `2` | `60, 60, 60` | `false` |
| <span style="background-color: #787878">______</span> | `3` | `120, 120, 120` | `false` |
| <span style="background-color: #d2d2d2">______</span> | `4` | `210, 210, 210` | `false` |
| <span style="background-color: #ffffff">______</span> | `5` | `255, 255, 255` | `false` |
| <span style="background-color: #600018">______</span> | `6` | `96, 0, 24` | `false` |
| <span style="background-color: #ed1c24">______</span> | `7` | `237, 28, 36` | `false` |
| <span style="background-color: #ff7f27">______</span> | `8` | `255, 127, 39` | `false` |
| <span style="background-color: #f6aa09">______</span> | `9` | `246, 170, 9` | `false` |
| <span style="background-color: #f9dd3b">______</span> | `10` | `249, 221, 59` | `false` |
| <span style="background-color: #fffabc">______</span> | `11` | `255, 250, 188` | `false` |
| <span style="background-color: #0eb968">______</span> | `12` | `14, 185, 104` | `false` |
| <span style="background-color: #13e67b">______</span> | `13` | `19, 230, 123` | `false` |
| <span style="background-color: #87ff5e">______</span> | `14` | `135, 255, 94` | `false` |
| <span style="background-color: #0c816e">______</span> | `15` | `12, 129, 110` | `false` |
| <span style="background-color: #10aea6">______</span> | `16` | `16, 174, 166` | `false` |
| <span style="background-color: #13e1be">______</span> | `17` | `19, 225, 190` | `false` |
| <span style="background-color: #28509e">______</span> | `18` | `40, 80, 158` | `false` |
| <span style="background-color: #4093e4">______</span> | `19` | `64, 147, 228` | `false` |
| <span style="background-color: #60f7f2">______</span> | `20` | `96, 247, 242` | `false` |
| <span style="background-color: #6b50f6">______</span> | `21` | `107, 80, 246` | `false` |
| <span style="background-color: #99b1fb">______</span> | `22` | `153, 177, 251` | `false` |
| <span style="background-color: #780c99">______</span> | `23` | `120, 12, 153` | `false` |
| <span style="background-color: #aa38b9">______</span> | `24` | `170, 56, 185` | `false` |
| <span style="background-color: #e09ff9">______</span> | `25` | `224, 159, 249` | `false` |
| <span style="background-color: #cb007a">______</span> | `26` | `203, 0, 122` | `false` |
| <span style="background-color: #ec1f80">______</span> | `27` | `236, 31, 128` | `false` |
| <span style="background-color: #f38da9">______</span> | `28` | `243, 141, 169` | `false` |
| <span style="background-color: #684634">______</span> | `29` | `104, 70, 52` | `false` |
| <span style="background-color: #95682a">______</span> | `30` | `149, 104, 42` | `false` |
| <span style="background-color: #f8b277">______</span> | `31` | `248, 178, 119` | `false` |
| <span style="background-color: #aaaaaa">______</span> | `32` | `170, 170, 170` | `true` |
| <span style="background-color: #a50e1e">______</span> | `33` | `165, 14, 30` | `true` |
| <span style="background-color: #fa8072">______</span> | `34` | `250, 128, 114` | `true` |
| <span style="background-color: #e45c1a">______</span> | `35` | `228, 92, 26` | `true` |
| <span style="background-color: #d6b594">______</span> | `36` | `214, 181, 148` | `true` |
| <span style="background-color: #9c8431">______</span> | `37` | `156, 132, 49` | `true` |
| <span style="background-color: #c5ad31">______</span> | `38` | `197, 173, 49` | `true` |
| <span style="background-color: #e8d45f">______</span> | `39` | `232, 212, 95` | `true` |
| <span style="background-color: #4a6b3a">______</span> | `40` | `74, 107, 58` | `true` |
| <span style="background-color: #5a944a">______</span> | `41` | `90, 148, 74` | `true` |
| <span style="background-color: #84c573">______</span> | `42` | `132, 197, 115` | `true` |
| <span style="background-color: #0f799f">______</span> | `43` | `15, 121, 159` | `true` |
| <span style="background-color: #bbfaf2">______</span> | `44` | `187, 250, 242` | `true` |
| <span style="background-color: #7dc7ff">______</span> | `45` | `125, 199, 255` | `true` |
| <span style="background-color: #4d31b8">______</span> | `46` | `77, 49, 184` | `true` |
| <span style="background-color: #4a4284">______</span> | `47` | `74, 66, 132` | `true` |
| <span style="background-color: #7a71c4">______</span> | `48` | `122, 113, 196` | `true` |
| <span style="background-color: #b5aef1">______</span> | `49` | `181, 174, 241` | `true` |
| <span style="background-color: #dba463">______</span> | `50` | `219, 164, 99` | `true` |
| <span style="background-color: #d18051">______</span> | `51` | `209, 128, 81` | `true` |
| <span style="background-color: #ffc5a5">______</span> | `52` | `255, 197, 165` | `true` |
| <span style="background-color: #9b5249">______</span> | `53` | `155, 82, 73` | `true` |
| <span style="background-color: #d18078">______</span> | `54` | `209, 128, 120` | `true` |
| <span style="background-color: #fab6a4">______</span> | `55` | `250, 182, 164` | `true` |
| <span style="background-color: #7b6352">______</span> | `56` | `123, 99, 82` | `true` |
| <span style="background-color: #9c846b">______</span> | `57` | `156, 132, 107` | `true` |
| <span style="background-color: #333941">______</span> | `58` | `51, 57, 65` | `true` |
| <span style="background-color: #6d758d">______</span> | `59` | `109, 117, 141` | `true` |
| <span style="background-color: #b3b9d1">______</span> | `60` | `179, 185, 209` | `true` |
| <span style="background-color: #6d643f">______</span> | `61` | `109, 100, 63` | `true` |
| <span style="background-color: #948c6b">______</span> | `62` | `148, 140, 107` | `true` |
| <span style="background-color: #cdc59e">______</span> | `63` | `205, 197, 158` | `true` |

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

```json
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
