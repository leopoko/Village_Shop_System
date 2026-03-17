# Village Shop System

An automated villager trading system mod for Minecraft 1.21.1.
Place shelves and registers in your village, and villagers will automatically browse the shelves and trade items.

**Loaders:** Fabric / NeoForge (multi-loader via Architectury API)
**Version:** Minecraft 1.21.1
**License:** MIT

## Required Dependencies

| Mod | Fabric | NeoForge |
|-----|--------|----------|
| [Architectury API](https://modrinth.com/mod/architectury-api) | 13.0.8+ | 13.0.8+ |
| [Fabric API](https://modrinth.com/mod/fabric-api) | Required | — |
| [Cloth Config](https://modrinth.com/mod/cloth-config) | 15.0+ | 15.0+ |

### Optional

| Mod | Description |
|-----|-------------|
| [Mod Menu](https://modrinth.com/mod/modmenu) | Opens the config screen from Fabric's mod list (recommended) |

---

## Blocks

### Selling Shelf

Villagers buy items from this shelf with emeralds.

- **Input slots (18):** Place items you want to sell
- **Output slots (9):** Emeralds paid by villagers appear here
- **Hopper:** Items in from top/sides, emeralds out from bottom
- Hover over items to see sell prices in the tooltip
- Press the "?" button in the GUI to view all tradeable items

### Selling Shelf (Accounting)

Same functionality as the Selling Shelf, plus **Shop Group** integration (see below).

- Same input/output slot layout as the Selling Shelf
- Set a **shop group name** to automatically transfer emeralds to registers in the same group
- If no register exists in the group, emeralds go to the local output slots
- When a villager interacts with this shelf, they enter a "shopping" state and visit other shelves in the same group

### Purchase Shelf

Spend emeralds to purchase items from villagers.

- **Input slots (18):** Place emeralds here
- **Output slots (9):** Purchased items appear here
- **Config slot (1):** Set which item to buy (the item is not consumed)
- **Hopper:** Emeralds in from top/sides, items out from bottom
- To configure: Click the gold-highlighted config slot while holding the desired item

### Cash Register

Receives emeralds from Selling Shelves (Accounting) within the same shop group.

- **Emerald slots (18):** View and withdraw only (players cannot insert items)
- **Hopper:** Emeralds out from bottom only (no input)
- Set a **shop group name** to link with Selling Shelves (Accounting)

---

## Shop Group System

Link multiple Selling Shelves (Accounting) and Registers under a **shop group name** so villagers browse multiple shelves in one shopping trip.

### Setup

1. Place Selling Shelves (Accounting) and Registers
2. Assign a shop group name using either method:
   - Use the **Shop Setting Stick** (see below)
   - **Sneak + right-click with empty hand** on a Selling Shelf (Accounting) or Register to open the group setting screen
3. Blocks with the same group name are linked together

### Villager Shopping Flow

1. A villager finds a Selling Shelf (Accounting) within range and interacts with it
2. Visits **1–3 other shelves** in the same group to trade
3. Walks to the nearest **Register** for a payment animation (sound + particles)
4. If chairs or standing positions are set, the villager **rests** there (with eating effects)
5. After a cooldown, the villager starts shopping again

---

## Shop Setting Stick

An item for configuring shop groups, chairs, and standing positions.

**Shift + right-click** to cycle through modes:

| Mode | Description |
|------|-------------|
| **Shop Group Setting** | Right-click to open GUI and assign a group name to the stick. Then click on a Selling Shelf (Accounting) or Register to apply the name |
| **Chair Setting** | Click a stair block to register it as a chair (villagers sit here to rest) |
| **Standing Position Setting** | Click a block to register the space above it as a standing position (villagers stand here to rest) |

Note: A group name must be assigned to the stick before setting chairs or standing positions.

---

## Trading Rules

### Tradeable Items

Prices are determined in the following priority order:

1. **Custom config items** — Set individual prices in the config
2. **Enchanted books** — Auto-calculated based on level and rarity (toggleable in config)
3. **Vanilla villager trade table items** — Uses vanilla prices as a base
4. **Food** — Auto-calculated based on nutrition value
5. **Tools** — Auto-calculated based on durability and mining speed
6. **Potions** — Auto-calculated based on effect type and duration (toggleable in config)

Items not matching any of the above cannot be traded.

### Sell Prices

- Based on vanilla trade table prices, with a **sell penalty** applied (default: −1 emerald)
- The penalty also applies to auto-calculated items (food, tools, etc.)
- Custom config prices are **not** affected by the penalty

### Buy Prices

- Based on vanilla trade table prices, with a **purchase markup** applied (default: ×5.0)
- Custom config prices are **not** affected by the markup

### Villager Budget

- Each villager gets a random budget per shopping session (50%–150% of the config value)
- Trading stops when the budget runs out

### Shopping Rush

- When a trade completes, there is a small chance (default: 0.5%) of triggering a **shopping rush**
- All nearby villagers have their cooldowns reset and start shopping at once

---

## Configuration

Open the config screen in-game or edit the config file directly.

- **Fabric:** Open from Mod Menu
- **NeoForge:** Open from the mod list
- **File:** `config/village_shop_system.json`

### Main Settings

| Category | Setting | Default | Description |
|----------|---------|:---:|-------------|
| **Villager AI** | Search range | 32 | Range (in blocks) for villagers to find shelves |
| | Shopping cooldown | 2400 ticks (2 min) | Wait time between shopping sessions |
| | Shelves to visit (min/max) | 1 / 3 | Number of shelves visited per trip |
| | Budget per trade | 20 | Emerald budget per shopping session |
| **Trade Rates** | Sell penalty | 1 | Emerald deduction when selling |
| | Purchase markup | ×5.0 | Price multiplier when buying |
| | Food price per hunger | 0.25 | Emeralds per nutrition point |
| | Potion base price | 3 | Base sell price for potions |
| | Enchanted book base price | 5 | Base price for enchanted books |
| **Feature Toggles** | Potion selling | Enabled | Allow potion trading |
| | Enchanted book trading | Enabled | Allow enchanted book trading |
| | Eating effects | Enabled | Villager eating animation during rest |
| | Pause cooldown while sleeping | Enabled | Pause shopping cooldown while villagers sleep |
| **Behavior** | Rest time (min/max) | 100 / 200 ticks | Duration of chair rest |
| | Shopping rush chance | 0.5% | Probability of triggering a rush |

### Custom Prices

Edit the config file to set custom sell/buy prices for any item.

```json
{
  "customSellPrices": {
    "minecraft:cake": [1, 1]
  },
  "customBuyPrices": {},
  "enchantedBookBuyPrices": {
    "minecraft:mending": 125
  }
}
```

- `customSellPrices`: `"item_id": [emeralds, item_count]`
- `customBuyPrices`: `"item_id": emeralds`
- `enchantedBookBuyPrices`: `"enchantment_id": emeralds`

---

## Hopper Automation Guide

Hopper connection directions for each block:

| Block | Top / Sides (Input) | Bottom (Output) |
|-------|:---:|:---:|
| Selling Shelf | Items in | Emeralds out |
| Selling Shelf (Accounting) | Items in | Emeralds out |
| Purchase Shelf | Emeralds in | Items out |
| Cash Register | No input | Emeralds out |

---

## License

[MIT License](LICENSE.txt)

---

# Village Shop System (日本語)

Minecraft 1.21.1 用の村人自動交易システム Mod です。
村に商品棚やレジを設置すると、村人が自動的に棚を巡回してアイテムを売買します。

**対応ローダー:** Fabric / NeoForge（Architectury API によるマルチローダー対応）
**対応バージョン:** Minecraft 1.21.1
**ライセンス:** MIT

## 必須前提 Mod

| Mod | Fabric | NeoForge |
|-----|--------|----------|
| [Architectury API](https://modrinth.com/mod/architectury-api) | 13.0.8 以上 | 13.0.8 以上 |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 必須 | — |
| [Cloth Config](https://modrinth.com/mod/cloth-config) | 15.0 以上 | 15.0 以上 |

### 任意

| Mod | 説明 |
|-----|------|
| [Mod Menu](https://modrinth.com/mod/modmenu) | Fabric 環境で設定画面を開くためのメニュー（推奨） |

---

## ブロック一覧

### 売却棚 (Selling Shelf)

村人がアイテムをエメラルドで買い取ってくれる棚です。

- **入力スロット（18個）:** 売りたいアイテムを入れます
- **出力スロット（9個）:** 村人が支払ったエメラルドが入ります
- **ホッパー連携:** 上面・側面からアイテム搬入、底面からエメラルド搬出
- アイテムにカーソルを合わせると売却価格がツールチップに表示されます
- GUIの「?」ボタンから取引可能なアイテムの一覧を確認できます

### 売却棚・会計連携 (Selling Shelf - Accounting)

売却棚の機能に加えて、**店グループ**（後述）と連携できます。

- 売却棚と同じ入力・出力スロット構成
- **店グループ名**を設定すると、エメラルドが同グループのレジに自動転送されます
- レジが存在しない場合はローカルの出力スロットにエメラルドが入ります
- 村人がこの棚でインタラクトすると「買い物中」状態になり、同グループ内の他の棚も巡回します

### 購入棚 (Purchase Shelf)

エメラルドを支払ってアイテムを購入できる棚です。

- **入力スロット（18個）:** エメラルドを入れます
- **出力スロット（9個）:** 購入したアイテムが入ります
- **設定スロット（1個）:** 購入したいアイテムをここで設定します（アイテムは消費されません）
- **ホッパー連携:** 上面・側面からエメラルド搬入、底面からアイテム搬出
- 設定方法: 手持ちのアイテムで設定スロット（金色枠）をクリック

### レジ (Cash Register)

売却棚（会計連携）からエメラルドを受け取る専用ブロックです。

- **エメラルドスロット（18個）:** 表示・取り出し専用（プレイヤーはアイテムを入れられません）
- **ホッパー連携:** 底面からエメラルド搬出のみ（搬入不可）
- **店グループ名**を設定して売却棚（会計連携）と紐付けます

---

## 店グループシステム

複数の売却棚（会計連携）とレジを**店グループ名**でまとめることで、村人が店内を巡回するシステムです。

### セットアップ手順

1. 売却棚（会計連携）とレジを設置する
2. 以下のいずれかの方法で店グループ名を設定する:
   - **店舗設定棒** を使う（後述）
   - 売却棚（会計連携）またはレジに対して **スニーク + 素手で右クリック** して設定画面を開く
3. 同じグループ名を設定したブロック同士が連携します

### 村人の買い物フロー

1. 村人が範囲内の売却棚（会計連携）を見つけてインタラクト
2. 同グループ内の他の棚を **1〜3個** 巡回して取引
3. 最寄りの **レジ** に移動して支払い演出（効果音 + パーティクル）
4. 椅子や立ち位置が設定されていれば、そこで **休憩**（食事演出あり）
5. クールダウン後、再び買い物を開始

---

## 店舗設定棒 (Shop Setting Stick)

店グループの設定や椅子・立ち位置の登録を行うアイテムです。

**Shift + 右クリック** でモードを切り替えます:

| モード | 説明 |
|--------|------|
| **店グループ設定** | 右クリックで設定棒にグループ名を割り当てるGUIを開く。売却棚（会計連携）やレジをクリックするとグループ名を適用 |
| **椅子設定** | 階段ブロックをクリックして椅子として登録（村人が座って休憩する場所） |
| **立ち位置設定** | ブロックをクリックしてその上を立ち位置として登録（村人が立って休憩する場所） |

※ 椅子・立ち位置の設定には、事前に設定棒へのグループ名の割り当てが必要です。

---

## 取引ルール

### 取引可能なアイテム

以下の順に価格が決定されます:

1. **Config でカスタム設定したアイテム** — 個別に価格を指定可能
2. **エンチャント本** — レベルとレア度に基づいて自動計算（Config で切替可能）
3. **バニラの村人取引テーブルに含まれるアイテム** — バニラ価格を参照
4. **食料** — 栄養値に基づいて自動計算
5. **ツール** — 耐久値・採掘速度に基づいて自動計算
6. **ポーション** — 効果の種類・持続時間に基づいて自動計算（Config で切替可能）

上記のいずれにも該当しないアイテムは取引できません。

### 売却価格について

- バニラ取引テーブルの価格を基準に、**売却ペナルティ**（デフォルト: エメラルド −1）が適用されます
- 食料・ツールなどの自動計算アイテムにも同様のペナルティが適用されます
- Config のカスタム価格にはペナルティは適用されません

### 購入価格について

- バニラ取引テーブルの価格を基準に、**購入マークアップ**（デフォルト: ×5.0）が適用されます
- Config のカスタム価格にはマークアップは適用されません

### 村人の予算

- 村人には 1 回の取引セッションごとにランダムな予算があります（Config 設定値の 50%〜150%）
- 予算を使い切ると取引を終了します

### 買い物ラッシュ

- 取引が成立すると低確率（デフォルト: 0.5%）で**買い物ラッシュ**が発生
- 周囲の村人のクールダウンがリセットされ、一斉に買い物を始めます

---

## Config 設定

ゲーム内で設定画面を開くか、Config ファイルを直接編集できます。

- **Fabric:** Mod Menu から設定画面を開く
- **NeoForge:** Mod 一覧から設定画面を開く
- **ファイル:** `config/village_shop_system.json`

### 主な設定項目

| カテゴリ | 設定 | デフォルト値 | 説明 |
|----------|------|:---:|------|
| **村人AI** | 村人検索範囲 | 32 | 村人が棚を検索する範囲（ブロック） |
| | 買い物クールダウン | 2400 tick (2分) | 次の買い物までの待機時間 |
| | 巡回棚数（最小/最大） | 1 / 3 | 1回の買い物で回る棚の数 |
| | 予算/取引 | 20 | 1回の取引セッションでの予算（エメラルド） |
| **取引レート** | 売却ペナルティ | 1 | 売却時のエメラルド減額 |
| | 購入マークアップ | ×5.0 | 購入時の価格倍率 |
| | 食料単価/栄養 | 0.25 | 栄養値あたりのエメラルド数 |
| | ポーション基本価格 | 3 | ポーション売却の基本価格 |
| | エンチャント本基本価格 | 5 | エンチャント本の基本価格 |
| **機能トグル** | ポーション売却 | 有効 | ポーションの売却を許可するか |
| | エンチャント本取引 | 有効 | エンチャント本の売買を許可するか |
| | 食事演出 | 有効 | 休憩中の村人の食事エフェクト |
| | 睡眠中クールダウン停止 | 有効 | 村人の睡眠中にクールダウンを一時停止 |
| **行動** | 休憩時間（最小/最大） | 100 / 200 tick | 椅子での休憩時間 |
| | 買い物ラッシュ確率 | 0.5% | ラッシュ発生確率 |

### カスタム価格の設定

Config ファイルを編集することで、任意のアイテムに独自の売買価格を設定できます。

```json
{
  "customSellPrices": {
    "minecraft:cake": [1, 1]
  },
  "customBuyPrices": {},
  "enchantedBookBuyPrices": {
    "minecraft:mending": 125
  }
}
```

- `customSellPrices`: `"アイテムID": [エメラルド数, アイテム数]` の形式
- `customBuyPrices`: `"アイテムID": エメラルド数` の形式
- `enchantedBookBuyPrices`: `"エンチャントID": エメラルド数` の形式

---

## ホッパー自動化ガイド

各ブロックのホッパー接続方向:

| ブロック | 上面・側面（搬入） | 底面（搬出） |
|----------|:---:|:---:|
| 売却棚 | アイテム搬入 | エメラルド搬出 |
| 売却棚（会計連携） | アイテム搬入 | エメラルド搬出 |
| 購入棚 | エメラルド搬入 | アイテム搬出 |
| レジ | 搬入不可 | エメラルド搬出 |

---

## ライセンス

[MIT License](LICENSE.txt)
