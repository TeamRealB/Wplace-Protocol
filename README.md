# Wplace Protocol

[Wplace](https://wplace.live)的技术栈、协议及接口的分析。

## 概念

_大多数命名为主观命名，不代表和源码或其他wplace项目中命名一致_

### 地图

<img src="/images/projection.JPG" align="right" width="200">

> 关键字：`Map / Canvas / World`

地图指Wplace的整体画布。基于[墨卡托投影（Mercator Projection / Web Mercator）](https://en.wikipedia.org/wiki/Mercator_projection)渲染，地图采用[OpenFreeMap](https://openfreemap.org/)的Liberty Style。地图包含`2048x2048`也就是`4,194,304`个[瓦片](#瓦片)，瓦片在前端通过Canvas覆盖在地图之上。

地图中大部分现实中没有属地/有争议的位置，例如太平洋被划分为了最近的陆地所属国家或地区。

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
    // string: 头像URL或base64，需要根据前缀判断（例如data:image/png;base64,)
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

### GET `/me/profile-pictures`

获得头像列表

一个人可以有多个头像（添加一个`20,000`Droplets），然后可以随时换头像列表中的任何一个头像

#### 请求

* 需要 `j` 完成认证

#### 成功返回

```jsonc
// array: 所有头像
[
    {
        // int: 头像ID
        "id": 0,
        // string: 头像URL或者Base64，可以通过是否以data:image/png;base64,开头判断
        "url": ""
    }
]
```

> 如果你没有任何头像则会返回空的

### POST `/me/profile-picture/change`

更换头像

#### 请求

* 需要 `j` 完成认证

#### 示例请求

更换已有自定义头像

```jsonc
{
    // int: 头像ID，需要确保你添加了这个头像
	"pictureId": 1
}
```

重置头像

```jsonc
{}
```

> 请求空的JsonObject可以重置头像

#### 成功返回

```jsonc
{
	"success": true
}
```

### POST `/me/profile-picture`

上传头像

#### 请求

* 需要 `j` 完成认证
* 请求体为Multipart File：`image`

#### 成功返回

```jsonc
{
	"success": true
}
```

#### 错误返回

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

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

```jsonc
{
    "error": "Forbidden",
    "status": 403
}
```

> 已有一个Alliance但是仍然尝试创建，正常情况下不会触发。

### POST `/alliance/update-description`

更新Alliance简介

#### 请求

* 需要 `j` 完成认证

#### 成功返回

```jsonc
{
	"success": true
}
```

#### 错误返回

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> 没有Alliance或权限不是admin

### GET `/alliance/invites`

获得Alliance的邀请链接

#### 请求

* 需要 `j` 完成认证

#### 成功返回

```jsonc
// array: Alliance邀请链接，通常只有一个且格式为UUID
[
    "fe7c9c32-e95a-4f5f-a866-554cde2149c3"
]
```

#### 错误返回

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> 没有Alliance或权限不是admin

### GET `/alliance/join/{invite}`

通过Invite UUID加入Alliance，获得Invite UUID参阅[/alliance/invites](#get-allianceinvites)

#### 请求

* 需要 `j` 完成认证
* URL中的{invite}参数为邀请UUID
    - 示例URL（设置为中国国旗）：`/alliance/join/fe7c9c32-e95a-4f5f-a866-554cde2149c3`

#### 成功返回

```jsonc
{
    "success": "true"
}
```

> 如果加入的目标和你已有的Alliance一致，也会返回成功

#### 错误返回

```jsonc
{
    "error": "Not Found",
    "status": 404
}
```

> 没有找到目标Alliance

```jsonc
{
  "error": "Already Reported",
  "status": 208
}
```

> 已经加入了一个Alliance

### POST `/alliance/update-headquarters`

更新Alliance的总部（Headquarters）

#### 请求

* 需要 `j` 完成认证

#### 示例请求

```jsonc
{
	"latitude": 22.537655528880563,
	"longitude": 114.0274942853182
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
	"error": "Forbidden",
	"status": 403
}
```

> 没有Alliance或权限不是admin

### GET `/alliance/members/{page}`

获得Alliance成员列表，有分页系统，有可能需要分多页获取如果成员超过50个

#### 请求

* 需要 `j` 完成认证
* URL中的{page}参数为页码，从0开始
    - 示例URL（获得第一页）：`/alliance/members/0`

#### 成功返回

```jsonc
{
    // array: 一页最多50个
	"data": [{
	    // int: 用户ID
		"id": 1,
		// string: 用户名
		"name": "cubk'",
		// enum: 权限
		// admin/memeber
		"role": "admin"
	}, {
		"id": 1,
		"name": "SillyBitch",
		"role": "admin"
	}, {
		"id": 1,
		"name": "cubk",
		"role": "member"
	}],
	// boolean: 是否还有下一页
	"hasNext": true
}
```

#### 错误返回

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> 没有Alliance或权限不是admin


### GET `/alliance/members/banned/{page}`

获得Alliance已经拉黑的成员列表，有分页系统，有可能需要分多页获取如果成员超过50个

已经拉黑的成员无法再加入Alliance

#### 请求

* 需要 `j` 完成认证
* URL中的{page}参数为页码，从0开始
    - 示例URL（获得第一页）：`/alliance/members/banned/0`

#### 成功返回

```jsonc
{
	"data": [{
		"id": 1,
		"name": "SuckMyDick"
	}],
	"hasNext": false
}
```

> 和普通成员接口大致一致，但是没有`role`，因为已经拉黑就不在alliance里了

#### 错误返回

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> 没有Alliance或权限不是admin

### POST `/alliance/give-admin`

将一个成员提升为Admin，无法降级

#### 请求

* 需要 `j` 完成认证

#### 示例请求

```jsonc
{
    // int: 需要提升的用户ID
	"promotedUserId": 1
}
```

#### 成功返回

本接口没有返回，响应码是`200`即成功

#### 错误返回

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> 没有Alliance或权限不是admin

### POST `/alliance/ban`

踢出并拉黑一个成员

拉黑之后如果不解除拉黑成员无法重新加入

#### 请求

* 需要 `j` 完成认证

#### 示例请求
```jsonc
{
    // int: 需要踢出或拉黑的用户ID
	"bannedUserId": 1
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
	"error": "Forbidden",
	"status": 403
}
```

> 没有Alliance或权限不是admin

### POST `/alliance/unban`

解除拉黑一个成员，解除之后他不会自动回到Alliance，只是可以重新加入了而已。

#### 请求

* 需要 `j` 完成认证

#### 示例请求

```jsonc
{
    // int: 需要解除拉黑的用户ID
	"unbannedUserId": 1
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
	"error": "Forbidden",
	"status": 403
}
```

> 没有Alliance或权限不是admin

### GET `/alliance/leaderboard/{mode}`

获得Alliance内玩家排行榜，仅限前50个。

#### 请求

* 需要 `j` 完成认证
* URL中的`mode`代表时间范围，是一个枚举，可以是以下任何一个值：
    - `today`
    - `week`
    - `month`
    - `all-time`
* 示例URL（今日排行榜）：`/alliance/leaderboard/today`

#### 成功返回

```jsonc
[
  {
    // int: 用户ID
    "userId": 10815100,
    // string: 用户名
    "name": "做爱",
    // int: 旗帜ID，旗帜列表参阅附录
    "equippedFlag": 0,
    // int: 已绘制像素数量
    "pixelsPainted": 32901,
    // 最后一次绘制的经纬度，如果用户关闭了showLastPixel则不会有这两个字段
    "lastLatitude": 22.527739206672393,
    "lastLongitude": 114.02762695312497
  },
  {
    "userId": 10850297,
    "name": "尹永铉",
    "equippedFlag": 0,
    "pixelsPainted": 31631
  }
]
```

### POST `/favorite-location`

收藏一个位置

#### 请求

* 需要 `j` 完成认证

#### 示例请求

```jsonc
{
	"latitude": 22.5199456234827,
	"longitude": 114.02428677802732
}
```

#### 成功返回

```jsonc
{
    // int: 收藏ID
	"id": 1,
	"success": true
}
```

#### 错误返回

```jsonc
{
  "error": "Forbidden",
  "status": 403
}
```

> 收藏数量超过maxFavoriteLocations


### POST `/favorite-location/delete`

取消收藏位置

#### 请求

* 需要 `j` 完成认证

#### 示例请求

```jsonc
{
    // int: 收藏ID
	"id": 1
}
```

#### 成功返回

```jsonc
{
    "success": true
}
```

> 传入任何ID即使是没有收藏的或者不存在的也会返回成功

### POST `/purchase`

购买物品，相关定义请阅读[商店](#商店)小节

#### 请求

* 需要 `j` 完成认证

#### 示例请求

```jsonc
{
    // object: 固定字段product
	"product": {
	    // int: 物品id
		"id": 100,
		// int: 购买数量，对于Paint Charges/Max Charge可以购买多个
		"amount": 1,
		// int: 变体值，部分物品存在变体，如果没有变体不需要这个值
		"variant": 49
	}
}
```

#### 成功返回

```jsonc
{
	"success": true
}
```

#### 错误返回

所有错误在本接口返回的均一样

```json
{"error":"Forbidden","status":403}{"success":true}
```

> 可能是巴西人毒品吃多了或者被足球精准命中后脑勺了导致大脑不太好使这里写错了但是这个响应体确实他妈的长这样，可能需要额外处理
> 
> ![proof](/images/bad-resp.png)

### POST `/flag/equip/{id}`

设置展示旗帜

#### 请求

* 需要 `j` 完成认证
* URL中的{id}参数为旗帜ID，所有旗帜ID和旗帜解锁检查参阅[旗帜](#旗帜)和[附录](#全部旗帜)
    - 示例URL（设置为中国国旗）：`/flag/equip/45`

#### 成功返回

```jsonc
{
	"success": true
}
```

#### 错误返回

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> 未解锁旗帜

### GET `/leaderboard/region/{mode}/{country}`

获得某个国家/地区的地区绘制排行榜（仅前50个）

#### 请求

* URL中的`mode`代表时间范围，是一个枚举，可以是以下任何一个值：
    - `today`
    - `week`
    - `month`
    - `all-time`
* URL中的`country`为地区ID，对应的表请参阅[附录](#全部旗帜)
* 示例URL（中国今天的城市排行榜）：`/leaderboard/region/today/45`

#### 成功返回：

```jsonc
[
  {
    // int: 排行榜ID，仅用于内部
    "id": 111006,
    // int: 地区名字
    "name": "Yongzhou",
    // int: 地区ID
    "cityId": 4205,
    // int: 地区编号
    "number": 1,
    // int: 国家/地区ID
    "countryId": 45,
    // int: 已绘制数量
    "pixelsPainted": 389274,
    // 最后一次绘制的经纬度
    "lastLatitude": 26.59347856637528,
    "lastLongitude": 111.63313476562497
  },
  {
    "id": 112043,
    "name": "Fuzhou",
    "cityId": 4381,
    "number": 11,
    "countryId": 45,
    "pixelsPainted": 307461,
    "lastLatitude": 25.21710750136907,
    "lastLongitude": 120.43010742187496
  }
}
```

### GET `/leaderboard/country/{mode}`

获得所有国家/地区排行榜，仅限前50个

#### 请求

* URL中的`mode`代表时间范围，是一个枚举，可以是以下任何一个值：
    - `today`
    - `week`
    - `month`
    - `all-time`
* 示例URL（今天的国家地区排行榜）：`/leaderboard/country/today`

#### 成功返回

````jsonc
[
  {
    // int: 国家地区ID，参阅附录获得全部
    // 此处的235对应美国
    "id": 235,
    "pixelsPainted": 40724480
  },
  {
    "id": 181,
    "pixelsPainted": 39226725
  }
]
````

### GET `/leaderboard/player/{mode}`

获得全球玩家排行榜，仅限前50个

#### 请求

* URL中的`mode`代表时间范围，是一个枚举，可以是以下任何一个值：
    - `today`
    - `week`
    - `month`
    - `all-time`
* 示例URL（今天的玩家排行榜）：`/leaderboard/player/today`

#### 成功返回

```jsonc
[
  {
    // int: 用户ID
    "id": 8883244,
    // string: 用户名
    "name": "Tightmatt Cousin",
    // int: Alliance ID，如果是0则代表没有
    "allianceId": 0,
    // string: Alliance名字，如果没有则是空字符串
    "allianceName": "",
    // int: 已装备旗帜，旗帜列表参考附录，如果没有则是0
    "equippedFlag": 155,
    // int: 已绘制的像素数量
    "pixelsPainted": 64451,
    // string: 头像URL或Base64，可通过是否以data:image/png;base64,开头判断，如果没有头像则没有这个字段
    "picture": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAbklEQVR42qxTQQrAMAhbpN/e+/as7LKBjLRGOkGQ0mhM0zg2w2nAJ2XAAC8x7gpwVqCgi8zkvFhqAEEdKW2x6IoaxfSZqHjrYYhFcYfOM3IGythoGAeqHouJ33Mq1ihc13Vuq9k/sf2d7wAAAP//U48dVi53OIQAAAAASUVORK5CYII=",
    // string: discord用户名
    "discord": "co."
  },
  {
    "id": 2235271,
    "name": "( ˘ ³˘) ",
    "allianceId": 0,
    "allianceName": "",
    "equippedFlag": 0,
    "pixelsPainted": 39841,
    "discord": "bittenonce"
  }
]
```

### GET `/leaderboard/alliance/{mode}`

获得全球Alliance排行榜，仅限前50个。

#### 请求

* URL中的`mode`代表时间范围，是一个枚举，可以是以下任何一个值：
    - `today`
    - `week`
    - `month`
    - `all-time`
* 示例URL（今天的Alliance排行榜）：`/leaderboard/alliance/today`

#### 成功返回：

```jsonc
[
  {
    // int: Alliance ID
    "id": 165,
    // string: Alliance名字
    "name": "bapo",
    // int: 已绘制像素数量
    "pixelsPainted": 771030
  },
  {
    "id": 29246,
    "name": "BROP Enterprises",
    "pixelsPainted": 507885
  }
]
```

### GET `/leaderboard/region/players/{city}/{mode}`

获得某个城市的玩家排行榜，仅限前50个。

#### 请求

* URL中的`mode`代表时间范围，是一个枚举，可以是以下任何一个值：
    - `today`
    - `week`
    - `month`
    - `all-time`
* URL中的`city`是城市ID，暂时没有一个明确的列表对应，因为城市太他妈多了。
* 示例URL（深圳玩家总排行榜）：`/leaderboard/region/players/114594/all-time`

#### 成功返回

```jsonc
[
  {
    "id": 1997928,
    "name": "宵崎奏",
    "allianceId": 593067,
    "allianceName": "匠の心",
    "pixelsPainted": 189818,
    "equippedFlag": 98,
    "picture": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA+ElEQVR42mJiQAP/ocBh8pP/GgHzwGwQDWOjq2dE1+w45SnDi727GCSc3VAUgsRg4MaGJLg+RpAmRkZGRphmQgDdICZkm7EpRtYAAiCXIbsOxWZkp2PzBjaXMDGQAbaJq8INJ8oAZG+ANCMDJnT/wfy90uAWmN6fI41hiNfL23CDmNBtAml8rsnIoFffDhe/vj4RLAaSR9YMNwBmCwhomumgaEQGIMORXUFyIMJcBdOM04APbQkExUDeASUkRvSEBJK4lMaGYcD1U1cYwi+owQMalpwZkfOBZuB8uAZQoIFpwywGt0nGDG9EkrDmBYoBE6UGAAIAAP//HhiiI4AXzBcAAAAASUVORK5CYII=",
    "discord": "思い出を取り戻して"
  },
  {
    "id": 7730493,
    "name": "$_0_U_/\\/\\_4",
    "allianceId": 597328,
    "allianceName": "義工",
    "pixelsPainted": 109076,
    "equippedFlag": 98,
    "picture": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAB+ElEQVR42pRTTWgTURD+9rmKBUPbg1RBDyslNAEriU3F3pTUS0FoQREEL3pQ8SwqRRREUPDkT3qwF6EIgvFUDyZ4EFKhG6om4IoElCpCBOmGoKiJrHwTJ27MJX7wmG/nzZv53sxbOygsBI1aAGJ9vwXlikrgY+mFJ/z4vomufaOOeyvPZZNJwmi4H3EsuVfWCn7gXxglDCj3/5QktEwoqlLbRAW/+/xvUkCL0BqtTuysbeiqEBsYbMk/Oy3c89dkaRLD7HpXtUxERQRlaw94MMy51iXizqWJREycN7OPUfbeYfW7DzMyhM8bf4l/ynGAT19x8doczk0fkriZeAJVtwLr9eIt6eJC9imOzuzHsDUgKvQ7NjkCuFVRwh4QVMGJUIGtjsPboxgvvpWAenqPHG4jNYSkW0Wk+FI+C0FEfMitwXBMqWYEoxfSclDvx8pUE05CLI9FZTLaYLNcetURVD9/sCWbY0pv6ZiI7mkjS7kyjJXcgf8FJzR76o4oMZwACR294vLDLBxnq7xSeUiF+UVx9IovH1bFuna9leD9YB+o5O6RG+2g+euPUHlwXxa5gjG7N20WfmL2tmXHp85YJw+MtX8xXuVK5rQ8XcXwH1u6mhc7ProLmWf5vz/T3JOipZ3l/DUwDC/3Bs3JqPDMUl7OkP8OAAD//6QS5QpYPtjuAAAAAElFTkSuQmCC",
    "discord": "soumasandesu"
  }
]
```

> 字段定义参阅[/leaderboard/player/{mode}](#get-leaderboardplayermode)

### GET `/leaderboard/region/alliances/{city}/{mode}`

获得某个城市的Alliance排行榜，仅限前50个。

#### 请求

* URL中的`mode`代表时间范围，是一个枚举，可以是以下任何一个值：
    - `today`
    - `week`
    - `month`
    - `all-time`
* URL中的`city`是城市ID，暂时没有一个明确的列表对应，因为城市太他妈多了。
* 示例URL（深圳Alliance总排行榜）：`/leaderboard/region/alliances/114594/all-time`

#### 成功返回

```jsonc
[
  {
    "id": 1,
    "name": "Team ReaIB",
    "pixelsPainted": 856069
  },
  {
    "id": 1,
    "name": "Team RealB",
    "pixelsPainted": 658302
  }
]
```
> 字段定义参阅[/leaderboard/alliance/{mode}](#get-leaderboardalliancemode)

### GET `/s0/tile/random`

获得一个随机的已经绘制的像素

#### 成功返回

```jsonc
{
    // 像素位置（相对于Tile）
	"pixel": {
		"x": 764,
		"y": 676
	},
	// Tile位置
	"tile": {
		"x": 1781,
		"y": 749
	}
}
```

Tile和像素位置之间的关系，参阅[瓦片](#瓦片)

### GET `/s0/pixel/{tileX}/{tileY}?x={x}&y={y}`

获得某个像素点的信息

#### 请求

* URL中的tileX和tileY需要为瓦片坐标，相关信息参阅[瓦片](#瓦片)
* x和y参数为像素相对坐标，需要在1024范围内
* 示例URL（深圳的某个位置）：`/s0/pixel/1672/892?x=668&y=265`

#### 成功返回

已绘制

```jsonc
{
    // object: 绘制者信息
	"paintedBy": {
	    // int: 用户ID
		"id": 1,
		// string: 用户名
		"name": "崔龙海",
		// int: Alliance ID，如果没有则是0
		"allianceId": 1,
		// string: Alliance名字，如果没有则是空字符串
		"allianceName": "Team ReaIB",
		// int: 旗帜ID，对应关系参阅附录
		"equippedFlag": 0
	},
	// object: 区域信息
	"region": {
	    // int: 信息ID，内部使用
		"id": 114594,
		// int: 城市ID
		"cityId": 4263,
		// int: 城市名字
		"name": "Shenzhen",
		// int: 区域编号
		"number": 2,
		// int: 国家/地区ID
		"countryId": 45
	}
}
```

未绘制（透明）

```jsonc
{
	"paintedBy": {
		"id": 0,
		"name": "",
		"allianceId": 0,
		"allianceName": "",
		"equippedFlag": 0
	},
	"region": {
		"id": 114594,
		"cityId": 4263,
		"name": "Shenzhen",
		"number": 2,
		"countryId": 45
	}
}
```

### GET `/files/s0/tiles/{tileX}/{tileY}.png`

获得某个[瓦片](#瓦片)的贴图

#### 请求

* URL中的tileX和tileY需要为瓦片坐标，相关信息参阅[瓦片](#瓦片)
* 示例URL：`/files/s0/tiles/1672/892.png`

#### 成功返回

![ex](/images/892.png)

### POST `/s0/pixel/{tileX}/{tileY}`

绘制像素

需要添加反作弊请求头`x-pawtect-variant`和`x-pawtect-token`，请参阅[反作弊](#反作弊)

#### 请求

* 需要`j`完成认证
* URL中的tileX和tileY需要为瓦片坐标，相关信息参阅[瓦片](#瓦片)
* 示例URL：`/s0/pixel/1672/892`

#### 示例请求

```jsonc
{
    // array: 绘制的颜色ID，每个值对应一个像素
	"colors": [49, 49, 49, 49, 49, 49],
	// array: 绘制的坐标，格式为x, y, x, y，按照 (x, y) 成对出现
	// 坐标顺序与 colors 一一对应，即第N个颜色应用于第N个坐标
	"coords": [
      140, 359, 
      141, 359, 
      141, 358, 
      142, 358, 
      143, 358, 
      143, 357
    ],
    // string: 验证码token
	"t": "0.xxxx",
	// string: 浏览器指纹
	"fp": "xxxx"
}
```

> `colors`为绘制的颜色代码和`coords`一一对应，参阅[颜色](#颜色)和[附录](#全部颜色表)
> 
> 在绘制的颜色跨域多个[瓦片](#瓦片)时候会分多次请求

#### 成功返回

```jsonc
{
	"painted": 6
}
```

#### 错误返回

```jsonc
{
	"error": "refresh",
	"status": 403
}
```

> 验证码token或pawtect无效

### POST `/report-user`

<img src="/images/staffscreen.png" align="right" width="500">

举报用户，举报时客户端会渲染一张截图，客服在查看时可以看到客户端的截图和现场截图

客服可以看见被举报的用户的IP下的所有用户。


#### 请求

* 需要`j`完成认证
* 请求体为multipart body
    - `reportedUserId`: 举报的用户ID
    - `latitude`: 纬度
    - `longitude`: 经度
    - `zoom`: 缩放
    - `reason`: 举报原因
    - `notes`: 举报文本，用户可以主动输入
    - `image`: 客户端渲染的一张举报截图会显示在客服页面

### 示例请求

CURL

```bash
curl -X POST "https://backend.wplace.live/report-user" \
  -H "Content-Type: multipart/form-data" \
  -F "reportedUserId=1" \
  -F "latitude=22.544484678446224" \
  -F "longitude=114.09375473639432" \
  -F "zoom=15.812584063490982" \
  -F "reason=griefing" \
  -F "notes=Messed up artworks for no reason" \
  -F "image=@图片;type=image/jpeg"
```

原始请求体

```text
------boundary
Content-Disposition: form-data; name="reportedUserId"

1
------boundary
Content-Disposition: form-data; name="latitude"

22.544484678446224
------boundary
Content-Disposition: form-data; name="longitude"

114.09375473639432
------boundary
Content-Disposition: form-data; name="zoom"

15.812584063490982
------boundary
Content-Disposition: form-data; name="reason"

griefing
------boundary
Content-Disposition: form-data; name="notes"

Messed up artworks for no reason
------boundary
Content-Disposition: form-data; name="image"; filename="report-1758232933710.jpeg"
Content-Type: image/jpeg

(binary file data)
------boundary--
```

## 反作弊

### 

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

```jsonc
{
  "error": "Bad Request",
  "status": 400
}
```

> 请求格式错误

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