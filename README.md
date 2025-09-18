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

Wplace提供了64种颜色，前32种为免费颜色，后32种每个需要`2,000`Droplets解锁。

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

对于颜色代码，请检查[附录](#全部颜色表)

#### 相关接口

- [/me](#get-me)

### 旗帜

> 关键字：`Flag`

Wplace包含251种旗帜，购买旗帜之后可以让你在对应的地区绘制时候节省10%的像素，一个旗帜的价格为`20,000`Droplets。

对于旗帜是否解锁通过一个自定义的BitMap来实现，以下是这个BitMap的JS代码：

```js
class Tt {
    constructor(e) {
        u(this, "bytes");
        this.bytes = e ?? new Uint8Array
    }
    set(e, a) {
        const n = Math.floor(e / 8),
            c = e % 8;
        if (n >= this.bytes.length) {
            const r = new Uint8Array(n + 1),
                i = r.length - this.bytes.length;
            for (let h = 0; h < this.bytes.length; h++) r[h + i] = this.bytes[h];
            this.bytes = r
        }
        const l = this.bytes.length - 1 - n;
        a ? this.bytes[l] = this.bytes[l] | 1 << c : this.bytes[l] = this.bytes[l] & ~(1 << c)
    }
    get(e) {
        const a = Math.floor(e / 8),
            n = e % 8,
            c = this.bytes.length;
        return a > c ? !1 : (this.bytes[c - 1 - a] & 1 << n) !== 0
    }
}
```

BitMap可读的Java代码参见[附录](#bitmap-java实现)

前端通过用户资料接口获得`flagsBitmap`字段之后，通过Base64解码为Bytes然后传入BitMap读取某个旗帜ID是否已解锁。

对于全部旗帜代码，请参考[附录](#全部旗帜)

#### 相关接口

- [/me](#get-me)

### 等级

等级可以根据已绘制的像素计算

```java
double totalPainted = 1; // 已经绘制的像素数量
double base = Math.pow(30, 0.65);
double level = Math.pow(totalPainted, 0.65) / base;
```

每升一级会获得`500`droplets和增加`2`最大像素

### 商店

## 协议

如无特殊说明，URL主机为`backend.wplace.live`

对于常见的API错误，参阅[附录](#通用api错误)

### 认证

认证通过Cookie中的字段`j`实现，在登录之后，后端会将[Json Web Token](https://en.wikipedia.org/wiki/JSON_Web_Token)保存到Cookie中，后续请求`wplace.live`和`backend.wplace.live`都会携带这个Cookie

Token是一段被编码的文本，而不是一个普通的随机字符串，可以通过[jwt.io](https://jwt.io)或任何JWT工具解码得到一些信息。

```json
{
  "userId": 1,
  "sessionId": "",
  "iss": "wplace",
  "exp": 1758373929,
  "iat": 1755781929
}
```

其中`exp`字段为过期时间戳，可以仅通过token得出过期时间。

### Cookie

通常来说请求接口只需要携带`j`一个Cookie即可，但是如果服务器处于高负载，开发者会开启[Under Attack模式](https://developers.cloudflare.com/fundamentals/reference/under-attack-mode/)，如果开启Under Attack模式需要额外携带一个有效的`cf_clearance`Cookie，否则会弹出Cloudflare质询。

需要确保你在让自动程序发起请求时请求头中的大部分字段（如 `User-Agent`、`Accept-Language` 等）和你获得`cf_clearance`的浏览器一致，否则会验证不通过仍然会弹出质询。

### GET `/me`

获得用户信息

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

### POST `/me/update`

更新当前用户的个人信息

#### 请求

* 需要 `j` 完成认证

#### 请求示例

```jsonc
{
    // string：用户昵称
    "name": "cubk",
    // boolean：是否在alliance展示最后一个像素
    "showLastPixel": true,
    // discord用户名
    "discord": "_cubk"
}
```

#### 成功返回

```jsonc
{
    "success": true
}
```

#### 错误返回

```jsonc
{
    "error": "The name has more than 16 characters",
    "status": 400
}
```

> 请求体不合法

### GET `/alliance`

获得Alliance信息

#### 请求

* 需要 `j` 完成认证

#### 成功返回

```jsonc
{
	// string: 工会介绍
	"description": "CCB",
	// object: 总部（Headquarters）
	"hq": {
		"latitude": 22.535013525851937,
		"longitude": 114.01152903098966
	},
	// int: Alliance ID
	"id": 453128,
	// int: 成员数量
	"members": 263,
	// string: 名字
	"name": "Team RealB",
	// string: 已绘制的总数
	"pixelsPainted": 1419281,
	// enum: 你的权限
	// admin/memeber
	"role": "admin"
}
```

#### 错误返回

```jsonc
{
	"error": "Not Found",
	"status": 404
}
```

> 没有加入任何Alliance

### POST `/alliance`

创建一个Alliance

#### 请求

* 需要 `j` 完成认证

#### 请求示例

```jsonc
{
    // string: Alliance名字，不能重名。
	"name": "Team RealB"
}
```

#### 成功返回

```jsonc
{
    // int: 创建完的Alliance ID
	"id": 1
}
```

#### 错误返回

```jsonc
{
	"error": "name_taken",
	"status": 400
}
```

> Alliance名字已经被占用

## 反作弊

## 附录

### 通用API错误

```jsonc
{
  "error": "Unauthorized",
  "status": 401
}
```

> 未附带 `j` token 或者 token 无效


```jsonc
{
  "error": "Internal Server Error. We'll look into it, please try again later.",
  "status": 500
}
```

> Cookie 已过期

### 全部颜色表
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

### BitMap Java实现

```java
public class WplaceBitMap {
    private byte[] bytes;

    public WplaceBitMap() {
        this.bytes = new byte[0];
    }

    public WplaceBitMap(byte[] bytes) {
        this.bytes = bytes != null ? bytes : new byte[0];
    }

    public void set(int index, boolean value) {
        int byteIndex = index / 8;
        int bitIndex = index % 8;

        if (byteIndex >= bytes.length) {
            byte[] newBytes = new byte[byteIndex + 1];
            int offset = newBytes.length - bytes.length;
            System.arraycopy(bytes, 0, newBytes, offset, bytes.length);
            bytes = newBytes;
        }

        int realIndex = bytes.length - 1 - byteIndex;

        if (value) {
            bytes[realIndex] |= (1 << bitIndex);
        } else {
            bytes[realIndex] &= ~(1 << bitIndex);
        }
    }

    public boolean get(int index) {
        int byteIndex = index / 8;
        int bitIndex = index % 8;

        if (byteIndex >= bytes.length) {
            return false;
        }

        int realIndex = bytes.length - 1 - byteIndex;
        return (bytes[realIndex] & (1 << bitIndex)) != 0;
    }

    public String toBase64() {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
```

### 全部旗帜


| 旗帜 | 地区代码 | ID  |
|---|------|-----|
| 🇦🇫 | `AF` | `1` |
| 🇦🇱 | `AL` | `2` |
| 🇩🇿 | `DZ` | `3` |
| 🇦🇸 | `AS` | `4` |
| 🇦🇩 | `AD` | `5` |
| 🇦🇴 | `AO` | `6` |
| 🇦🇮 | `AI` | `7` |
| 🇦🇶 | `AQ` | `8` |
| 🇦🇬 | `AG` | `9` |
| 🇦🇷 | `AR` | `10` |
| 🇦🇲 | `AM` | `11` |
| 🇦🇼 | `AW` | `12` |
| 🇦🇺 | `AU` | `13` |
| 🇦🇹 | `AT` | `14` |
| 🇦🇿 | `AZ` | `15` |
| 🇧🇸 | `BS` | `16` |
| 🇧🇭 | `BH` | `17` |
| 🇧🇩 | `BD` | `18` |
| 🇧🇧 | `BB` | `19` |
| 🇧🇾 | `BY` | `20` |
| 🇧🇪 | `BE` | `21` |
| 🇧🇿 | `BZ` | `22` |
| 🇧🇯 | `BJ` | `23` |
| 🇧🇲 | `BM` | `24` |
| 🇧🇹 | `BT` | `25` |
| 🇧🇴 | `BO` | `26` |
| 🇧🇶 | `BQ` | `27` |
| 🇧🇦 | `BA` | `28` |
| 🇧🇼 | `BW` | `29` |
| 🇧🇻 | `BV` | `30` |
| 🇧🇷 | `BR` | `31` |
| 🇮🇴 | `IO` | `32` |
| 🇧🇳 | `BN` | `33` |
| 🇧🇬 | `BG` | `34` |
| 🇧🇫 | `BF` | `35` |
| 🇧🇮 | `BI` | `36` |
| 🇨🇻 | `CV` | `37` |
| 🇰🇭 | `KH` | `38` |
| 🇨🇲 | `CM` | `39` |
| 🇨🇦 | `CA` | `40` |
| 🇰🇾 | `KY` | `41` |
| 🇨🇫 | `CF` | `42` |
| 🇹🇩 | `TD` | `43` |
| 🇨🇱 | `CL` | `44` |
| 🇨🇳 | `CN` | `45` |
| 🇨🇽 | `CX` | `46` |
| 🇨🇨 | `CC` | `47` |
| 🇨🇴 | `CO` | `48` |
| 🇰🇲 | `KM` | `49` |
| 🇨🇬 | `CG` | `50` |
| 🇨🇰 | `CK` | `51` |
| 🇨🇷 | `CR` | `52` |
| 🇭🇷 | `HR` | `53` |
| 🇨🇺 | `CU` | `54` |
| 🇨🇼 | `CW` | `55` |
| 🇨🇾 | `CY` | `56` |
| 🇨🇿 | `CZ` | `57` |
| 🇨🇮 | `CI` | `58` |
| 🇩🇰 | `DK` | `59` |
| 🇩🇯 | `DJ` | `60` |
| 🇩🇲 | `DM` | `61` |
| 🇩🇴 | `DO` | `62` |
| 🇪🇨 | `EC` | `63` |
| 🇪🇬 | `EG` | `64` |
| 🇸🇻 | `SV` | `65` |
| 🇬🇶 | `GQ` | `66` |
| 🇪🇷 | `ER` | `67` |
| 🇪🇪 | `EE` | `68` |
| 🇸🇿 | `SZ` | `69` |
| 🇪🇹 | `ET` | `70` |
| 🇫🇰 | `FK` | `71` |
| 🇫🇴 | `FO` | `72` |
| 🇫🇯 | `FJ` | `73` |
| 🇫🇮 | `FI` | `74` |
| 🇫🇷 | `FR` | `75` |
| 🇬🇫 | `GF` | `76` |
| 🇵🇫 | `PF` | `77` |
| 🇹🇫 | `TF` | `78` |
| 🇬🇦 | `GA` | `79` |
| 🇬🇲 | `GM` | `80` |
| 🇬🇪 | `GE` | `81` |
| 🇩🇪 | `DE` | `82` |
| 🇬🇭 | `GH` | `83` |
| 🇬🇮 | `GI` | `84` |
| 🇬🇷 | `GR` | `85` |
| 🇬🇱 | `GL` | `86` |
| 🇬🇩 | `GD` | `87` |
| 🇬🇵 | `GP` | `88` |
| 🇬🇺 | `GU` | `89` |
| 🇬🇹 | `GT` | `90` |
| 🇬🇬 | `GG` | `91` |
| 🇬🇳 | `GN` | `92` |
| 🇬🇼 | `GW` | `93` |
| 🇬🇾 | `GY` | `94` |
| 🇭🇹 | `HT` | `95` |
| 🇭🇲 | `HM` | `96` |
| 🇭🇳 | `HN` | `97` |
| 🇭🇰 | `HK` | `98` |
| 🇭🇺 | `HU` | `99` |
| 🇮🇸 | `IS` | `100` |
| 🇮🇳 | `IN` | `101` |
| 🇮🇩 | `ID` | `102` |
| 🇮🇷 | `IR` | `103` |
| 🇮🇶 | `IQ` | `104` |
| 🇮🇪 | `IE` | `105` |
| 🇮🇲 | `IM` | `106` |
| 🇮🇱 | `IL` | `107` |
| 🇮🇹 | `IT` | `108` |
| 🇯🇲 | `JM` | `109` |
| 🇯🇵 | `JP` | `110` |
| 🇯🇪 | `JE` | `111` |
| 🇯🇴 | `JO` | `112` |
| 🇰🇿 | `KZ` | `113` |
| 🇰🇪 | `KE` | `114` |
| 🇰🇮 | `KI` | `115` |
| 🇽🇰 | `XK` | `116` |
| 🇰🇼 | `KW` | `117` |
| 🇰🇬 | `KG` | `118` |
| 🇱🇦 | `LA` | `119` |
| 🇱🇻 | `LV` | `120` |
| 🇱🇧 | `LB` | `121` |
| 🇱🇸 | `LS` | `122` |
| 🇱🇷 | `LR` | `123` |
| 🇱🇾 | `LY` | `124` |
| 🇱🇮 | `LI` | `125` |
| 🇱🇹 | `LT` | `126` |
| 🇱🇺 | `LU` | `127` |
| 🇲🇴 | `MO` | `128` |
| 🇲🇬 | `MG` | `129` |
| 🇲🇼 | `MW` | `130` |
| 🇲🇾 | `MY` | `131` |
| 🇲🇻 | `MV` | `132` |
| 🇲🇱 | `ML` | `133` |
| 🇲🇹 | `MT` | `134` |
| 🇲🇭 | `MH` | `135` |
| 🇲🇶 | `MQ` | `136` |
| 🇲🇷 | `MR` | `137` |
| 🇲🇺 | `MU` | `138` |
| 🇾🇹 | `YT` | `139` |
| 🇲🇽 | `MX` | `140` |
| 🇫🇲 | `FM` | `141` |
| 🇲🇩 | `MD` | `142` |
| 🇲🇨 | `MC` | `143` |
| 🇲🇳 | `MN` | `144` |
| 🇲🇪 | `ME` | `145` |
| 🇲🇸 | `MS` | `146` |
| 🇲🇦 | `MA` | `147` |
| 🇲🇿 | `MZ` | `148` |
| 🇲🇲 | `MM` | `149` |
| 🇳🇦 | `NA` | `150` |
| 🇳🇷 | `NR` | `151` |
| 🇳🇵 | `NP` | `152` |
| 🇳🇱 | `NL` | `153` |
| 🇳🇨 | `NC` | `154` |
| 🇳🇿 | `NZ` | `155` |
| 🇳🇮 | `NI` | `156` |
| 🇳🇪 | `NE` | `157` |
| 🇳🇬 | `NG` | `158` |
| 🇳🇺 | `NU` | `159` |
| 🇳🇫 | `NF` | `160` |
| 🇰🇵 | `KP` | `161` |
| 🇲🇰 | `MK` | `162` |
| 🇲🇵 | `MP` | `163` |
| 🇳🇴 | `NO` | `164` |
| 🇴🇲 | `OM` | `165` |
| 🇵🇰 | `PK` | `166` |
| 🇵🇼 | `PW` | `167` |
| 🇵🇸 | `PS` | `168` |
| 🇵🇦 | `PA` | `169` |
| 🇵🇬 | `PG` | `170` |
| 🇵🇾 | `PY` | `171` |
| 🇵🇪 | `PE` | `172` |
| 🇵🇭 | `PH` | `173` |
| 🇵🇳 | `PN` | `174` |
| 🇵🇱 | `PL` | `175` |
| 🇵🇹 | `PT` | `176` |
| 🇵🇷 | `PR` | `177` |
| 🇶🇦 | `QA` | `178` |
| 🇨🇩 | `CD` | `179` |
| 🇷🇴 | `RO` | `180` |
| 🇷🇺 | `RU` | `181` |
| 🇷🇼 | `RW` | `182` |
| 🇷🇪 | `RE` | `183` |
| 🇧🇱 | `BL` | `184` |
| 🇸🇭 | `SH` | `185` |
| 🇰🇳 | `KN` | `186` |
| 🇱🇨 | `LC` | `187` |
| 🇲🇫 | `MF` | `188` |
| 🇵🇲 | `PM` | `189` |
| 🇻🇨 | `VC` | `190` |
| 🇼🇸 | `WS` | `191` |
| 🇸🇲 | `SM` | `192` |
| 🇸🇹 | `ST` | `193` |
| 🇸🇦 | `SA` | `194` |
| 🇸🇳 | `SN` | `195` |
| 🇷🇸 | `RS` | `196` |
| 🇸🇨 | `SC` | `197` |
| 🇸🇱 | `SL` | `198` |
| 🇸🇬 | `SG` | `199` |
| 🇸🇽 | `SX` | `200` |
| 🇸🇰 | `SK` | `201` |
| 🇸🇮 | `SI` | `202` |
| 🇸🇧 | `SB` | `203` |
| 🇸🇴 | `SO` | `204` |
| 🇿🇦 | `ZA` | `205` |
| 🇬🇸 | `GS` | `206` |
| 🇰🇷 | `KR` | `207` |
| 🇸🇸 | `SS` | `208` |
| 🇪🇸 | `ES` | `209` |
| 🇱🇰 | `LK` | `210` |
| 🇸🇩 | `SD` | `211` |
| 🇸🇷 | `SR` | `212` |
| 🇸🇯 | `SJ` | `213` |
| 🇸🇪 | `SE` | `214` |
| 🇨🇭 | `CH` | `215` |
| 🇸🇾 | `SY` | `216` |
| 🇨🇳 | `TW` | `217` |
| 🇹🇯 | `TJ` | `218` |
| 🇹🇿 | `TZ` | `219` |
| 🇹🇭 | `TH` | `220` |
| 🇹🇱 | `TL` | `221` |
| 🇹🇬 | `TG` | `222` |
| 🇹🇰 | `TK` | `223` |
| 🇹🇴 | `TO` | `224` |
| 🇹🇹 | `TT` | `225` |
| 🇹🇳 | `TN` | `226` |
| 🇹🇲 | `TM` | `227` |
| 🇹🇨 | `TC` | `228` |
| 🇹🇻 | `TV` | `229` |
| 🇹🇷 | `TR` | `230` |
| 🇺🇬 | `UG` | `231` |
| 🇺🇦 | `UA` | `232` |
| 🇦🇪 | `AE` | `233` |
| 🇬🇧 | `GB` | `234` |
| 🇺🇸 | `US` | `235` |
| 🇺🇲 | `UM` | `236` |
| 🇺🇾 | `UY` | `237` |
| 🇺🇿 | `UZ` | `238` |
| 🇻🇺 | `VU` | `239` |
| 🇻🇦 | `VA` | `240` |
| 🇻🇪 | `VE` | `241` |
| 🇻🇳 | `VN` | `242` |
| 🇻🇬 | `VG` | `243` |
| 🇻🇮 | `VI` | `244` |
| 🇼🇫 | `WF` | `245` |
| 🇪🇭 | `EH` | `246` |
| 🇾🇪 | `YE` | `247` |
| 🇿🇲 | `ZM` | `248` |
| 🇿🇼 | `ZW` | `249` |
| 🇦🇽 | `AX` | `250` |
| 🇮🇨 | `IC` | `251` |