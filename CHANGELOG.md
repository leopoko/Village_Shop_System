# Changelog

## [1.2] - 2026-11-03

Minecraft 1.20.1 (Fabric / Forge) 版の初公開。機能は 1.21.1 版 v1.2 と同等。
First public release for Minecraft 1.20.1 (Fabric / Forge), feature-equivalent to v1.2 for 1.21.1.

- Fix: 店舗設定棒（Shop Setting Stick）を持って売却棚B/レジを右クリックしても棚のGUIが開くだけで、店グループを適用できなかった問題を修正
  - ブロック側のGUI処理がアイテムの `useOn()` より先に実行されていたため。棒を持っている場合はブロック側の処理をスキップするようにした
- Add: 通貨アイテム設定 `currencyItem` を追加（デフォルト: `minecraft:emerald`）。他Modのコイン等をエメラルドの代わりに使える (#4)
  - バニラ取引から算出した価格は、このアイテムで1:1に支払われる
  - 価格ツールチップの通貨名も設定したアイテム名で表示されるようにした
  - 設定はサーバーとクライアントで同じ値にすること
- Fix: 既存の設定ファイルに `purchasePriceMultiplier` / `shoppingRushChance` のキーが無い場合、本来のデフォルト値（5.0 / 0.005）ではなく古い値（1.2 / 0.05）が使われていた問題を修正
- Change: 通貨の格納処理を共通化し、通貨アイテムの最大スタック数に従うようにした（64固定ではなくなった）

## [1.1]
- 買い物中の村人がバニラAIに妨害されてナビゲーションが中断される問題を修正
  - 買い物中にBrainのWALK_TARGETとINTERACTION_TARGETを毎tick消去するように変更

## [1.0]
- 初回リリース (Minecraft 1.20.1)
