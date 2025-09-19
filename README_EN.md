# Wplace Protocol

Analysis of [Wplace](https://wplace.live)'s technology stack, protocols, and interfaces.

Disclaimer: Some unreferenced interfaces are not listed because they may be removed at any time. If there are any errors, please contact me in time.

Table of contents:

- [Concepts and Systems](#concepts-and-systems)
  - [Map](#map)
  - [Tiles](#tiles)
    - [Calculating the corresponding longitude and latitude](#calculating-the-corresponding-longitude-and-latitude)
    - [Related endpoints](#related-endpoints)
  - [Colors](#colors)
    - [Related endpoints](#related-endpoints-1)
  - [Flags](#flags)
    - [Related endpoints](#related-endpoints-2)
  - [Levels](#levels)
  - [Store](#store)
    - [Related endpoints](#related-endpoints-3)
- [Protocol](#protocol)
  - [Authentication](#authentication)
  - [Cookie](#cookie)
  - [GET `/me`](#get-me)
  - [POST `/me/update`](#post-meupdate)
  - [GET `/me/profile-pictures`](#get-meprofile-pictures)
  - [POST `/me/profile-picture/change`](#post-meprofile-picturechange)
  - [POST `/me/profile-picture`](#post-meprofile-picture)
  - [GET `/alliance`](#get-alliance)
  - [POST `/alliance`](#post-alliance)
  - [POST `/alliance/update-description`](#post-allianceupdate-description)
  - [GET `/alliance/invites`](#get-allianceinvites)
  - [GET `/alliance/join/{invite}`](#get-alliancejoininvite)
  - [POST `/alliance/update-headquarters`](#post-allianceupdate-headquarters)
  - [GET `/alliance/members/{page}`](#get-alliancememberspage)
  - [GET `/alliance/members/banned/{page}`](#get-alliancemembersbannedpage)
  - [POST `/alliance/give-admin`](#post-alliancegive-admin)
  - [POST `/alliance/ban`](#post-allianceban)
  - [POST `/alliance/unban`](#post-allianceunban)
  - [GET `/alliance/leaderboard/{mode}`](#get-allianceleaderboardmode)
  - [POST `/favorite-location`](#post-favorite-location)
  - [POST `/favorite-location/delete`](#post-favorite-locationdelete)
  - [POST `/purchase`](#post-purchase)
  - [POST `/flag/equip/{id}`](#post-flagequipid)
  - [GET `/leaderboard/region/{mode}/{country}`](#get-leaderboardregionmodecountry)
  - [GET `/leaderboard/country/{mode}`](#get-leaderboardcountrymode)
  - [GET `/leaderboard/player/{mode}`](#get-leaderboardplayermode)
  - [GET `/leaderboard/alliance/{mode}`](#get-leaderboardalliancemode)
  - [GET `/leaderboard/region/players/{city}/{mode}`](#get-leaderboardregionplayerscitymode)
  - [GET `/leaderboard/region/alliances/{city}/{mode}`](#get-leaderboardregionalliancescitymode)
  - [GET `/s0/tile/random`](#get-s0tilerandom)
  - [GET `/s0/pixel/{tileX}/{tileY}?x={x}&y={y}`](#get-s0pixeltilextileyxxyy)
  - [GET `/files/s0/tiles/{tileX}/{tileY}.png`](#get-filess0tilestilextileypng)
  - [POST `/s0/pixel/{tileX}/{tileY}`](#post-s0pixeltilextiley)
  - [POST `/report-user`](#post-report-user)
- [Anti Cheat](#anti-cheat)
- [Appendix](#appendix)
  - [Common API Errors](#common-api-errors)
  - [All Color Tables](#all-color-tables)
  - [BitMap Java implementation](#bitmap-java-implementation)
  - [All Flags](#all-flags)

## Concepts and Systems

_Most names are subjective and do not necessarily reflect the same naming conventions as in source code or other wplace projects._

### Map

<img src="/images/projection.JPG" align="right" width="200">

> Keywords: `Map / Canvas / World`

Map refers to the overall canvas of Wplace. The map is rendered in the [Mercator (Web Mercator)](https://en.wikipedia.org/wiki/Mercator_projection) projection, using the [OpenFreeMap](https://openfreemap.org/) map, which uses the Liberty Style. The map consists of `2048x2048` pixels, or `4,194,304` [tiles](#tiles), these tiles are overlaid on the map using Canvas.

Most of the locations on the map are unclaimed/disputed in reality, such as the Pacific Ocean, which is divided into the countries or regions with the nearest landmass.

The total number of pixels in the map is `4,194,304,000,000` (approximately 4.1 trillion / 4.1 trillion / 4.1 trillion).

### Tiles

> Keywords: `Tile / Chunk`

A tile is the smallest unit of the wplace rendering canvas. Each tile is a `1000×1000` PNG image on the server, containing `1,000,000` pixels.

The data type corresponding to the tile is `Vec2i`, namely `x` and `y`.

The relative coordinates mentioned in the API are the coordinates starting from 0 of the tile.

#### Calculating the corresponding longitude and latitude

The entire [map](#map) has `2048` tiles in both the horizontal and vertical directions. The `Zoom` value can be calculated using this:

```java
int n = 2048; // Number of tiles
int z = (int) (Math.log(n) / Math.log(2)); // Calculate Zoom using the change of base formula.
```

Using this formula, we can calculate the zoom to be **approximately** `11`, and then use the following algorithm to calculate the latitude and longitude:

```java
double n = Math.pow(2.0, 11); // zoom is 11
double lon = (x + 0.5) / n * 360.0 - 180.0;
double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * (y + 0.5) / n)));
double lat = Math.toDegrees(latRad);
```

Among them, `lon` and `lat` are the values of latitude and longitude

> Formula reference from: [Slippy map tilenames](https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames)

#### Related Endpoints

- [/s0/pixel/{tileX}/{tileY}?x={x}&y={y}](#get-s0pixeltilextileyxxyy)
- [/s0/pixel/{tileX}/{tileY}](#post-s0pixeltilextiley)

### Colors

> Keywords: `Color / Palette`

Wplace provides 64 colors, the first 32 are free colors, and the latter 32 each require `2,000` Droplets to unlock.

To check if a color is unlocked, the frontend uses a bitmask check to examine `extraColorsBitmap`, which is a field in the JSON returned by the user profile interface.

The checking logic is:

```java
int extraColorsBitmap = 0;
int colorId = 63; // Color ID to check
boolean unlocked;

if (colorId < 32) { // Skip the first 32 as the first 32 colors are free
    unlocked = true;
} else {
    int mask = 1 << (colorId - 32);
    unlocked = (extraColorsBitmap & mask) != 0;
}
```

> Disclaimer: This code is Java code derived by the author based on analysis of the obfuscated JS code in Wplace, not the original code.

For color codes, please check [Appendix](#all-color-tables)

#### Related Endpoints

- [/me](#get-me)
- [/purchase](#post-purchase)

### Flags

> Keywords: `Flag`

Wplace contains 251 flags. After purchasing a flag, you can save 10% pixels when drawing in the corresponding region. The price of a flag is `20,000` Droplets.

Whether a flag is unlocked is implemented through a custom BitMap. Here is the JS code for this BitMap:

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

Readable Java code for BitMap can be found in [Appendix](#bitmap-java-implementation)

After the frontend obtains the `flagsBitmap` field through the user profile interface, it decodes it from Base64 to Bytes and then passes it to BitMap to read whether a flag ID has been unlocked.

For all flag codes, please refer to [Appendix](#all-flags)

#### Related Endpoints

- [/me](#get-me)
- [/purchase](#post-purchase)
- [/flag/equip/{id}](#post-flagequipid)

### Levels

> Keywords: `Level`

Levels can be calculated based on the painted pixels

```java
double totalPainted = 1; // Number of pixels already painted
double base = Math.pow(30, 0.65);
double level = Math.pow(totalPainted, 0.65) / base;
```

Each level up will gain `500` droplets and increase `2` maximum pixels

### Store

> Keywords: `Store / Purchase`

Items can be purchased with the in-game virtual currency Droplet in the store. The following is a list of items:

| Item ID | Item Name             | Price (Droplet) | Variants     |
|---------|-----------------------|-----------------|--------------|
| `70`    | +5 Max. Charges       | `500`           | None         |
| `80`    | +30 Paint Charges     | `500`           | None         |
| `100`   | Unlock Paid Colors    | `2000`          | [Color ID](#colors) |
| `110`   | Unlock Flag           | `20000`         | [Flag ID](#flags) |

#### Related Endpoints

- [/purchase](#post-purchase)

Other item IDs are reserved for recharge items (cash payment)

## Protocol

Unless otherwise specified, the URL host is `backend.wplace.live`

For common API errors, refer to [Appendix](#common-api-errors)

### Authentication

Authentication is implemented through the field `j` in Cookie. After logging in, the backend will save the [Json Web Token](https://en.wikipedia.org/wiki/JSON_Web_Token) to the Cookie. Subsequent requests to `wplace.live` and `backend.wplace.live` will carry this Cookie.

The token is an encoded text, not an ordinary random string. Some information can be decoded through [jwt.io](https://jwt.io) or any JWT tool.

```json
{
  "userId": 1,
  "sessionId": "",
  "iss": "wplace",
  "exp": 1758373929,
  "iat": 1755781929
}
```

The `exp` field is the expiration timestamp, and the expiration time can be determined only through the token.

### Cookie

Generally speaking, only the `j` Cookie is required to request the interface. However, if the server is under high load, the developer will enable [Under Attack mode](https://developers.cloudflare.com/fundamentals/reference/under-attack-mode/). If Under Attack mode is enabled, an additional valid `cf_clearance` Cookie is required, otherwise a Cloudflare challenge will pop up.

You need to ensure that when automatic programs initiate requests, most fields in the request headers (such as `User-Agent`, `Accept-Language`, etc.) are consistent with the browser you obtained `cf_clearance` from, otherwise the verification will not pass and the challenge will still pop up.

### GET `/me`

Get user information

#### Request

- Requires authentication with `j`

#### Successful Return

```jsonc
{
    // int: Alliance ID
    "allianceId": 1, 
    // enum: Alliance permissions
    // admin/member
    "allianceRole": "admin",
    // boolean: Whether banned
    "banned": false,
    // object: Pixel information
    "charges": {
        // int: Recovery interval of pixels, in milliseconds, 30000 milliseconds is 30 seconds
        "cooldownMs": 30000,
        // float: Remaining pixels
        "count": 35.821833333333586,
        // float: Maximum number of pixels
        "max": 500
    },
    // string: ISO-3166-1 alpha-2 region code
    // Reference: https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
    "country": "JP",
    // string: Discord username
    "discord": "",
    // int: Remaining droplets
    "droplets": 75,
    // int: Equipped flag
    "equippedFlag": 0,
    // object: A/B testing markers, the internal meaning is unclear
    // For example, the variant value is koala, the internal meaning is unclear, just a code name.
    // But it will be transmitted with the request header. If the variant of 2025-09_pawtect is disabled, pawtect-token will not be sent.
    // This indicates that some users have not enabled the new security mechanism.
    "experiments": {
        "2025-09_pawtect": {
            "variant": "koala"
        }
    },
    // int: extraColorsBitmap, see the color section to understand its function.
    "extraColorsBitmap": 0,
    // array: Favorite locations
    "favoriteLocations": [
        {
            "id": 1,
            "name": "",
            "latitude": 46.797833514893085,
            "longitude": 0.9266305280273432
        }
    ],
    // string: List of unlocked flags, see the flag section to understand its function.
    "flagsBitmap": "AA==",
    // enum: Generally does not appear, only displayed if you have permissions
    // moderator/global_moderator/admin
    "role": "",
    // int: User ID
    "id": 1,
    // boolean: Whether there is a purchase, if so, the order list will be displayed in the menu
    "isCustomer": false,
    // float: Level
    "level": 94.08496005353335,
    // int: Maximum number of favorites, default is 15, no way to improve has been found yet
    "maxFavoriteLocations": 15,
    // string: Username
    "name": "username",
    // boolean: Whether phone number verification is required, if so, a phone verification window will pop up when visiting
    "needsPhoneVerification": false,
    // string: Avatar URL or base64, need to judge according to the prefix (e.g. data:image/png;base64,)
    "picture": "",
    // int: Number of pixels already painted
    "pixelsPainted": 114514,
    // boolean: Whether to display your last painted location on the alliance page
    "showLastPixel": true,
    // string: Your unbanned timestamp, if it is 1970, it means you are not banned or have been permanently banned.
    "timeoutUntil": "1970-01-01T00:00:00Z"
}
```

### POST `/me/update`

Update current user's personal information

#### Request

* Requires authentication with `j`

#### Request Example

```jsonc
{
    // string: User nickname
    "name": "cubk",
    // boolean: Whether to display the last pixel on alliance
    "showLastPixel": true,
    // Discord username
    "discord": "_cubk"
}
```

#### Successful Return

```jsonc
{
    "success": true
}
```

#### Error Return

```jsonc
{
    "error": "The name has more than 16 characters",
    "status": 400
}
```

> Invalid request body

### GET `/me/profile-pictures`

Get avatar list

A person can have multiple avatars (add one `20,000` Droplets), and then can change to any avatar in the avatar list at any time

#### Request

* Requires authentication with `j`

#### Successful Return

```jsonc
// array: All avatars
[
    {
        // int: Avatar ID
        "id": 0,
        // string: Avatar URL or Base64, can be judged by whether it starts with data:image/png;base64,
        "url": ""
    }
]
```

> If you don't have any avatars, an empty array will be returned

### POST `/me/profile-picture/change`

Change avatar

#### Request

* Requires authentication with `j`

#### Request Example

Change existing custom avatar

```jsonc
{
    // int: Avatar ID, you need to ensure you have added this avatar
	"pictureId": 1
}
```

Reset avatar

```jsonc
{}
```

> Requesting an empty JsonObject can reset the avatar

#### Successful Return

```jsonc
{
	"success": true
}
```

### POST `/me/profile-picture`

Upload avatar

#### Request

* Requires authentication with `j`
* Request body is Multipart File: `image`

#### Successful Return

```jsonc
{
	"success": true
}
```

#### Error Return

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

### GET `/alliance`

Get Alliance information

#### Request

* Requires authentication with `j`

#### Successful Return

```jsonc
{
	// string: Alliance introduction
	"description": "CCB",
	// object: Headquarters
	"hq": {
		"latitude": 22.535013525851937,
		"longitude": 114.01152903098966
	},
	// int: Alliance ID
	"id": 453128,
	// int: Number of members
	"members": 263,
	// string: Name
	"name": "Team RealB",
	// string: Total painted
	"pixelsPainted": 1419281,
	// enum: Your permissions
	// admin/member
	"role": "admin"
}
```

#### Error Return

```jsonc
{
	"error": "Not Found",
	"status": 404
}
```

> Not joined any Alliance

### POST `/alliance`

Create an Alliance

#### Request

* Requires authentication with `j`

#### Request Example

```jsonc
{
    // string: Alliance name, cannot be duplicated.
	"name": "Team RealB"
}
```

#### Successful Return

```jsonc
{
    // int: Created Alliance ID
	"id": 1
}
```

#### Error Return

```jsonc
{
	"error": "name_taken",
	"status": 400
}
```

> Alliance name is already taken

```jsonc
{
    "error": "Forbidden",
    "status": 403
}
```

> Already have an Alliance but still trying to create, normally will not trigger.

### POST `/alliance/update-description`

Update Alliance introduction

#### Request

* Requires authentication with `j`

#### Successful Return

```jsonc
{
	"success": true
}
```

#### Error Return

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### GET `/alliance/invites`

Get Alliance invitation links

#### Request

* Requires authentication with `j`

#### Successful Return

```jsonc
// array: Alliance invitation links, usually only one and the format is UUID
[
    "fe7c9c32-e95a-4f5f-a866-554cde2149c3"
]
```

#### Error Return

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### GET `/alliance/join/{invite}`

Join Alliance through Invite UUID, see [/alliance/invites](#get-allianceinvites) to get Invite UUID

#### Request

* Requires authentication with `j`
* The {invite} parameter in the URL is the invitation UUID
  - Example URL (set to Chinese flag): `/alliance/join/fe7c9c32-e95a-4f5f-a866-554cde2149c3`

#### Successful Return

```jsonc
{
    "success": "true"
}
```

> If the target to join is consistent with your existing Alliance, it will also return success

#### Error Return

```jsonc
{
    "error": "Not Found",
    "status": 404
}
```

> Target Alliance not found

```jsonc
{
  "error": "Already Reported",
  "status": 208
}
```

> Already joined an Alliance

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> Banned by this Alliance

### POST `/alliance/update-headquarters`

Update Alliance headquarters

#### Request

* Requires authentication with `j`

#### Request Example

```jsonc
{
	"latitude": 22.537655528880563,
	"longitude": 114.0274942853182
}
```

#### Successful Return

```jsonc
{
	"success": true
}
```

#### Error Return


```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### GET `/alliance/members/{page}`

Get Alliance member list, with pagination system, may need to get multiple pages if members exceed 50

#### Request

* Requires authentication with `j`
* The {page} parameter in the URL is the page number, starting from 0
  - Example URL (get the first page): `/alliance/members/0`

#### Successful Return

```jsonc
{
    // array: Up to 50 per page
	"data": [{
	    // int: User ID
		"id": 1,
		// string: Username
		"name": "cubk'",
		// enum: Permissions
		// admin/member
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
	// boolean: Whether there is a next page
	"hasNext": true
}
```

#### Error Return

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin


### GET `/alliance/members/banned/{page}`

Get the list of banned members of the Alliance, with a pagination system. If the members exceed 50, you may need to get multiple pages.

Banned members cannot rejoin the Alliance.

#### Request

* Requires authentication with `j`
* The {page} parameter in the URL is the page number, starting from 0
  - Example URL (get the first page): `/alliance/members/banned/0`

#### Successful Return

```jsonc
{
	"data": [{
		"id": 1,
		"name": "SuckMyDick"
	}],
	"hasNext": false
}
```

> Similar to the regular member interface, but without `role`, because banned members are no longer in the alliance

#### Error Return

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### POST `/alliance/give-admin`

Promote a member to Admin, cannot be demoted

#### Request

* Requires authentication with `j`

#### Request Example

```jsonc
{
    // int: User ID to promote
	"promotedUserId": 1
}
```

#### Successful Return

This interface has no return, response code is `200` for success

#### Error Return

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### POST `/alliance/ban`

Kick out and ban a member

After banning, if the ban is not lifted, the member cannot rejoin

#### Request

* Requires authentication with `j`

#### Request Example
```jsonc
{
    // int: User ID to kick out or ban
	"bannedUserId": 1
}
```

#### Successful Return

```jsonc
{
	"success": true
}
```

#### Error Return

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### POST `/alliance/unban`

Unban a member. After unbanning, they will not automatically return to the Alliance, but can only rejoin.

#### Request

* Requires authentication with `j`

#### Request Example

```jsonc
{
    // int: User ID to unban
	"unbannedUserId": 1
}
```

#### Successful Return

```jsonc
{
	"success": true
}
```

#### Error Return

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### GET `/alliance/leaderboard/{mode}`

Get the player leaderboard within the Alliance, limited to the top 50.

#### Request

* Requires authentication with `j`
* The `mode` in the URL represents the time range, which is an enum and can be any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* Example URL (today's leaderboard): `/alliance/leaderboard/today`

#### Successful Return

```jsonc
[
  {
    // int: User ID
    "userId": 10815100,
    // string: Username
    "name": "做爱",
    // int: Flag ID, see appendix for flag list
    "equippedFlag": 0,
    // int: Number of painted pixels
    "pixelsPainted": 32901,
    // Latitude and longitude of the last paint, if the user has turned off showLastPixel, these two fields will not be present
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

Favorite a location

#### Request

* Requires authentication with `j`

#### Request Example

```jsonc
{
	"latitude": 22.5199456234827,
	"longitude": 114.02428677802732
}
```

#### Successful Return

```jsonc
{
    // int: Favorite ID
	"id": 1,
	"success": true
}
```

#### Error Return

```jsonc
{
  "error": "Forbidden",
  "status": 403
}
```

> Number of favorites exceeds maxFavoriteLocations


### POST `/favorite-location/delete`

Unfavorite a location

#### Request

* Requires authentication with `j`

#### Request Example

```jsonc
{
    // int: Favorite ID
	"id": 1
}
```

#### Successful Return

```jsonc
{
    "success": true
}
```

> Any ID will return success even if it is not favorited or does not exist

### POST `/purchase`

Purchase items, for related definitions please read the [Store](#store) section

#### Request

* Requires authentication with `j`

#### Request Example

```jsonc
{
    // object: Fixed field product
	"product": {
	    // int: Item id
		"id": 100,
		// int: Purchase quantity, for Paint Charges/Max Charge multiple can be purchased
		"amount": 1,
		// int: Variant value, some items have variants, if no variant this value is not needed
		"variant": 49
	}
}
```

#### Successful Return

```jsonc
{
	"success": true
}
```

#### Error Return

All errors returned by this interface are the same

```json
{"error":"Forbidden","status":403}{"success":true}
```

> It's possible that the Brazilian person ate too many drugs or was hit in the back of the head by football causing brain damage and wrote this wrong, but this response body actually looks like this, may need additional processing
> 
> ![proof](/images/bad-resp.png)

### POST `/flag/equip/{id}`

Set display flag

#### Request

* Requires authentication with `j`
* The {id} parameter in the URL is the flag ID, all flag IDs and flag unlock checks refer to [Flags](#flags) and [Appendix](#all-flags)
  - Example URL (set to Chinese flag): `/flag/equip/45`

#### Successful Return

```jsonc
{
	"success": true
}
```

#### Error Return

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> Flag not unlocked

### GET `/leaderboard/region/{mode}/{country}`

Get the regional painting leaderboard for a country/region (top 50 only)

#### Request

* The `mode` in the URL represents the time range, which is an enum and can be any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* The `country` in the URL is the region ID, the corresponding table please refer to [Appendix](#all-flags)
* Example URL (China's city leaderboard today): `/leaderboard/region/today/45`

#### Successful Return:

```jsonc
[
  {
    // int: Leaderboard ID, for internal use only
    "id": 111006,
    // int: Region name
    "name": "Yongzhou",
    // int: Region ID
    "cityId": 4205,
    // int: Region number
    "number": 1,
    // int: Country/region ID
    "countryId": 45,
    // int: Number of painted pixels
    "pixelsPainted": 389274,
    // Latitude and longitude of the last paint
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
]
```

### GET `/leaderboard/country/{mode}`

Get the leaderboard of all countries/regions, limited to the top 50

#### Request

* The `mode` in the URL represents the time range, which is an enum and can be any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* Example URL (today's country/region leaderboard): `/leaderboard/country/today`

#### Successful Return

````jsonc
[
  {
    // int: Country/region ID, refer to appendix for all
    // The 235 here corresponds to the United States
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

Get the global player leaderboard, limited to the top 50

#### Request

* The `mode` in the URL represents the time range, which is an enum and can be any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* Example URL (today's player leaderboard): `/leaderboard/player/today`

#### Successful Return

```jsonc
[
  {
    // int: User ID
    "id": 8883244,
    // string: Username
    "name": "Tightmatt Cousin",
    // int: Alliance ID, if 0 means none
    "allianceId": 0,
    // string: Alliance name, if none then empty string
    "allianceName": "",
    // int: Equipped flag, see appendix for flag list, if none then 0
    "equippedFlag": 155,
    // int: Number of painted pixels
    "pixelsPainted": 64451,
    // string: Avatar URL or Base64, can be judged by whether it starts with data:image/png;base64,, if no avatar then no this field
    "picture": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAbklEQVR42qxTQQrAMAhbpN/e+/as7LKBjLRGOkGQ0mhM0zg2w2nAJ2XAAC8x7gpwVqCgi8zkvFhqAEEdKW2x6IoaxfSZqHjrYYhFcYfOM3IGythoGAeqHouJ33Mq1ihc13Vuq9k/sf2d7wAAAP//U48dVi53OIQAAAAASUVORK5CYII=",
    // string: Discord username
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

Get the global Alliance leaderboard, limited to the top 50.

#### Request

* The `mode` in the URL represents the time range, which is an enum and can be any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* Example URL (today's Alliance leaderboard): `/leaderboard/alliance/today`

#### Successful Return

```jsonc
[
  {
    // int: Alliance ID
    "id": 165,
    // string: Alliance name
    "name": "bapo",
    // int: Number of painted pixels
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

Get the player leaderboard for a city, limited to the top 50.

#### Request

* The `mode` in the URL represents the time range, which is an enum and can be any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* The `city` in the URL is the city ID, there is no clear list correspondence yet, because there are too many cities.
* Example URL (Shenzhen player overall leaderboard): `/leaderboard/region/players/114594/all-time`

#### Successful Return

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

> Field definitions refer to [/leaderboard/player/{mode}](#get-leaderboardplayermode)

### GET `/leaderboard/region/alliances/{city}/{mode}`

Get the Alliance leaderboard for a city, limited to the top 50.

#### Request

* The `mode` in the URL represents the time range, which is an enum and can be any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* The `city` in the URL is the city ID, there is no clear list correspondence yet, because there are too many cities.
* Example URL (Shenzhen Alliance overall leaderboard): `/leaderboard/region/alliances/114594/all-time`

#### Successful Return

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
> Field definitions refer to [/leaderboard/alliance/{mode}](#get-leaderboardalliancemode)

### GET `/s0/tile/random`

Get a random painted pixel

#### Successful Return

```jsonc
{
    // Pixel position (relative to Tile)
	"pixel": {
		"x": 764,
		"y": 676
	},
	// Tile position
	"tile": {
		"x": 1781,
		"y": 749
	}
}
```

The relationship between Tile and pixel position, refer to [Tiles](#tiles)

### GET `/s0/pixel/{tileX}/{tileY}?x={x}&y={y}`

Get information about a pixel point

#### Request

* tileX and tileY in the URL need to be tile coordinates, related information refer to [Tiles](#tiles)
* x and y parameters are pixel relative coordinates, need to be within 1024 range
* Example URL (a location in Shenzhen): `/s0/pixel/1672/892?x=668&y=265`

#### Successful Return

Painted

```jsonc
{
    // object: Painter information
	"paintedBy": {
	    // int: User ID
		"id": 1,
		// string: Username
		"name": "崔龙海",
		// int: Alliance ID, if none then 0
		"allianceId": 1,
		// string: Alliance name, if none then empty string
		"allianceName": "Team ReaIB",
		// int: Flag ID, refer to appendix for correspondence
		"equippedFlag": 0
	},
	// object: Region information
	"region": {
	    // int: Information ID, for internal use
		"id": 114594,
		// int: City ID
		"cityId": 4263,
		// int: City name
		"name": "Shenzhen",
		// int: Region number
		"number": 2,
		// int: Country/region ID
		"countryId": 45
	}
}
```

Not painted (transparent)

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

Get the texture of a [tile](#tiles)

#### Request

* tileX and tileY in the URL need to be tile coordinates, related information refer to [Tiles](#tiles)
* Example URL: `/files/s0/tiles/1672/892.png`

#### Successful Return

![ex](/images/892.png)

### POST `/s0/pixel/{tileX}/{tileY}`

Paint pixels

Need to add anti-cheat request headers `x-pawtect-variant` and `x-pawtect-token`, please refer to [Anti-cheat](#anti-cheat)

#### Request

* Requires authentication with `j`
* tileX and tileY in the URL need to be tile coordinates, related information refer to [Tiles](#tiles)
* Example URL: `/s0/pixel/1672/892`

#### Request Example

```jsonc
{
    // array: Paint color IDs, each value corresponds to a pixel
	"colors": [49, 49, 49, 49, 49, 49],
	// array: Paint coordinates, format is x, y, x, y, appearing in pairs as (x, y)
	// The coordinate order corresponds one-to-one with colors, that is, the Nth color is applied to the Nth coordinate
	"coords": [
      140, 359, 
      141, 359, 
      141, 358, 
      142, 358, 
      143, 358, 
      143, 357
    ],
    // string: Captcha token
	"t": "0.xxxx",
	// string: Browser fingerprint
	"fp": "xxxx"
}
```

> `colors` corresponds one-to-one with the color codes in `coords`, refer to [Colors](#colors) and [Appendix](#all-color-tables)
>
> When painting colors across multiple [tiles](#tiles), multiple requests will be made
>
> For captcha token, refer to [Turnstile](#turnstile---captcha)
> For `fp`, refer to [Browser Fingerprint](#fingerprintjs---browser-fingerprint)
> For `x-pawtect-token` and `x-pawtect-variant`, refer to [pawtect](#pawtect)

#### Successful Return

```jsonc
{
	"painted": 6
}
```

#### Error Return

```jsonc
{
	"error": "refresh",
	"status": 403
}
```

> Captcha token or pawtect invalid

### POST `/report-user`

<img src="/images/staffscreen.png" align="right" width="500">

Report user. When reporting, the client will render a screenshot, and customer service can see the client's screenshot and live screenshot when viewing.

Customer service can see all users under the reported user's IP.

#### Request

* Requires authentication with `j`
* Request body is multipart body
  - `reportedUserId`: Reported user ID
  - `latitude`: Latitude
  - `longitude`: Longitude
  - `zoom`: Zoom
  - `reason`: Report reason
  - `notes`: Report text, users can actively enter
  - `image`: A report screenshot rendered by the client will be displayed on the customer service page

#### Request Example

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

Raw request body

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

## Anti Cheat

For the [/s0/pixel/{tileX}/{tileY}](#post-s0pixeltilextiley) interface, wplace adds multiple anti-cheat measures to prevent automatic painting and multiple accounts.

### `lp` - LocalStorage Detection

After logging in, Local Storage will write the `lp` field, which is a base64 encoded json. After decoding, you can see:

```json
{
	"userId": 1,
	"time": 1758235291531
}
```

It contains your user ID and login timestamp. When you try to submit a paint but the user ID and Local Storage are inconsistent, you will be prompted not to use multiple accounts to paint.

#### Solution

- For robots or scripts that don't run in browsers, just ignore it
- Use multiple [browser profiles](https://support.google.com/chrome/answer/2364824)
- Delete `lp` from Local Storage when switching accounts

### Turnstile - Captcha

<img src="/images/captcha.png" align="right" width="400">

wplace uses [Turnstile Captcha](https://www.cloudflare.com/application-services/products/turnstile/), and after each painting, the saved captcha will be cleared on the frontend.

Generally speaking, this captcha will not pop up frequently, but if the server is under high load and Under Attack mode is started, it will pop up before each painting.

#### Solution

- Paid automatic captcha passing API through captcha platform
- Capture the `cf-turnstile-response` field in `https://challenges.cloudflare.com` through man-in-the-middle proxy (when the server has not enabled Under Attack mode)
- Open a browser to hang scripts automatically and send back to the client through browser plugins.

### FingerprintJS - Browser Fingerprint

<img src="/images/FingerprintJS.png" align="right" width="400">

wplace uses [FingerprintJS](https://fingerprint.com/) to report `visitorId` (fp field) to detect multiple accounts and robots.

That is, through data such as `User-Agent`, `screen resolution`, `time zone`, etc., to detect whether the browser is headless, anonymous mode, etc.

And there is a `0.001%` chance of selling your information to FingerprintJS providers.

```javascript
function Q8() {
    if (!(window.__fpjs_d_m || Math.random() >= 0.001)) try {
        var _ = new XMLHttpRequest;
        _.open(
            'get',
            'https://m1.openfpcdn.io/fingerprintjs/v'.concat(I0, '/npm-monitoring'),
            !0
        ),
            _.send()
    } catch (s) {
        console.error(s)
    }
}
```

> Real code in Wplace's JS, 0.001% chance of uploading your statistics to FingerprintJS server

#### Solution

- Strictly speaking, wplace has not fully enabled this detection because only one `visitorId` (an MD5 value) is uploaded. Theoretically, any MD5 can pass because this value cannot be verified from the server side. However, to prevent being detected as multiple accounts, it is recommended to use `MD5(userId + salt)`

### Pawtect

Pawtect is a WASM module based on Rust introduced by wplace's latest and hottest technology. Its sample can be viewed in [pawtect_wasm_bg.wasm](files/pawtect_wasm_bg.wasm). It is used to sign the request body before requesting, and then send it to the server together with the request header.

Some users will not enable this check. If you want to know whether an account has enabled this check, you need to first request [/me](#get-me) to obtain the `experiments` information. If the `variant` is disabled, you only need to pass `x-pawtect-variant: disabled` when requesting, otherwise you need to pass both `x-pawtect-variant` and `x-pawtect-token` request headers.

#### Solution

- Directly capture through real browser (man-in-the-middle proxy or browser plugin)
- Implement signature by loading WASM module through reference code below (if your script is developed using nodejs)

#### Reference Code

```javascript
let m;
let memory;
const textEncoder = new TextEncoder();
const textDecoder = new TextDecoder();
let J = 0;

function re(n, malloc, realloc) {
    if (realloc === undefined) {
        const s = textEncoder.encode(n);
        const ptr = malloc(s.length, 1) >>> 0;
        new Uint8Array(memory.buffer, ptr, s.length).set(s);
        J = s.length;
        return ptr;
    }
    let a = n.length;
    let ptr = malloc(a, 1) >>> 0;
    const mem = new Uint8Array(memory.buffer);
    let i = 0;
    for (; i < a; i++) {
        const code = n.charCodeAt(i);
        if (code > 0x7F) break;
        mem[ptr + i] = code;
    }
    if (i !== a) {
        if (i !== 0) n = n.slice(i);
        ptr = realloc(ptr, a, a = i + n.length * 3, 1) >>> 0;
        const view = new Uint8Array(memory.buffer, ptr + i, a - i);
        const { written } = textEncoder.encodeInto(n, view);
        i += written;
        ptr = realloc(ptr, a, i, 1) >>> 0;
    }
    J = i;
    return ptr;
}

function P(ptr, len) {
    return textDecoder.decode(new Uint8Array(memory.buffer, ptr, len));
}

function fn(n) {
    let e,
        t;
    try {
        const a = re(n, m.__wbindgen_malloc, m.__wbindgen_realloc),
            r = J,
            o = m.get_pawtected_endpoint_payload(a, r);
        return e = o[0],
            t = o[1],
            P(o[0], o[1])
    } finally {
        m.__wbindgen_free(e, t, 1)
    }
}

async function loadWASM() {
    const wasmBuffer = await readFile("./pawtect_wasm_bg.wasm");
    const imports = hn();
    const { instance } = await WebAssembly.instantiate(wasmBuffer, imports);
    m = instance.exports;
    memory = m.memory;
}

function hn() {
    const n = {};
    n.wbg = {};
    n.wbg.__wbg_buffer_609cc3eee51ed158 = e => e.buffer;
    n.wbg.__wbg_call_672a4d21634d4a24 = (e, t) => e.call(t);
    n.wbg.__wbg_call_7cccdd69e0791ae2 = (e, t, a) => e.call(t, a);
    n.wbg.__wbg_crypto_574e78ad8b13b65f = e => e.crypto;
    n.wbg.__wbg_getRandomValues_b8f5dbd5f3995a9e = (e, t) => e.getRandomValues(t);
    n.wbg.__wbg_msCrypto_a61aeb35a24c1329 = e => e.msCrypto;
    n.wbg.__wbg_new_a12002a7f91c75be = e => new Uint8Array(e);
    n.wbg.__wbg_newnoargs_105ed471475aaf50 = (e, t) => new Function(P(e, t));
    n.wbg.__wbg_newwithbyteoffsetandlength_d97e637ebe145a9a = (e, t, a) =>
        new Uint8Array(e, t >>> 0, a >>> 0);
    n.wbg.__wbg_newwithlength_a381634e90c276d4 = e => new Uint8Array(e >>> 0);
    n.wbg.__wbg_node_905d3e251edff8a2 = e => e.node;
    n.wbg.__wbg_process_dc0fbacc7c1c06f7 = e => e.process;
    n.wbg.__wbg_randomFillSync_ac0988aba3254290 = (e, t) => e.randomFillSync(t);
    n.wbg.__wbg_require_60cc747a6bc5215a = () => module.require;
    n.wbg.__wbg_set_65595bdd868b3009 = (e, t, a) => e.set(t, a >>> 0);
    n.wbg.__wbg_static_accessor_GLOBAL_88a902d13a557d07 = () =>
        typeof global === "undefined" ? null : global;
    n.wbg.__wbg_static_accessor_GLOBAL_THIS_56578be7e9f832b0 = () =>
        typeof globalThis === "undefined" ? null : globalThis;
    n.wbg.__wbg_static_accessor_SELF_37c5d418e4bf5819 = () =>
        typeof self === "undefined" ? null : self;
    n.wbg.__wbg_static_accessor_WINDOW_5de37043a91a9c40 = () =>
        typeof window === "undefined" ? null : window;
    n.wbg.__wbg_subarray_aa9065fa9dc5df96 = (e, t, a) => e.subarray(t >>> 0, a >>> 0);
    n.wbg.__wbg_versions_c01dfd4722a88165 = e => e.versions;
    n.wbg.__wbindgen_init_externref_table = () => {
        const e = m.__wbindgen_export_2;
        const t = e.grow(4);
        e.set(0, void 0);
        e.set(t + 0, void 0);
        e.set(t + 1, null);
        e.set(t + 2, true);
        e.set(t + 3, false);
    };
    n.wbg.__wbindgen_is_function = e => typeof e === "function";
    n.wbg.__wbindgen_is_object = e => typeof e === "object" && e !== null;
    n.wbg.__wbindgen_is_string = e => typeof e === "string";
    n.wbg.__wbindgen_is_undefined = e => e === void 0;
    n.wbg.__wbindgen_memory = () => m.memory;
    n.wbg.__wbindgen_string_new = (e, t) => P(e, t);
    n.wbg.__wbindgen_throw = (e, t) => {
        throw new Error(P(e, t));
    };
    return n;
}

// Need to add post logic yourself
// Example input: https://backend.wplace.live/s0/pixel/1/1, {}, 1
function postPaw(url, bodyStr, userId) {
    loadWASM();
    if (m.__wbindgen_start) m.__wbindgen_start();
    m.set_user_id(userId);
    const urlPtr = re(url, m.__wbindgen_malloc, m.__wbindgen_realloc);
    m.request_url(urlPtr, J);
    const loadPayload = m.get_load_payload();
    const sign = fn(bodyStr);
};

```



## Appendix

### Common API Errors

```jsonc
{
  "error": "Unauthorized",
  "status": 401
}
```

> `j` token not attached or token invalid


```jsonc
{
  "error": "Internal Server Error. We'll look into it, please try again later.",
  "status": 500
}
```

> Cookie expired

```jsonc
{
  "error": "Bad Request",
  "status": 400
}
```

> Request format error

### All Color Tables
| Color | ID | RGB | Paid    |
|------|------| ------- |---------|
| | `0` | Transparent | `false` |
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
| ![#aaaaaa](https://img.shields.io/badge/-%20-aaaaaa?style=flat-square) | `32` | `170, 170, 170` | `true`  |
| ![#a50e1e](https://img.shields.io/badge/-%20-a50e1e?style=flat-square) | `33` | `165, 14, 30` | `true`  |
| ![#fa8072](https://img.shields.io/badge/-%20-fa8072?style=flat-square) | `34` | `250, 128, 114` | `true`  |
| ![#e45c1a](https://img.shields.io/badge/-%20-e45c1a?style=flat-square) | `35` | `228, 92, 26` | `true`  |
| ![#d6b594](https://img.shields.io/badge/-%20-d6b594?style=flat-square) | `36` | `214, 181, 148` | `true`  |
| ![#9c8431](https://img.shields.io/badge/-%20-9c8431?style=flat-square) | `37` | `156, 132, 49` | `true`  |
| ![#c5ad31](https://img.shields.io/badge/-%20-c5ad31?style=flat-square) | `38` | `197, 173, 49` | `true`  |
| ![#e8d45f](https://img.shields.io/badge/-%20-e8d45f?style=flat-square) | `39` | `232, 212, 95` | `true`  |
| ![#4a6b3a](https://img.shields.io/badge/-%20-4a6b3a?style=flat-square) | `40` | `74, 107, 58` | `true`  |
| ![#5a944a](https://img.shields.io/badge/-%20-5a944a?style=flat-square) | `41` | `90, 148, 74` | `true`  |
| ![#84c573](https://img.shields.io/badge/-%20-84c573?style=flat-square) | `42` | `132, 197, 115` | `true`  |
| ![#0f799f](https://img.shields.io/badge/-%20-0f799f?style=flat-square) | `43` | `15, 121, 159` | `true`  |
| ![#bbfaf2](https://img.shields.io/badge/-%20-bbfaf2?style=flat-square) | `44` | `187, 250, 242` | `true`  |
| ![#7dc7ff](https://img.shields.io/badge/-%20-7dc7ff?style=flat-square) | `45` | `125, 199, 255` | `true`  |
| ![#4d31b8](https://img.shields.io/badge/-%20-4d31b8?style=flat-square) | `46` | `77, 49, 184` | `true`  |
| ![#4a4284](https://img.shields.io/badge/-%20-4a4284?style=flat-square) | `47` | `74, 66, 132` | `true`  |
| ![#7a71c4](https://img.shields.io/badge/-%20-7a71c4?style=flat-square) | `48` | `122, 113, 196` | `true`  |
| ![#b5aef1](https://img.shields.io/badge/-%20-b5aef1?style=flat-square) | `49` | `181, 174, 241` | `true`  |
| ![#dba463](https://img.shields.io/badge/-%20-dba463?style=flat-square) | `50` | `219, 164, 99` | `true`  |
| ![#d18051](https://img.shields.io/badge/-%20-d18051?style=flat-square) | `51` | `209, 128, 81` | `true`  |
| ![#ffc5a5](https://img.shields.io/badge/-%20-ffc5a5?style=flat-square) | `52` | `255, 197, 165` | `true`  |
| ![#9b5249](https://img.shields.io/badge/-%20-9b5249?style=flat-square) | `53` | `155, 82, 73` | `true`  |
| ![#d18078](https://img.shields.io/badge/-%20-d18078?style=flat-square) | `54` | `209, 128, 120` | `true`  |
| ![#fab6a4](https://img.shields.io/badge/-%20-fab6a4?style=flat-square) | `55` | `250, 182, 164` | `true`  |
| ![#7b6352](https://img.shields.io/badge/-%20-7b6352?style=flat-square) | `56` | `123, 99, 82` | `true`  |
| ![#9c846b](https://img.shields.io/badge/-%20-9c846b?style=flat-square) | `57` | `156, 132, 107` | `true`  |
| ![#333941](https://img.shields.io/badge/-%20-333941?style=flat-square) | `58` | `51, 57, 65` | `true`  |
| ![#6d758d](https://img.shields.io/badge/-%20-6d758d?style=flat-square) | `59` | `109, 117, 141` | `true`  |
| ![#b3b9d1](https://img.shields.io/badge/-%20-b3b9d1?style=flat-square) | `60` | `179, 185, 209` | `true`  |
| ![#6d643f](https://img.shields.io/badge/-%20-6d643f?style=flat-square) | `61` | `109, 100, 63` | `true`  |
| ![#948c6b](https://img.shields.io/badge/-%20-948c6b?style=flat-square) | `62` | `148, 140, 107` | `true`  |
| ![#cdc59e](https://img.shields.io/badge/-%20-cdc59e?style=flat-square) | `63` | `205, 197, 158` | `true`  |

### BitMap Java Implementation

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

### All Flags


| Flag | Region Code | ID  |
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