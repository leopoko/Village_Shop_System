# Village Shop System Mod

## プロジェクト概要
Minecraft 1.21.1用の村人自動交易システムMod。Architectury APIによるマルチローダー（Fabric + NeoForge）対応。

## 技術スタック
- **Minecraft**: 1.21.1
- **Architectury API**: 13.0.8
- **Fabric Loader**: 0.18.4 / **NeoForge**: 21.1.219
- **Java**: 21
- **Mod ID**: `village_shop_system`
- **パッケージ**: `com.github.leopoko.village_shop_system`

## プロジェクト構造
```
common/   - 共通コード（ロジックの大半をここに配置）
fabric/   - Fabric固有のエントリポイント・クライアント
neoforge/ - NeoForge固有のエントリポイント
```

## 主要コンポーネント
- **売却棚 (SellingShelf)**: プレイヤーがアイテムを入れ、村人がエメラルドで買い取る
- **売却棚B (SellingShelfB)**: 売却棚Aと同機能 + 店グループ連携（エメラルドをレジに転送）
- **購入棚 (PurchaseShelf)**: エメラルドを入れ、設定したアイテムを村人が補充する
- **レジ (Register)**: 店グループのエメラルドを集約。ホッパーで搬出可能
- **店舗設定棒 (ChairSettingStick)**: 店グループ名設定、椅子登録、立ち位置登録の3モード切替

## コーディング規約
- 共通コードは全て `common/` に配置する
- Architectury APIの `DeferredRegister` を使用してブロック/アイテム/BlockEntity/Menuを登録
- クライアント専用コード（Screen等）は各ローダーのclientパッケージに配置
  - NeoForge: `RegisterMenuScreensEvent` で画面登録（`FMLClientSetupEvent` では遅すぎる）
  - Fabric: `Village_shop_systemClient.init()` で `MenuRegistry.registerScreenFactory` 使用
- ネットワーク通信は Architectury `NetworkManager` を使用
- ワールドデータ保存は `SavedData` を使用
- Hopper I/O は `WorldlyContainer` で実装
- 村人AIの注入は Mixin + Goal System（Brain APIは使わない、1.20.1移植性のため）
- MC 1.21.1 の `Screen.render()` はデフォルトでブラー効果を追加するため、カスタム画面では `renderBackground()` をオーバーライドする
- `ArmorStand.setMarker()` は private なので NBT save/load で設定
- `Collections.shuffle(list, RandomSource)` は使えないので手動 Fisher-Yates を使用

## 取引価格システム
1. **バニラ取引テーブル**: `TradeRegistry` が `VillagerTrades.TRADES` をスキャン
2. **食料価格**: 栄養値ベース（`ModConfig.foodPricePerHunger`）
3. **ツール価格**: 耐久値 × `toolDurabilityFactor` + 速度 × `toolSpeedFactor`
4. **売却ペナルティ**: エメラルド -1（`ModConfig.sellingEmeraldPenalty`）
5. **購入マークアップ**: × 1.2（`ModConfig.purchasePriceMultiplier`）

## ビルド
```bash
./gradlew build              # 両ローダーのJAR生成
./gradlew fabric:runClient   # Fabric開発環境で起動
./gradlew neoforge:runClient # NeoForge開発環境で起動
```

## 言語
- メイン: 英語 (en_us.json)
- サブ: 日本語 (ja_jp.json)

## 将来の対応
- 1.20.1への移植を予定。バージョン依存APIの使用を最小限にすること。
