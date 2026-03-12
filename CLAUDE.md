# Village Shop System Mod

## プロジェクト概要
Minecraft 1.21.1用の村人自動交易システムMod。村に商品棚やレジを設置すると、村人が自動的に棚を巡回して交易を行う。Architectury APIによるマルチローダー（Fabric + NeoForge）対応。マルチプレイ対応必須。将来的に1.20.1にも移植予定。

## 技術スタック
- **Minecraft**: 1.21.1
- **Architectury API**: 13.0.8
- **Fabric Loader**: 0.18.4 / **NeoForge**: 21.1.219
- **Java**: 21
- **Mod ID**: `village_shop_system`
- **パッケージ**: `com.github.leopoko.village_shop_system`

## ビルド
```bash
./gradlew build              # 両ローダーのJAR生成
./gradlew fabric:runClient   # Fabric開発環境で起動
./gradlew neoforge:runClient # NeoForge開発環境で起動
```

## 言語ファイル
- メイン: `en_us.json`
- サブ: `ja_jp.json`
- パス: `common/src/main/resources/assets/village_shop_system/lang/`

---

## 要件定義（ゲーム仕様）

### 売却棚A (SellingShelf)
- 商品アイテム用入力スロット18個（3行x6列）、エメラルド出力スロット9個（3行x3列）
- ホッパー: 上面・側面から商品アイテム搬入、底面からエメラルド搬出
- 村人が棚のアイテムをエメラルドで買い取る
- レートはバニラの取引内容（物→エメラルド）を参照
- **売却ペナルティ**: デフォルトでエメラルド-1。1エメラルドの物は必要個数が増える。64スタック上限
- 村人が自らの取引内容と一致する場合、ペナルティ無し（未実装）
- ツールチップで売却価格を表示

### 売却棚B (SellingShelfB)
- 売却棚Aと同じ機能 + 会計システム連携
- 店グループ名を設定する欄がある
- エメラルドは同じ店グループのレジに転送される（レジが無い場合はローカル出力スロットに入る）

### 購入棚 (PurchaseShelf)
- エメラルド入力スロット18個（3行x6列）、購入品出力スロット9個（3行x3列）、設定用ゴーストスロット1個
- ホッパー: 上面・側面からエメラルド搬入、底面から商品搬出
- 設定されたアイテムを村人が補充する（エメラルドを消費してアイテムを出力スロットに生成）
- レートはバニラの取引内容（エメラルド→物）を参照、値段はデフォルトx5.0
- 設定アイテムはゴーストスロットにアイテムを持ってクリックで設定（アイテムは消費しない）
- ツールチップで購入価格を表示

### レジ (Register)
- エメラルド専用スロット18個（3行x6列）。プレイヤーはアイテムを入れられない（取り出しのみ）
- ホッパー: 底面からエメラルド搬出のみ。搬入不可
- 店グループ名を設定する欄がある
- 会計システムで売却棚Bからエメラルドが転送されてくる

### 店舗設定棒 (ChairSettingStick)
- Shift+右クリックで3モードを循環:
  1. **店グループ設定モード**: 右クリックでGUI（テキスト入力）を開き、この棒に割り当てるグループ名を設定。売却棚B/レジをクリックするとグループ名を適用
  2. **椅子設定モード**: 階段ブロックをクリックで椅子として登録（グループ名要設定済）
  3. **立ち位置設定モード**: ブロックをクリックでその上を立ち位置として登録（グループ名要設定済）
- 売却棚B/レジに対してスニーク+素手で右クリックしても店グループ設定画面を直接開ける

### 会計システム（店グループ）
- 複数の売却棚Bとレジを店グループ名で紐付け
- 村人が店グループ内の売却棚Bの一つにインタラクトすると「買い物中」状態になる
- 買い物中の村人は店グループ内の他の棚1〜3個を巡回して取引
- その後レジに行き、パーティクル+効果音で「支払い」演出
- 購入物に食料やポーション等がある場合、椅子/立ち位置があれば移動して休憩
- 全ての椅子/立ち位置が埋まっている場合はそのまま買い物中状態を解除
- データは `SavedData` (ShopGroupManager) でOverworldに永続保存

### 交易ルール
- このmodの交易でも村人は経験値を獲得する
- 村人は自分の職業・取引内容に関係なく売却棚で取引可能
- 交易可能なアイテム:
  1. configで追加したカスタム価格アイテム（`customSellPrices`/`customBuyPrices`）
  2. エンチャント本（`enableEnchantedBookTrading`で切替可能、レベル・レア度ベースで価格決定）
  3. バニラの村人取引テーブルに存在するアイテム
  4. 食糧値を持つアイテム（栄養値ベースで価格決定）
  5. ツール（破壊速度・耐久値ベースで価格決定）
  6. ポーション（`enablePotionSelling`で切替可能、効果の種類・持続時間ベースで価格決定）
- 村人には1回の取引セッションごとにランダムな予算がある（configの50%〜150%）
- **買い物ラッシュ**: 取引成立時に低確率（`shoppingRushChance`）で周囲の村人のクールダウンをリセット
- **食事演出**: 食料購入後の休憩中に村人が食事エフェクトを表示（`villagerEatingEffects`で切替可能）
- **睡眠中クールダウン一時停止**: 村人の睡眠中はクールダウンが進行しない（`pauseCooldownWhileSleeping`で切替可能）

### 椅子の実装
- 不可視の `SeatEntity`（カスタムEntity）を階段ブロック上にスポーンし、村人を騎乗させる
- 物理判定なし、不可視、重力なし、無敵
- 乗客がいなくなると自動的にdiscard（サーバー側のみ判定）

---

## ファイル構成と各クラスの役割

### エントリポイント
```
Village_shop_system.java        # 共通init: 全レジストリ + パケット登録
Village_shop_systemClient.java  # クライアントinit: Screen登録 + コールバック設定
```

### block/ - ブロッククラス
```
AbstractShopBlock.java          # 抽象基底: 方向対応, 右クリックでGUI, 破壊時ドロップ
                                # スニーク+素手でSellingShelfB/Registerの店グループ設定画面を開く
                                # static BiConsumer<String, BlockPos> openBlockGroupScreen コールバック
SellingShelfBlock.java          # 売却棚A
SellingShelfBBlock.java         # 売却棚B
PurchaseShelfBlock.java         # 購入棚
RegisterBlock.java              # レジ
```

### blockentity/ - BlockEntityクラス
```
BaseShelfBlockEntity.java       # 抽象基底: WorldlyContainer + MenuProvider
                                # INPUT_SLOTS=18, OUTPUT_SLOTS=9, TOTAL_SLOTS=27
                                # NBT保存/読込, マルチプレイ同期 (getUpdateTag/syncToClient)
SellingShelfBlockEntity.java    # 売却棚A BE: processTrades(budget)で取引実行
SellingShelfBBlockEntity.java   # 売却棚B BE: shopGroupフィールド, エメラルドをレジに転送
PurchaseShelfBlockEntity.java   # 購入棚 BE: configuredItemフィールド, CONFIG_SLOT追加
                                # PURCHASE_TOTAL_SLOTS = TOTAL_SLOTS + 1 = 28
RegisterBlockEntity.java        # レジ BE: shopGroupフィールド, insertEmeralds(), ホッパー搬出のみ
```

### menu/ - メニュー（コンテナ）クラス
```
SellingShelfMenu.java           # 入力18 + 出力9 + プレイヤー36 = 63スロット
PurchaseShelfMenu.java          # 入力18(エメラルドのみ) + 出力9 + 設定1 + プレイヤー36 = 64スロット
                                # CONFIG_SLOT_INDEX = 27 (ゴーストスロット, clicked()でオーバーライド)
RegisterMenu.java               # 出力18 + プレイヤー36 = 54スロット
```

### screen/ - GUI画面（クライアント専用だがcommonに配置）
```
SellingShelfScreen.java         # 194px幅, プログラム描画(ShopGuiHelper), "?"ボタンで取引可能アイテム一覧
PurchaseShelfScreen.java        # 194px幅, プログラム描画, 金色ハイライトの設定スロット, "?"ボタン
RegisterScreen.java             # 176px幅, PNGテクスチャ (register_block.png), 6列のみ
ShopGroupSettingScreen.java     # テキスト入力画面, 2モード: stick/block (BlockPos nullableで判別)
ShopGuiHelper.java              # バニラ風パネル/スロットのプログラム描画ユーティリティ
TradeListOverlay.java           # ページネーション付き取引可能アイテム一覧オーバーレイ
                                # "?"ボタンから表示、画面中央にモーダル表示
```

### trade/ - 取引システム
```
TradeRegistry.java              # 取引価格データベース
                                # 2パス: (1) ハードコード済みバニラ取引 (2) VillagerTrades動的スキャン
                                # エンコード: (emeralds << 16) | itemCount
                                # 遅延初期化: 初回アクセス時にEntityが必要
TradePriceCalculator.java       # 価格計算ロジック
                                # calculateSellPrice(ItemStack): 売却価格（ペナルティ適用済）
                                # calculateBuyPrice(Item, count): 購入価格（マークアップ適用済）
                                # calculateBuyPrice(ItemStack, count): エンチャント本対応版
                                # calculatePotionSellPrice(ItemStack): ポーション売却価格
                                # calculateEnchantedBookSellPrice(ItemStack): エンチャント本売却価格
                                # calculateEnchantedBookPrice(ItemStack, count): エンチャント本購入価格
                                # getSellTradeRatio(Item): [emeralds, itemCount] or null
                                # isTradeable/isSellable/isBuyableStack: 食料・ツール・ポーション・エンチャント本判定含む
```

### shopgroup/ - 店グループ
```
ShopGroup.java                  # データ構造: name, sellingShelfBPositions, registerPositions,
                                # chairPositions, standingPositions. NBT保存/読込
ShopGroupManager.java           # SavedData: Overworldに永続保存
                                # グループのCRUD, removeBlockFromAllGroups(破壊時)
```

### entity/ - カスタムEntity
```
SeatEntity.java                 # 椅子用の不可視Entity（ArmorStandの代替）
                                # noPhysics, invisible, noGravity, silent, invulnerable
                                # 乗客がいなくなるとサーバー側で自動discard
```

### villager/ - 村人AI
```
VillagerShopBehavior.java       # 村人の買い物行動ロジック (毎tick呼ばれる)
                                # State: IDLE -> NAVIGATING -> SHOPPING_VISITING -> REGISTER -> REST -> IDLE
                                # 予算システム: tradeBudget (config値の50%-150%)
                                # 椅子に座る: SeatEntity生成 -> 村人を騎乗
                                # 買い物ラッシュ: shoppingRushChanceで周囲の村人のクールダウンリセット
                                # 食事演出: 休憩中に食料消費エフェクト表示
ShopBehaviorAccessor.java       # Duck interface: VillagerShopBehaviorへのアクセス
```

### mixin/
```
VillagerMixin.java              # Villager.customServerAiStep()のTAILにinject
                                # VillagerShopBehavior.tick()を毎tick呼び出し
```

### network/
```
ModPackets.java                 # SHOP_GROUP_UPDATE (C2S): BlockPos + groupName -> 棚/レジのグループ更新
                                # STICK_GROUP_UPDATE (C2S): groupName -> 設定棒のグループ更新
                                # Architectury NetworkManager使用
```

### config/
```
ModConfig.java                  # JSON永続化Config（Gson + Architectury Platform.getConfigFolder()）
                                # 静的フィールド + ConfigData内部クラスでload/save
                                # カスタム価格マップ: customSellPrices, customBuyPrices, enchantedBookBuyPrices
ModConfigScreen.java            # Cloth Config 2 APIによるGUI設定画面
                                # カテゴリ分け: 村人AI / 取引レート / 機能トグル / 行動設定
```

### registry/
```
ModBlocks.java                  # 4ブロック登録
ModItems.java                   # 4ブロックアイテム + 設定棒
ModBlockEntities.java           # 4 BlockEntityType
ModMenuTypes.java               # 3 MenuType (SellingShelf, PurchaseShelf, Register)
ModEntityTypes.java             # SeatEntity登録
ModCreativeTabs.java            # クリエイティブタブ
```

### ローダー固有
```
fabric/Village_shop_systemFabric.java               # ModInitializer -> Village_shop_system.init()
fabric/client/Village_shop_systemFabricClient.java   # ClientModInitializer -> Village_shop_systemClient.init()
neoforge/Village_shop_systemNeoForge.java            # @Mod -> Village_shop_system.init()
neoforge/Village_shop_systemNeoForgeClient.java      # RegisterMenuScreensEvent + FMLClientSetupEvent
```

---

## GUIレイアウト

### 売却棚 / 購入棚 (194px幅)
- `imageWidth = 194`, `imageHeight = 166`
- 入力スロット: 3行x6列, x=8開始, y=18開始 (各18px間隔)
- 出力スロット: 3行x3列, x=126開始, y=18開始
- プレイヤーインベントリ: x=16開始, y=84 (3行) / y=142 (ホットバー)
- "?"情報ボタン: x=116, y=4, w=9, h=10
- 購入棚の設定スロット: x=116, y=36 (金色ハイライト)
- **PNGテクスチャ不使用**: `ShopGuiHelper` でプログラム描画

### レジ (176px幅)
- `imageWidth = 176`, `imageHeight = 166`
- エメラルドスロット: 3行x6列, x=8, y=18
- プレイヤーインベントリ: x=8, y=84 / y=142
- PNGテクスチャ使用: `register_block.png`

---

## 取引価格システム詳細

### 価格ソース（優先順）
1. **カスタム価格（config）**: `customSellPrices`/`customBuyPrices` で個別設定（ペナルティ/マークアップ適用なし）
2. **エンチャント本**: `enableEnchantedBookTrading`が有効時。エンチャントレベル・レア度ベースで価格決定。`enchantedBookBuyPrices`で個別上書き可能
3. **バニラ取引テーブル**: `TradeRegistry` のハードコード + `VillagerTrades.TRADES` スキャン
4. **食料価格**: `nutrition * count * foodPricePerHunger` (デフォルト: 0.25 = 栄養4で1エメラルド)
5. **ツール価格**: `durability * toolDurabilityFactor + speed * toolSpeedFactor`
6. **ポーション**: `enablePotionSelling`が有効時。効果の種類・持続時間ベースで価格決定
7. 上記いずれにも該当しないアイテムは取引不可

### 売却価格
- カスタム: configで指定した固定値（ペナルティなし）
- エンチャント本: エンチャントレベル * レア度係数 + `enchantedBookBasePrice`
- バニラ取引: `(emeralds * count) / tradeItemCount - sellingEmeraldPenalty`
- 食料: `nutrition * count * foodPricePerHunger - sellingEmeraldPenalty`
- ツール: `durability * 0.005 + speed * 0.5 - sellingEmeraldPenalty`
- ポーション: `potionBasePrice` + 効果数 + 持続時間 / `potionDurationUnit`

### 購入価格
- カスタム: configで指定した固定値（マークアップなし）
- エンチャント本: `enchantedBookBuyPrices`で個別設定、または自動計算
- バニラ買い取引あり: `ceil(baseEmeralds * purchasePriceMultiplier)` (x5.0)
- バニラ売り取引のみ: `ceil(baseEmeralds * purchasePriceMultiplier * 1.5)` (x7.5)

### Config値一覧
| 項目 | フィールド | デフォルト |
|------|-----------|-----------|
| 村人検索範囲 | `villagerSearchRange` | 32 |
| 買い物クールダウン | `villagerShoppingCooldownTicks` | 2400 (2分) |
| 村人予算/取引 | `villagerBudgetPerTrade` | 20 |
| 取引経験値 | `villagerTradeXp` | 1 |
| 売却ペナルティ | `sellingEmeraldPenalty` | 1 |
| 購入マークアップ | `purchasePriceMultiplier` | 5.0 |
| 食料単価/栄養 | `foodPricePerHunger` | 0.25 |
| ツール耐久係数 | `toolDurabilityFactor` | 0.005 |
| ツール速度係数 | `toolSpeedFactor` | 0.5 |
| ポーション基本価格 | `potionBasePrice` | 3 |
| ポーション持続時間単位 | `potionDurationUnit` | 3600 (3分) |
| エンチャント本基本価格 | `enchantedBookBasePrice` | 5 |
| ポーション売却有効 | `enablePotionSelling` | true |
| エンチャント本取引有効 | `enableEnchantedBookTrading` | true |
| 休憩時間(min/max) | `minRestTicks`/`maxRestTicks` | 100/200 |
| 巡回棚数(min/max) | `minShelvesToVisit`/`maxShelvesToVisit` | 1/3 |
| 睡眠中クールダウン停止 | `pauseCooldownWhileSleeping` | true |
| 食事演出 | `villagerEatingEffects` | true |
| 買い物ラッシュ確率 | `shoppingRushChance` | 0.005 (0.5%) |
| カスタム売却価格 | `customSellPrices` | {cake: [1,1]} |
| カスタム購入価格 | `customBuyPrices` | {} |
| エンチャント本購入価格 | `enchantedBookBuyPrices` | {mending: 125} |

---

## 村人AI フロー (VillagerShopBehavior)

```
IDLE (200tick毎に検索)
  | 範囲内に取引可能な棚を発見、ランダム予算設定
NAVIGATING_TO_SHELF
  | 棚に到着、processTrades(budget)実行
  |-- 売却棚Aの場合 -> クールダウンしてIDLEへ
  +-- 売却棚Bの場合 -> 店グループあり
SHOPPING_VISITING_SHELVES (1-3棚巡回)
  | 巡回完了
SHOPPING_GOING_TO_REGISTER (最寄りレジへ)
  | レジ到着（効果音+パーティクル）
SHOPPING_GOING_TO_REST (椅子/立ち位置の最寄りへ)
  | 到着
RESTING (100-200tick)
  | 時間切れ
IDLE (クールダウン付き)
```

---

## ネットワークパケット

| ID | 方向 | 内容 |
|----|------|------|
| `shop_group_update` | C2S | BlockPos + groupName(max 64) -> SellingShelfB/RegisterのshopGroup更新 |
| `stick_group_update` | C2S | groupName(max 64) -> ChairSettingStickのshopGroup更新 |

---

## コーディング規約・注意事項

### Architectury
- 共通コードは全て `common/` に配置する
- `DeferredRegister` でブロック/アイテム/BlockEntity/Menuを登録
- ネットワーク通信は `Architectury NetworkManager` を使用
- Screen クラスはクライアント専用だが `common/screen/` に配置（各ローダーから参照）

### ローダー別の画面登録
- **NeoForge**: `RegisterMenuScreensEvent` で画面登録（`FMLClientSetupEvent` では遅すぎる）
- **Fabric**: `Village_shop_systemClient.init()` で `MenuRegistry.registerScreenFactory` 使用

### MC 1.21.1 固有の注意
- `Screen.render()` はデフォルトでブラー効果を追加する。`ShopGroupSettingScreen` では `renderBackground()` をオーバーライドしてブラーを避ける
- `Items.SCUTE` は存在しない。`Items.TURTLE_SCUTE` を使用
- 椅子にはカスタム `SeatEntity` を使用（ArmorStandではない）
- `Collections.shuffle(list, RandomSource)` は使えない。手動 Fisher-Yates シャッフルを使用
- `ItemStack.save()` / `ItemStack.parse()` は `HolderLookup.Provider` / `registries` 引数が必要

### マルチプレイ同期
- BlockEntity同期: `getUpdateTag()` + `syncToClient()` (`setChanged()` + `level.sendBlockUpdated()`)
- ShopGroupManager: サーバー側のみ管理（Overworld SavedData）
- GUI操作: C2Sパケットでサーバー処理

### 1.20.1移植を考慮
- Brain API は使わず Mixin + customServerAiStep inject
- バージョン依存APIの使用を最小限に

---

## リソースファイル

### テクスチャ
- `textures/gui/register_block.png` - レジGUI用（176x166）
- `textures/gui/selling_shelf.png` - 未使用（プログラム描画に移行済）
- `textures/gui/purchase_shelf.png` - 未使用（プログラム描画に移行済）

### モデル・ブロックステート
- `blockstates/` - 4ブロック分（selling_shelf, selling_shelf_b, purchase_shelf, register_block）
- `models/block/` - 4ブロックモデル
- `models/item/` - 4ブロックアイテム + chair_setting_stick

### ルートテーブル
- `data/village_shop_system/loot_table/blocks/` - 4ブロック分

### Mixin設定
- `village_shop_system.mixins.json` - VillagerMixin登録

---

## 実装済み（以前のTODO）
- [x] Cloth Config 統合（ModConfigScreen.java + JSON永続化）
- [x] ポーション取引（効果の種類・持続時間ベースで価格決定、`enablePotionSelling`で切替可能）
- [x] エンチャント本取引（レベル・レア度ベースで価格決定、`enableEnchantedBookTrading`で切替可能）
- [x] config で追加取引アイテム設定（`customSellPrices`/`customBuyPrices`/`enchantedBookBuyPrices`）
- [x] ArmorStand → カスタム SeatEntity に置き換え

## バージョン更新手順
1. `gradle.properties` の `mod_version` を新バージョンに変更（例: `1.2` → `1.3`）
2. `CHANGELOG.md` の先頭に新バージョンのセクションを追加（`## [x.x]` 形式）
3. ビルド確認: `./gradlew build`

## 未実装・TODO
- [ ] 村人が自分の職業と一致するアイテムの売却ペナルティ免除
- [ ] 複数村人の同時棚操作時のロック機構
- [ ] 椅子/立ち位置が全て埋まっている場合のスキップ処理の精緻化
- [ ] 購入棚の設定アイテムを周囲の村人の取引内容から選択するUI（現在はゴーストスロット方式）
