# Wplace Protocol

Analysis of [Wplace](https://wplace.live)'s technology stack, protocols, and endpoints.

Disclaimer: Some unreferenced endpoints are omitted as they may be removed at any time. Please contact me promptly if you notice any errors.

Table of contents:

- [Concepts and Systems](#concepts-and-systems)
  - [Map](#map)
  - [Tiles](#tiles)
    - [Calculating Corresponding Latitude/Longitude](#calculating-corresponding-latitude-and-longitude)
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
- [Anti Cheat](#anti-cheating)
- [Appendix](#appendix)
  - [General API Errors](#general-api-errors)
  - [Full Color Palette](#full-color-palette)
  - [BitMap Java Implementation](#bitmap-java-implementation)
  - [All Flags](#all-flags)

## Concepts and Systems

_Most names are subjective and may not align with source code or other Wplace projects_

### Map

<img src="/images/projection.JPG" align="right" width="200" alt="Mercator Projection"/>

> Keywords: `Map / Canvas / World`

The map refers to Wplace's overall canvas. Rendered using the [Mercator Projection / Web Mercator](https://en.wikipedia.org/wiki/Mercator_projection), the map employs the Liberty Style from [OpenFreeMap](https://openfreemap.org/). The map comprises `2048x2048` tiles, totaling `4,194,304` tiles. These tiles are overlaid on the map using Canvas in the frontend.

Most locations on the map that lack real-world territorial ownership or are disputed have been assigned to the nearest landmass's country or region. For example, the North Pacific is assigned to Honolulu, USA, and the South Pacific is assigned to Adams Island, Australia.

The total number of pixels in the map is `4,194,304,000,000` (approximately 4.1 trillion).

### Tiles

> Keywords: `Tile / Chunk`

Tiles are the smallest units rendered on the wplace canvas. Each tile is a `1000×1000` PNG image on the server, containing `1,000,000` pixels.

The data type for tiles is `Vec2i`, representing `x` and `y` coordinates.

Relative coordinates mentioned in the API start from position 0 within the tile.

#### Calculating Corresponding Latitude and Longitude

The entire [map](#map) has `2048` tiles both horizontally and vertically. This allows calculating the `Zoom` value:

```java
int n = 2048; // Number of tiles
int z = (int) (Math.log(n) / Math.log(2)); // Calculate Zoom using the change-of-base formula
```

Using this formula, the zoom level is calculated to be **approximately** `11`. Subsequently, the following algorithm can be used to compute the latitude and longitude:

```java
double n = Math.pow(2.0, 11); // zoom is 11
double lon = (x + 0.5) / n * 360.0 - 180.0;
double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * (y + 0.5) / n)));
double lat = Math.toDegrees(latRad);
```

Here, `lon` and `lat` represent the latitude and longitude values.

> Formula reference: [Slippy map tilenames](https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames)

#### Related Endpoints

- [/s0/pixel/{tileX}/{tileY}?x={x}&y={y}](#get-s0pixeltilextileyxxyy)
- [/s0/pixel/{tileX}/{tileY}](#post-s0pixeltilextiley)

### Colors

> Keywords: `Color / Palette`

Wplace offers 64 colors. The first 32 are free, while each of the latter 32 requires `2,000` Droplets to unlock.

To determine if a color is unlocked, the frontend performs a bitmask check on `extraColorsBitmap`—a field within the JSON returned by the user profile API.

The verification logic is as follows:

```java
int extraColorsBitmap = 0;
int colorId = 63; // Color ID to check
boolean unlocked;

if (colorId < 32) { // Skip first 32 since they're free
    unlocked = true;
} else {
    int mask = 1 << (colorId - 32);
    unlocked = (extraColorsBitmap & mask) != 0;
}
```

> Disclaimer: This code is Java code analyzed by the author from obfuscated JavaScript code in Wplace, not the original source code.

For color codes, please refer to [Appendix](#full-color-palette)

#### Related Endpoints

- [/me](#get-me)
- [/purchase](#post-purchase)

### Flags

> Keyword: `Flag`

Wplace contains 251 flags. Purchasing a flag allows you to save 10% of pixels when painting in the corresponding region. Each flag costs `20,000` Droplets.

Flag unlock status is managed via a custom BitMap. Below is the JS code for this BitMap:

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

After the frontend obtains the `flagsBitmap` field through the user profile endpoint, it decodes it from Base64 to Bytes and then passes it to BitMap to read whether a flag ID has been unlocked.

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

Each level up will gain `500` droplets and increase `2` maximum charges

### Store

> Keywords: `Store / Purchase`

Items can be purchased with the in-game virtual currency `Droplet` in the store. The following is a list of items:

| Item ID | Item Name          | Price (Droplet) | Variants            |
|---------|--------------------|-----------------|---------------------|
| `70`    | +5 Max. Charges    | `500`           | None                |
| `80`    | +30 Paint Charges  | `500`           | None                |
| `100`   | Unlock Paid Colors | `2000`          | [Color ID](#colors) |
| `110`   | Unlock Flag        | `20000`         | [Flag ID](#flags)   |

#### Related Endpoints

- [/purchase](#post-purchase)

Other item IDs are reserved for recharge items (cash payment)

## Protocol

Unless otherwise specified, the URL host is `backend.wplace.live`

For common API errors, refer to [Appendix](#general-api-errors)

### Authentication

Authentication is achieved through the `j` field in cookies. After login, the backend stores a [JSON Web Token](https://en.wikipedia.org/wiki/JSON_Web_Token) in the cookie. Subsequent requests to `wplace.live` and `backend.wplace.live` will carry this cookie.

The token is encoded text, not a random string. You can decode it using [jwt.io](https://jwt.io) or any JWT tool to retrieve information.

```json
{
  "userId": 1,
  "sessionId": "",
  "iss": "wplace",
  "exp": 1758373929,
  "iat": 1755781929
}
```

The `exp` field represents the expiration timestamp, allowing the expiration time to be determined solely from the token.

### Cookie

Typically, only the `j` cookie is required when requesting an API endpoint. However, if the server experiences high load, developers may enable [Under Attack Mode](https://developers.cloudflare.com/fundamentals/reference/under-attack-mode/). When Under Attack Mode is active, an additional valid `cf_clearance` cookie must be included in the request header. Failure to do so will trigger a Cloudflare challenge.

Ensure that most request header fields (e.g., `User-Agent`, `Accept-Language`) in automated requests match those of the browser used to obtain the `cf_clearance` cookie. Otherwise, verification will fail and the challenge will still appear.

### GET `/me`

Retrieve user information

#### Request

- Requires `j` for authentication

#### Response upon successful request

```jsonc
{
    // int: Alliance ID
    "allianceId": 1, 
    // enum: Alliance permission
    // admin/member
    "allianceRole": "admin",
    // boolean: Whether banned
    "banned": false,
    // object: Pixel information
    "charges": {
        // int: Pixel recharge interval in milliseconds (30000 ms = 30 seconds)
        "cooldownMs": 30000,
        // float: Remaining pixels
        "count": 35.821833333333586,
        // float: Maximum pixel count
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
    // object: Canary test flag, internal meaning unclear
    // For example, the variant value “koala” has no defined internal meaning and serves only as an identifier.
    // However, it is sent in request headers. If the variant for 2025-09_pawtect is disabled, the pawtect-token is not sent.
    // This indicates some users have not been enabled for the new security mechanism.
    "experiments": {
        "2025-09_pawtect": {
            "variant": "koala"
        }
    },
    // int: extraColorsBitmap, see the #Colors section for its function.
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
    // string: List of unlocked flags. See the #Flags section for details on their function.
    "flagsBitmap": "AA==",
    // enum: Typically not displayed; shown only if you have permission
    // moderator/global_moderator/admin
    "role": "",
    // int: User ID
    "id": 1,
    // boolean: Indicates if the user has made purchases; if true, displays order list in menu
    "isCustomer": false,
    // float: Level
    "level": 94.08496005353335,
    // int: Maximum favorite locations, default 15. No known method to increase currently
    "maxFavoriteLocations": 15,
    // string: Username
    "name": "username",
    // boolean: Requires phone number verification. If enabled, a verification window will pop up during access.
    "needsPhoneVerification": false,
    // string: Avatar URL or base64. Determine based on prefix (e.g., data:image/png;base64,)
    "picture": "",
    // int: Number of pixels already painted
    "pixelsPainted": 114514,
    // boolean: Whether to display your last painted location on the alliance page
    "showLastPixel": true,
    // string: Your unban timestamp. If set to 1970, it indicates you've never been banned or have been permanently banned.
    "timeoutUntil": "1970-01-01T00:00:00Z"
}
```

### POST `/me/update`

Update the current user's personal information

#### Request

* Requires `j` for authentication

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

#### Response upon successful request

```jsonc
{
    "success": true
}
```

#### Returned when a request error occurs

```jsonc
{
    "error": "The name has more than 16 characters",
    "status": 400
}
```

> Invalid request body

### GET `/me/profile-pictures`

Retrieve profile picture list

A user may have multiple profile pictures  (adding one requires `20,000` Droplets) and can switch to any picture in the list at any time.

#### Request

* Requires `j` for authentication

#### Response upon successful request

```jsonc
// array: All avatars
[
    {
        // int: Avatar ID
        "id": 0,
        // string: Avatar URL or Base64, can be identified by whether it starts with data:image/png;base64,
        "url": ""
    }
]
```

> If you don't have any avatars, an empty array will be returned

### POST `/me/profile-picture/change`

Change Profile Picture

#### Request

* Requires `j` for authentication

#### Request Example

Change existing custom profile picture

```jsonc
{
    // int: Profile picture ID. Ensure this picture exists.
	"pictureId": 1
}
```

Reset Profile Picture

```jsonc
{}
```

> Sending an empty json object resets the profile picture.

#### Response upon successful request

```jsonc
{
	"success": true
}
```

### POST `/me/profile-picture`

Upload Profile Picture

#### Request

* Requires `j` for authentication
* Request body is Multipart File: `image`

#### Response upon successful request

```jsonc
{
	"success": true
}
```

#### Returned when a request error occurs

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

### GET `/alliance`

Retrieve Alliance information

#### Request

* Requires `j` for authentication

#### Response upon successful request

```jsonc
{
	// string: Alliance description
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
	// string: Total pixels painted
	"pixelsPainted": 1419281,
	// enum: Your role
	// admin/member
	"role": "admin"
}
```

#### Returned when a request error occurs

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

* Requires `j` for authentication

#### Request Example

```jsonc
{
    // string: Alliance name, must be unique.
	"name": "Team RealB"
}
```

#### Response upon successful request

```jsonc
{
    // int: ID of the created Alliance
	"id": 1
}
```

#### Returned when a request error occurs

```jsonc
{
	"error": "name_taken",
	"status": 400
}
```

> Alliance name is already in use

```jsonc
{
    "error": "Forbidden",
    "status": 403
}
```

> Attempted to create an Alliance when one already exists. This should not occur under normal circumstances.

### POST `/alliance/update-description`

Update Alliance Description

#### Request

* Requires `j` for authentication

#### Response upon successful request

```jsonc
{
	"success": true
}
```

#### Returned when a request error occurs

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance exists or permission is not admin

### GET `/alliance/invites`

Retrieve Alliance invitation links

#### Request

* Requires `j` for authentication

#### Response upon successful request

```jsonc
// array: Alliance invitation links, typically a single UUID-formatted entry
[
    "fe7c9c32-e95a-4f5f-a866-554cde2149c3"
]
```

#### Returned when a request error occurs

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance exists or permission is not admin

### GET `/alliance/join/{invite}`

Join an Alliance using an invitation UUID. To obtain an invitation UUID, refer to [/alliance/invites](#get-allianceinvites).

#### Request

* Requires `j` for authentication
* The {invite} parameter in the URL represents the invitation UUID
  - Example URL (set to the Chinese flag): `/alliance/join/fe7c9c32-e95a-4f5f-a866-554cde2149c3`

#### Response upon successful request

```jsonc
{
    "success": "true"
}
```

> If the target Alliance matches one you already belong to, success will still be returned

#### Returned when a request error occurs

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

> Blocked by this Alliance

### POST `/alliance/update-headquarters`

Update Alliance headquarters

#### Request

* Requires `j` for authentication

#### Request Example

```jsonc
{
	"latitude": 22.537655528880563,
	"longitude": 114.0274942853182
}
```

#### Response upon successful request

```jsonc
{
	"success": true
}
```

#### Returned when a request error occurs


```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance exists or permission is not admin

### GET `/alliance/members/{page}`

Retrieve the Alliance member list. Features pagination; may require multiple pages if members exceed 50.

#### Request

* Requires `j` for authentication
* The {page} parameter in the URL represents the page number, starting from 0
  - Example URL (for first page): `/alliance/members/0`

#### Response upon successful request

```jsonc
{
    // array: Maximum 50 members per page
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
	// boolean: whether there is a next page
	"hasNext": true
}
```

#### Returned when a request error occurs

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin


### GET `/alliance/members/banned/{page}`

Retrieves a list of members banned by the Alliance. Includes pagination; may require multiple requests if members exceed 50.

Banned members cannot rejoin the Alliance.

#### Request

* Requires `j` for authentication
* `{page}` parameter in URL represents page number, starting from 0
  - Example URL (retrieve first page): `/alliance/members/banned/0`

#### Response upon successful request

```jsonc
{
	"data": [{
		"id": 1,
		"name": "SuckMyDick"
	}],
	"hasNext": false
}
```

> Similar to the regular member endpoint, but lacks `role` since banned users are no longer in the alliance.

#### Returned when a request error occurs

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### POST `/alliance/give-admin`

Promotes a member to Admin status. Cannot be downgraded.

#### Request

* Requires `j` for authentication

#### Request Example

```jsonc
{
    // int: User ID to be promoted
	"promotedUserId": 1
}
```

#### Response upon successful request

This endpoint does not return data. A `200` status code indicates success.

#### Returned when a request error occurs

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### POST `/alliance/ban`

Kick and ban a member

Once banned, the member cannot rejoin unless the ban is lifted

#### Request

* Requires `j` for authentication

#### Request Example
```jsonc
{
    // int: User ID to kick out or ban
	"bannedUserId": 1
}
```

#### Response upon successful request

```jsonc
{
	"success": true
}
```

#### Returned when a request error occurs

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### POST `/alliance/unban`

Unbans a member. After unbanning, the member will not automatically rejoin the Alliance but will be able to reapply.

#### Request

* Requires `j` for authentication

#### Request Example

```jsonc
{
    // int: User ID to unban
	"unbannedUserId": 1
}
```

#### Response upon successful request

```jsonc
{
	"success": true
}
```

#### Returned when a request error occurs

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> No Alliance or permission is not admin

### GET `/alliance/leaderboard/{mode}`

Retrieve the top 50 player rankings within the Alliance.

#### Request

* Requires `j` for authentication
* `mode` in the URL represents the time range and is an enumeration with any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* Example URL (Today's Leaderboard): `/alliance/leaderboard/today`

#### Response upon successful request

```jsonc
[
  {
    // int: User ID
    "userId": 10815100,
    // string: Username
    "name": "Make Love",
    // int: Flag ID (refer to appendix for flag list)
    "equippedFlag": 0,
    // int: Number of pixels painted
    "pixelsPainted": 32901,
    // Latitude and longitude of last drawn pixel; absent if user disabled showLastPixel
    "lastLatitude": 22.527739206672393,
    "lastLongitude": 114.02762695312497
  },
  {
    "userId": 10850297,
    "name": "Yoon Yong Hyun",
    "equippedFlag": 0,
    "pixelsPainted": 31631
  }
]
```

### POST `/favorite-location`

Favorite a location

#### Request

* Requires `j` for authentication

#### Request Example

```jsonc
{
	"latitude": 22.5199456234827,
	"longitude": 114.02428677802732
}
```

#### Response upon successful request

```jsonc
{
    // int: Favorite ID
	"id": 1,
	"success": true
}
```

#### Returned when a request error occurs

```jsonc
{
  "error": "Forbidden",
  "status": 403
}
```

> The number of favourites exceeds the `maxFavoriteLocations` limit.


### POST `/favorite-location/delete`

Remove a favorite location

#### Request

* Requires `j` for authentication

#### Request Example

```jsonc
{
    // int: Favorite ID
	"id": 1
}
```

#### Response upon successful request

```jsonc
{
    "success": true
}
```

> Passing any ID, even for unfavorited or non-existent locations, will return success.

### POST `/purchase`

Purchase an item. For related definitions, refer to the [Store](#Store) section.

#### Request

* Requires `j` for authentication

#### Request Example

```jsonc
{
    // object: Fixed field product
	"product": {
	    // int: Item id
		"id": 100,
		// int: Purchase quantity. Multiple units can be purchased for Paint Charges/Max. Charge.
		"amount": 1,
		// int: Variant value. Some items have variants; omit if no variant exists.
		"variant": 49
	}
}
```

#### Response upon successful request

```jsonc
{
	"success": true
}
```

#### Returned when a request error occurs

All errors returned by this endpoint follow the same format

```json
{"error":"Forbidden","status":403}{"success":true}
```

> Possibly due to Brazilians overdoing it on drugs or getting hit in the back of the head by a soccer ball, causing brain malfunction and leading to this typo. But this response body genuinely looks like this, so extra handling might be needed.
> 
> ![proof](/images/bad-resp.png)

### POST `/flag/equip/{id}`

Set display flag

#### Request

* Requires `j` for authentication
* The {id} parameter in the URL represents the flag ID. Refer to [Flags](#flags) and [Appendix](#all-flags) for all flag IDs and unlock checks.
  - Example URL (to set the Chinese national flag): `/flag/equip/45`

#### Response upon successful request

```jsonc
{
	"success": true
}
```

#### Returned when a request error occurs

```jsonc
{
	"error": "Forbidden",
	"status": 403
}
```

> Flag not unlocked

### GET `/leaderboard/region/{mode}/{country}`

Retrieve a region-based leaderboard for a specific country/region (top 50 entries only)

#### Request

* `mode` in the URL denotes the time range and is an enumeration with any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* `country` in the URL is the region ID. Refer to the [Appendix](#all-flags) for the corresponding table.
* Example URL (China's city leaderboard for today): `/leaderboard/region/today/45`

#### Response upon successful request:

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
    // int: Number of pixels painted
    "pixelsPainted": 389274,
    // Latitude and longitude of last painted point
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

Retrieve all country leaderboards, limited to the top 50

#### Request

* `mode` in the URL represents the time range and is an enumeration with any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* Example URL (today's country leaderboard): `/leaderboard/country/today`

#### Response upon successful request

````jsonc
[
  {
    // int: Country ID (see appendix for full list)
    // 235 corresponds to the United States
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

Retrieve the global player leaderboard, limited to the top 50 players

#### Request

* `mode` in the URL represents the time range and is an enumeration with any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* Example URL (today's player leaderboard): `/leaderboard/player/today`

#### Response upon successful request

```jsonc
[
  {
    // int: User ID
    "id": 8883244,
    // string: Username
    "name": "Tightmatt Cousin",
    // int: Alliance ID, 0 if none
    "allianceId": 0,
    // string: Alliance name, empty string if none
    "allianceName": "",
    // int: Equipped flags (refer to appendix for flag list), 0 if none
    "equippedFlag": 155,
    // int: Number of pixels painted
    "pixelsPainted": 64451,
    // string:  Avatar URL or Base64 (determined by whether it starts with `data:image/png;base64,`). This field is absent if no avatar is present
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

Retrieve the global Alliance leaderboard, limited to the top 50 entries.

#### Request

* `mode` in the URL represents the time range and is an enumeration with any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* Example URL (today's Alliance leaderboard): `/leaderboard/alliance/today`

#### Response upon successful request

```jsonc
[
  {
    // int: Alliance ID
    "id": 165,
    // string: Alliance name
    "name": "bapo",
    // int: Number of pixels painted
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

Retrieve the top 50 players on the leaderboard for a specific city.

#### Request

* `mode` in the URL represents the time range and is an enumeration with any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* `city` in the URL is the city ID. Currently, there is no definitive list available due to the sheer number of cities.
* Example URL (Shenzhen overall player leaderboard): `/leaderboard/region/players/114594/all-time`

#### Response upon successful request

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

Retrieve the Alliance leaderboard for a specific city, limited to the top 50 entries.

#### Request

* `mode` in the URL represents the time range and is an enumeration with any of the following values:
  - `today`
  - `week`
  - `month`
  - `all-time`
* `city` in the URL is the city ID. Currently, there is no definitive list available due to the sheer number of cities.
* Example URL (Shenzhen Alliance Overall Leaderboard): `/leaderboard/region/alliances/114594/all-time`

#### Response upon successful request

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

Retrieve a randomly selected painted pixel

#### Response upon successful request

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

For the relationship between Tile and pixel positions, refer to [Tiles](#tiles)

### GET `/s0/pixel/{tileX}/{tileY}?x={x}&y={y}`

Retrieve information about a specific pixel

#### Request

* tileX and tileY in the URL must be tile coordinates. For details, refer to [Tiles](#Tiles)
* The x and y parameters represent relative pixel coordinates within a 1024-pixel range
* Example URL (location in Shenzhen): `/s0/pixel/1672/892?x=668&y=265`

#### Response upon successful request

Painted

```jsonc
{
    // object: Painter information
	"paintedBy": {
	    // int: User ID
		"id": 1,
		// string: Username
		"name": "崔龙海",
		// int: Alliance ID (0 if none)
		"allianceId": 1,
		// string: Alliance name (empty string if none)
		"allianceName": "Team ReaIB",
		// int: Flag ID (refer to appendix for mapping)
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

Retrieve a texture for a specific [tile](#tiles)

#### Request

* `tileX` and `tileY` in the URL must be tile coordinates. Refer to [Tiles](#Tiles) for details.
* Example URL: `/files/s0/tiles/1672/892.png`

#### Response upon successful request

![ex](/images/892.png)

### POST `/s0/pixel/{tileX}/{tileY}`

Paint pixels

You must include the anti-cheat request headers `x-pawtect-variant` and `x-pawtect-token`. See [Anti-Cheat](#anti-cheating) for details.

#### Request

* Requires `j` for authentication
* `tileX` and `tileY` in the URL must be tile coordinates. See [Tiles](#tiles) for details.
* Example URL: `/s0/pixel/1672/892`

#### Request Example

```jsonc
{
    // array: Color IDs to paint, each value corresponds to one pixel
	"colors": [49, 49, 49, 49, 49, 49],
	// array: Coordinates to paint, formatted as x, y, x, y, appearing in (x, y) pairs
	// Coordinate order corresponds one-to-one with colors, i.e., the Nth color applies to the Nth coordinate
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

> `colors` corresponds to the color codes used in rendering, paired with `coords`. Refer to [Colors](#colors) and [Appendix](#full-color-palette)
>
> When painting colors spanning multiple [tiles](#tiles), requests may be split across multiple calls
>
> For the verification token, see [Turnstile](#turnstile---captcha)
> `fp`: See [Browser Fingerprint](#fingerprintjs---browser-fingerprinting)
> `x-pawtect-token` and `x-pawtect-variant`: See [pawtect](#pawtect)

#### Response upon successful request

```jsonc
{
	"painted": 6
}
```

#### Returned when a request error occurs

```jsonc
{
	"error": "refresh",
	"status": 403
}
```

> Invalid verification code token or pawtect

### POST `/report-user`

<img src="/images/staffscreen.png" align="right" width="500" alt="staffscreen">

Report a user. When reporting, the client renders a screenshot. Mods can view both the client screenshot and the live screenshot during review.

Mods can see all users under the IP address of the reported user.

#### Request

* Requires `j` to complete authentication
* Request body is multipart
  - `reportedUserId`: ID of the reported user
  - `latitude`: Latitude
  - `longitude`: Longitude
  - `zoom`: Zoom level
  - `reason`: Reason for reporting
  - `notes`: Report text, user-provided input
  - `image`: A screenshot rendered by the client will display on the mods' page

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
  -F "image=@image;type=image/jpeg"
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

## Anti-Cheating

Multiple anti-cheating measures have been added to the wplace endpoint for the [/s0/pixel/{tileX}/{tileY}](#post-s0pixeltilextiley) endpoint to prevent automated drawing and multiple accounts.

### `lp` - LocalStorage Detection

After login, LocalStorage writes an `lp` field containing a base64-encoded JSON. Decoding reveals:

```json
{
	"userId": 1,
	"time": 1758235291531
}
```

This contains your user ID and login timestamp. Attempting to submit a painting request with a user ID that doesn't match Local Storage will trigger a warning against using multiple accounts.

#### Solutions

- Ignore this if using bots or scripts not running in browsers
- Use multiple [browser profiles](https://support.google.com/chrome/answer/2364824)
- When switching accounts, delete `lp` from Local Storage

### Turnstile - Captcha

<img src="/images/captcha.png" align="right" width="400" alt="captcha">

wplace uses [Turnstile Captcha](https://www.cloudflare.com/application-services/products/turnstile/), and after each painting, the saved captcha will be cleared on the frontend.

Typically, this Captcha doesn't appear frequently. However, if the server is under high load and activates Under Attack mode, it will appear before each painting.

Site Key: `0x4AAAAAABpqJe8FO0N84q0F`

#### Solutions

- Use a paid captcha-solving platform's API for automatic verification
- Scrape the `cf-turnstile-response` field from `https://challenges.cloudflare.com` via a man-in-the-middle proxy (when the server isn't in Under Attack mode)
- Manually open a browser with a script to automatically refresh, then send the response back to the client via a browser plugin.

### FingerprintJS -  Browser Fingerprinting

<img src="/images/FingerprintJS.png" align="right" width="400" alt="FingerprintJS">

wplace uses [FingerprintJS](https://fingerprint.com/) to report the `visitorId` (fp field) for detecting multiple accounts and bots.

This involves checking browser data like `User-Agent`, `screen resolution`, and `time zone` to identify headless browsers or incognito mode.

Additionally, there's a `0.001%` chance your information may be sold to FingerprintJS's provider.

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

> Actual code in Wplace's JS with a 0.001% chance of uploading your statistics to FingerprintJS servers

#### Solution

- Strictly speaking, Wplace hasn't fully enabled this detection yet since it only uploads a `visitorId` (an MD5 value). Theoretically, any MD5 could pass because this value can't be verified on the server side. However, to prevent detection of multiple accounts, it's recommended to use `MD5(userId + salt)`.

### Pawtect

Pawtect is the latest and hottest Rust-based WASM module introduced to Wplace. Its sample can be viewed at [pawtect_wasm_bg.wasm](files/pawtect_wasm_bg.wasm). It signs the request body before sending it to the server along with the request header.

Some users may disable this check. To determine if an account has it enabled, first request [/me](#get-me) to obtain the `experiments` information. If the `variant` is `disabled`, only `x-pawtect-variant: disabled` is required in the request header. Otherwise, both `x-pawtect-variant` and `x-pawtect-token` headers are needed.

#### Solutions

- Directly capture data via a real browser (using a man-in-the-middle proxy or browser plugin)
- If you're developing in Java, use the pure Java Pawtect implementation in this repository: [Pawtect.java](pawtect-java/Pawtect.java) (requires Bouncy Castle)
- Load the WASM module using the reference code below to implement signing (if your script is developed in Node.js)

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

### General Api Errors

```jsonc
{
  "error": "Unauthorized",
  "status": 401
}
```

> No `j` token provided or invalid token


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

### Full Color Palette
| Color                                                                  | ID   | RGB             | Paid    |
|------------------------------------------------------------------------|------|-----------------|---------|
|                                                                        | `0`  | Transparent     | `false` |
| ![#000000](https://img.shields.io/badge/-%20-000000?style=flat-square) | `1`  | `0, 0, 0`       | `false` |
| ![#3c3c3c](https://img.shields.io/badge/-%20-3c3c3c?style=flat-square) | `2`  | `60, 60, 60`    | `false` |
| ![#787878](https://img.shields.io/badge/-%20-787878?style=flat-square) | `3`  | `120, 120, 120` | `false` |
| ![#d2d2d2](https://img.shields.io/badge/-%20-d2d2d2?style=flat-square) | `4`  | `210, 210, 210` | `false` |
| ![#ffffff](https://img.shields.io/badge/-%20-ffffff?style=flat-square) | `5`  | `255, 255, 255` | `false` |
| ![#600018](https://img.shields.io/badge/-%20-600018?style=flat-square) | `6`  | `96, 0, 24`     | `false` |
| ![#ed1c24](https://img.shields.io/badge/-%20-ed1c24?style=flat-square) | `7`  | `237, 28, 36`   | `false` |
| ![#ff7f27](https://img.shields.io/badge/-%20-ff7f27?style=flat-square) | `8`  | `255, 127, 39`  | `false` |
| ![#f6aa09](https://img.shields.io/badge/-%20-f6aa09?style=flat-square) | `9`  | `246, 170, 9`   | `false` |
| ![#f9dd3b](https://img.shields.io/badge/-%20-f9dd3b?style=flat-square) | `10` | `249, 221, 59`  | `false` |
| ![#fffabc](https://img.shields.io/badge/-%20-fffabc?style=flat-square) | `11` | `255, 250, 188` | `false` |
| ![#0eb968](https://img.shields.io/badge/-%20-0eb968?style=flat-square) | `12` | `14, 185, 104`  | `false` |
| ![#13e67b](https://img.shields.io/badge/-%20-13e67b?style=flat-square) | `13` | `19, 230, 123`  | `false` |
| ![#87ff5e](https://img.shields.io/badge/-%20-87ff5e?style=flat-square) | `14` | `135, 255, 94`  | `false` |
| ![#0c816e](https://img.shields.io/badge/-%20-0c816e?style=flat-square) | `15` | `12, 129, 110`  | `false` |
| ![#10aea6](https://img.shields.io/badge/-%20-10aea6?style=flat-square) | `16` | `16, 174, 166`  | `false` |
| ![#13e1be](https://img.shields.io/badge/-%20-13e1be?style=flat-square) | `17` | `19, 225, 190`  | `false` |
| ![#28509e](https://img.shields.io/badge/-%20-28509e?style=flat-square) | `18` | `40, 80, 158`   | `false` |
| ![#4093e4](https://img.shields.io/badge/-%20-4093e4?style=flat-square) | `19` | `64, 147, 228`  | `false` |
| ![#60f7f2](https://img.shields.io/badge/-%20-60f7f2?style=flat-square) | `20` | `96, 247, 242`  | `false` |
| ![#6b50f6](https://img.shields.io/badge/-%20-6b50f6?style=flat-square) | `21` | `107, 80, 246`  | `false` |
| ![#99b1fb](https://img.shields.io/badge/-%20-99b1fb?style=flat-square) | `22` | `153, 177, 251` | `false` |
| ![#780c99](https://img.shields.io/badge/-%20-780c99?style=flat-square) | `23` | `120, 12, 153`  | `false` |
| ![#aa38b9](https://img.shields.io/badge/-%20-aa38b9?style=flat-square) | `24` | `170, 56, 185`  | `false` |
| ![#e09ff9](https://img.shields.io/badge/-%20-e09ff9?style=flat-square) | `25` | `224, 159, 249` | `false` |
| ![#cb007a](https://img.shields.io/badge/-%20-cb007a?style=flat-square) | `26` | `203, 0, 122`   | `false` |
| ![#ec1f80](https://img.shields.io/badge/-%20-ec1f80?style=flat-square) | `27` | `236, 31, 128`  | `false` |
| ![#f38da9](https://img.shields.io/badge/-%20-f38da9?style=flat-square) | `28` | `243, 141, 169` | `false` |
| ![#684634](https://img.shields.io/badge/-%20-684634?style=flat-square) | `29` | `104, 70, 52`   | `false` |
| ![#95682a](https://img.shields.io/badge/-%20-95682a?style=flat-square) | `30` | `149, 104, 42`  | `false` |
| ![#f8b277](https://img.shields.io/badge/-%20-f8b277?style=flat-square) | `31` | `248, 178, 119` | `false` |
| ![#aaaaaa](https://img.shields.io/badge/-%20-aaaaaa?style=flat-square) | `32` | `170, 170, 170` | `true`  |
| ![#a50e1e](https://img.shields.io/badge/-%20-a50e1e?style=flat-square) | `33` | `165, 14, 30`   | `true`  |
| ![#fa8072](https://img.shields.io/badge/-%20-fa8072?style=flat-square) | `34` | `250, 128, 114` | `true`  |
| ![#e45c1a](https://img.shields.io/badge/-%20-e45c1a?style=flat-square) | `35` | `228, 92, 26`   | `true`  |
| ![#d6b594](https://img.shields.io/badge/-%20-d6b594?style=flat-square) | `36` | `214, 181, 148` | `true`  |
| ![#9c8431](https://img.shields.io/badge/-%20-9c8431?style=flat-square) | `37` | `156, 132, 49`  | `true`  |
| ![#c5ad31](https://img.shields.io/badge/-%20-c5ad31?style=flat-square) | `38` | `197, 173, 49`  | `true`  |
| ![#e8d45f](https://img.shields.io/badge/-%20-e8d45f?style=flat-square) | `39` | `232, 212, 95`  | `true`  |
| ![#4a6b3a](https://img.shields.io/badge/-%20-4a6b3a?style=flat-square) | `40` | `74, 107, 58`   | `true`  |
| ![#5a944a](https://img.shields.io/badge/-%20-5a944a?style=flat-square) | `41` | `90, 148, 74`   | `true`  |
| ![#84c573](https://img.shields.io/badge/-%20-84c573?style=flat-square) | `42` | `132, 197, 115` | `true`  |
| ![#0f799f](https://img.shields.io/badge/-%20-0f799f?style=flat-square) | `43` | `15, 121, 159`  | `true`  |
| ![#bbfaf2](https://img.shields.io/badge/-%20-bbfaf2?style=flat-square) | `44` | `187, 250, 242` | `true`  |
| ![#7dc7ff](https://img.shields.io/badge/-%20-7dc7ff?style=flat-square) | `45` | `125, 199, 255` | `true`  |
| ![#4d31b8](https://img.shields.io/badge/-%20-4d31b8?style=flat-square) | `46` | `77, 49, 184`   | `true`  |
| ![#4a4284](https://img.shields.io/badge/-%20-4a4284?style=flat-square) | `47` | `74, 66, 132`   | `true`  |
| ![#7a71c4](https://img.shields.io/badge/-%20-7a71c4?style=flat-square) | `48` | `122, 113, 196` | `true`  |
| ![#b5aef1](https://img.shields.io/badge/-%20-b5aef1?style=flat-square) | `49` | `181, 174, 241` | `true`  |
| ![#dba463](https://img.shields.io/badge/-%20-dba463?style=flat-square) | `50` | `219, 164, 99`  | `true`  |
| ![#d18051](https://img.shields.io/badge/-%20-d18051?style=flat-square) | `51` | `209, 128, 81`  | `true`  |
| ![#ffc5a5](https://img.shields.io/badge/-%20-ffc5a5?style=flat-square) | `52` | `255, 197, 165` | `true`  |
| ![#9b5249](https://img.shields.io/badge/-%20-9b5249?style=flat-square) | `53` | `155, 82, 73`   | `true`  |
| ![#d18078](https://img.shields.io/badge/-%20-d18078?style=flat-square) | `54` | `209, 128, 120` | `true`  |
| ![#fab6a4](https://img.shields.io/badge/-%20-fab6a4?style=flat-square) | `55` | `250, 182, 164` | `true`  |
| ![#7b6352](https://img.shields.io/badge/-%20-7b6352?style=flat-square) | `56` | `123, 99, 82`   | `true`  |
| ![#9c846b](https://img.shields.io/badge/-%20-9c846b?style=flat-square) | `57` | `156, 132, 107` | `true`  |
| ![#333941](https://img.shields.io/badge/-%20-333941?style=flat-square) | `58` | `51, 57, 65`    | `true`  |
| ![#6d758d](https://img.shields.io/badge/-%20-6d758d?style=flat-square) | `59` | `109, 117, 141` | `true`  |
| ![#b3b9d1](https://img.shields.io/badge/-%20-b3b9d1?style=flat-square) | `60` | `179, 185, 209` | `true`  |
| ![#6d643f](https://img.shields.io/badge/-%20-6d643f?style=flat-square) | `61` | `109, 100, 63`  | `true`  |
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
            bytes[realIndex] |= (byte) (1 << bitIndex);
        } else {
            bytes[realIndex] &= (byte) ~(1 << bitIndex);
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


| Flag | Region Code | ID    |
|------|-------------|-------|
| 🇦🇫 | `AF`        | `1`   |
| 🇦🇱 | `AL`        | `2`   |
| 🇩🇿 | `DZ`        | `3`   |
| 🇦🇸 | `AS`        | `4`   |
| 🇦🇩 | `AD`        | `5`   |
| 🇦🇴 | `AO`        | `6`   |
| 🇦🇮 | `AI`        | `7`   |
| 🇦🇶 | `AQ`        | `8`   |
| 🇦🇬 | `AG`        | `9`   |
| 🇦🇷 | `AR`        | `10`  |
| 🇦🇲 | `AM`        | `11`  |
| 🇦🇼 | `AW`        | `12`  |
| 🇦🇺 | `AU`        | `13`  |
| 🇦🇹 | `AT`        | `14`  |
| 🇦🇿 | `AZ`        | `15`  |
| 🇧🇸 | `BS`        | `16`  |
| 🇧🇭 | `BH`        | `17`  |
| 🇧🇩 | `BD`        | `18`  |
| 🇧🇧 | `BB`        | `19`  |
| 🇧🇾 | `BY`        | `20`  |
| 🇧🇪 | `BE`        | `21`  |
| 🇧🇿 | `BZ`        | `22`  |
| 🇧🇯 | `BJ`        | `23`  |
| 🇧🇲 | `BM`        | `24`  |
| 🇧🇹 | `BT`        | `25`  |
| 🇧🇴 | `BO`        | `26`  |
| 🇧🇶 | `BQ`        | `27`  |
| 🇧🇦 | `BA`        | `28`  |
| 🇧🇼 | `BW`        | `29`  |
| 🇧🇻 | `BV`        | `30`  |
| 🇧🇷 | `BR`        | `31`  |
| 🇮🇴 | `IO`        | `32`  |
| 🇧🇳 | `BN`        | `33`  |
| 🇧🇬 | `BG`        | `34`  |
| 🇧🇫 | `BF`        | `35`  |
| 🇧🇮 | `BI`        | `36`  |
| 🇨🇻 | `CV`        | `37`  |
| 🇰🇭 | `KH`        | `38`  |
| 🇨🇲 | `CM`        | `39`  |
| 🇨🇦 | `CA`        | `40`  |
| 🇰🇾 | `KY`        | `41`  |
| 🇨🇫 | `CF`        | `42`  |
| 🇹🇩 | `TD`        | `43`  |
| 🇨🇱 | `CL`        | `44`  |
| 🇨🇳 | `CN`        | `45`  |
| 🇨🇽 | `CX`        | `46`  |
| 🇨🇨 | `CC`        | `47`  |
| 🇨🇴 | `CO`        | `48`  |
| 🇰🇲 | `KM`        | `49`  |
| 🇨🇬 | `CG`        | `50`  |
| 🇨🇰 | `CK`        | `51`  |
| 🇨🇷 | `CR`        | `52`  |
| 🇭🇷 | `HR`        | `53`  |
| 🇨🇺 | `CU`        | `54`  |
| 🇨🇼 | `CW`        | `55`  |
| 🇨🇾 | `CY`        | `56`  |
| 🇨🇿 | `CZ`        | `57`  |
| 🇨🇮 | `CI`        | `58`  |
| 🇩🇰 | `DK`        | `59`  |
| 🇩🇯 | `DJ`        | `60`  |
| 🇩🇲 | `DM`        | `61`  |
| 🇩🇴 | `DO`        | `62`  |
| 🇪🇨 | `EC`        | `63`  |
| 🇪🇬 | `EG`        | `64`  |
| 🇸🇻 | `SV`        | `65`  |
| 🇬🇶 | `GQ`        | `66`  |
| 🇪🇷 | `ER`        | `67`  |
| 🇪🇪 | `EE`        | `68`  |
| 🇸🇿 | `SZ`        | `69`  |
| 🇪🇹 | `ET`        | `70`  |
| 🇫🇰 | `FK`        | `71`  |
| 🇫🇴 | `FO`        | `72`  |
| 🇫🇯 | `FJ`        | `73`  |
| 🇫🇮 | `FI`        | `74`  |
| 🇫🇷 | `FR`        | `75`  |
| 🇬🇫 | `GF`        | `76`  |
| 🇵🇫 | `PF`        | `77`  |
| 🇹🇫 | `TF`        | `78`  |
| 🇬🇦 | `GA`        | `79`  |
| 🇬🇲 | `GM`        | `80`  |
| 🇬🇪 | `GE`        | `81`  |
| 🇩🇪 | `DE`        | `82`  |
| 🇬🇭 | `GH`        | `83`  |
| 🇬🇮 | `GI`        | `84`  |
| 🇬🇷 | `GR`        | `85`  |
| 🇬🇱 | `GL`        | `86`  |
| 🇬🇩 | `GD`        | `87`  |
| 🇬🇵 | `GP`        | `88`  |
| 🇬🇺 | `GU`        | `89`  |
| 🇬🇹 | `GT`        | `90`  |
| 🇬🇬 | `GG`        | `91`  |
| 🇬🇳 | `GN`        | `92`  |
| 🇬🇼 | `GW`        | `93`  |
| 🇬🇾 | `GY`        | `94`  |
| 🇭🇹 | `HT`        | `95`  |
| 🇭🇲 | `HM`        | `96`  |
| 🇭🇳 | `HN`        | `97`  |
| 🇭🇰 | `HK`        | `98`  |
| 🇭🇺 | `HU`        | `99`  |
| 🇮🇸 | `IS`        | `100` |
| 🇮🇳 | `IN`        | `101` |
| 🇮🇩 | `ID`        | `102` |
| 🇮🇷 | `IR`        | `103` |
| 🇮🇶 | `IQ`        | `104` |
| 🇮🇪 | `IE`        | `105` |
| 🇮🇲 | `IM`        | `106` |
| 🇮🇱 | `IL`        | `107` |
| 🇮🇹 | `IT`        | `108` |
| 🇯🇲 | `JM`        | `109` |
| 🇯🇵 | `JP`        | `110` |
| 🇯🇪 | `JE`        | `111` |
| 🇯🇴 | `JO`        | `112` |
| 🇰🇿 | `KZ`        | `113` |
| 🇰🇪 | `KE`        | `114` |
| 🇰🇮 | `KI`        | `115` |
| 🇽🇰 | `XK`        | `116` |
| 🇰🇼 | `KW`        | `117` |
| 🇰🇬 | `KG`        | `118` |
| 🇱🇦 | `LA`        | `119` |
| 🇱🇻 | `LV`        | `120` |
| 🇱🇧 | `LB`        | `121` |
| 🇱🇸 | `LS`        | `122` |
| 🇱🇷 | `LR`        | `123` |
| 🇱🇾 | `LY`        | `124` |
| 🇱🇮 | `LI`        | `125` |
| 🇱🇹 | `LT`        | `126` |
| 🇱🇺 | `LU`        | `127` |
| 🇲🇴 | `MO`        | `128` |
| 🇲🇬 | `MG`        | `129` |
| 🇲🇼 | `MW`        | `130` |
| 🇲🇾 | `MY`        | `131` |
| 🇲🇻 | `MV`        | `132` |
| 🇲🇱 | `ML`        | `133` |
| 🇲🇹 | `MT`        | `134` |
| 🇲🇭 | `MH`        | `135` |
| 🇲🇶 | `MQ`        | `136` |
| 🇲🇷 | `MR`        | `137` |
| 🇲🇺 | `MU`        | `138` |
| 🇾🇹 | `YT`        | `139` |
| 🇲🇽 | `MX`        | `140` |
| 🇫🇲 | `FM`        | `141` |
| 🇲🇩 | `MD`        | `142` |
| 🇲🇨 | `MC`        | `143` |
| 🇲🇳 | `MN`        | `144` |
| 🇲🇪 | `ME`        | `145` |
| 🇲🇸 | `MS`        | `146` |
| 🇲🇦 | `MA`        | `147` |
| 🇲🇿 | `MZ`        | `148` |
| 🇲🇲 | `MM`        | `149` |
| 🇳🇦 | `NA`        | `150` |
| 🇳🇷 | `NR`        | `151` |
| 🇳🇵 | `NP`        | `152` |
| 🇳🇱 | `NL`        | `153` |
| 🇳🇨 | `NC`        | `154` |
| 🇳🇿 | `NZ`        | `155` |
| 🇳🇮 | `NI`        | `156` |
| 🇳🇪 | `NE`        | `157` |
| 🇳🇬 | `NG`        | `158` |
| 🇳🇺 | `NU`        | `159` |
| 🇳🇫 | `NF`        | `160` |
| 🇰🇵 | `KP`        | `161` |
| 🇲🇰 | `MK`        | `162` |
| 🇲🇵 | `MP`        | `163` |
| 🇳🇴 | `NO`        | `164` |
| 🇴🇲 | `OM`        | `165` |
| 🇵🇰 | `PK`        | `166` |
| 🇵🇼 | `PW`        | `167` |
| 🇵🇸 | `PS`        | `168` |
| 🇵🇦 | `PA`        | `169` |
| 🇵🇬 | `PG`        | `170` |
| 🇵🇾 | `PY`        | `171` |
| 🇵🇪 | `PE`        | `172` |
| 🇵🇭 | `PH`        | `173` |
| 🇵🇳 | `PN`        | `174` |
| 🇵🇱 | `PL`        | `175` |
| 🇵🇹 | `PT`        | `176` |
| 🇵🇷 | `PR`        | `177` |
| 🇶🇦 | `QA`        | `178` |
| 🇨🇩 | `CD`        | `179` |
| 🇷🇴 | `RO`        | `180` |
| 🇷🇺 | `RU`        | `181` |
| 🇷🇼 | `RW`        | `182` |
| 🇷🇪 | `RE`        | `183` |
| 🇧🇱 | `BL`        | `184` |
| 🇸🇭 | `SH`        | `185` |
| 🇰🇳 | `KN`        | `186` |
| 🇱🇨 | `LC`        | `187` |
| 🇲🇫 | `MF`        | `188` |
| 🇵🇲 | `PM`        | `189` |
| 🇻🇨 | `VC`        | `190` |
| 🇼🇸 | `WS`        | `191` |
| 🇸🇲 | `SM`        | `192` |
| 🇸🇹 | `ST`        | `193` |
| 🇸🇦 | `SA`        | `194` |
| 🇸🇳 | `SN`        | `195` |
| 🇷🇸 | `RS`        | `196` |
| 🇸🇨 | `SC`        | `197` |
| 🇸🇱 | `SL`        | `198` |
| 🇸🇬 | `SG`        | `199` |
| 🇸🇽 | `SX`        | `200` |
| 🇸🇰 | `SK`        | `201` |
| 🇸🇮 | `SI`        | `202` |
| 🇸🇧 | `SB`        | `203` |
| 🇸🇴 | `SO`        | `204` |
| 🇿🇦 | `ZA`        | `205` |
| 🇬🇸 | `GS`        | `206` |
| 🇰🇷 | `KR`        | `207` |
| 🇸🇸 | `SS`        | `208` |
| 🇪🇸 | `ES`        | `209` |
| 🇱🇰 | `LK`        | `210` |
| 🇸🇩 | `SD`        | `211` |
| 🇸🇷 | `SR`        | `212` |
| 🇸🇯 | `SJ`        | `213` |
| 🇸🇪 | `SE`        | `214` |
| 🇨🇭 | `CH`        | `215` |
| 🇸🇾 | `SY`        | `216` |
| 🇨🇳 | `TW`        | `217` |
| 🇹🇯 | `TJ`        | `218` |
| 🇹🇿 | `TZ`        | `219` |
| 🇹🇭 | `TH`        | `220` |
| 🇹🇱 | `TL`        | `221` |
| 🇹🇬 | `TG`        | `222` |
| 🇹🇰 | `TK`        | `223` |
| 🇹🇴 | `TO`        | `224` |
| 🇹🇹 | `TT`        | `225` |
| 🇹🇳 | `TN`        | `226` |
| 🇹🇲 | `TM`        | `227` |
| 🇹🇨 | `TC`        | `228` |
| 🇹🇻 | `TV`        | `229` |
| 🇹🇷 | `TR`        | `230` |
| 🇺🇬 | `UG`        | `231` |
| 🇺🇦 | `UA`        | `232` |
| 🇦🇪 | `AE`        | `233` |
| 🇬🇧 | `GB`        | `234` |
| 🇺🇸 | `US`        | `235` |
| 🇺🇲 | `UM`        | `236` |
| 🇺🇾 | `UY`        | `237` |
| 🇺🇿 | `UZ`        | `238` |
| 🇻🇺 | `VU`        | `239` |
| 🇻🇦 | `VA`        | `240` |
| 🇻🇪 | `VE`        | `241` |
| 🇻🇳 | `VN`        | `242` |
| 🇻🇬 | `VG`        | `243` |
| 🇻🇮 | `VI`        | `244` |
| 🇼🇫 | `WF`        | `245` |
| 🇪🇭 | `EH`        | `246` |
| 🇾🇪 | `YE`        | `247` |
| 🇿🇲 | `ZM`        | `248` |
| 🇿🇼 | `ZW`        | `249` |
| 🇦🇽 | `AX`        | `250` |
| 🇮🇨 | `IC`        | `251` |