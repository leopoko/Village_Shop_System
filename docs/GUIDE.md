# Village Shop System – Getting Started Guide

> 日本語版: [GUIDE_ja.md](GUIDE_ja.md)

This guide walks you through building your first shop, step by step, and explains what to check when "villagers don't trade".

**The short version:** you don't trade *with* villagers. You **stock a shelf**, and villagers walk over **on their own** and buy from it (or restock it). You just wait and collect the emeralds.

---

## 1. Your first shop in 3 minutes

You only need **one Selling Shelf** to try the mod.

1. **Craft a Selling Shelf**
   ```
   [Planks]    [Planks]  [Planks]
   [Bookshelf] [Emerald] [Bookshelf]
   [Planks]    [Planks]  [Planks]
   ```
2. **Place it inside a village**, on the ground where villagers can walk up to it (they must be able to stand within ~2 blocks of it).
3. **Open it and put in something villagers want.** Hover over an item in the shelf: if the tooltip shows `Sell: ...`, villagers will buy it. If it says the item can't be traded, they won't.
   Good items to start with: **bread, wheat, carrots, potatoes, coal, paper, iron ingots**.
   Tip: press the **`?` button** in the shelf screen to see every item villagers will buy and its price.
4. **Wait.** It is normal for **nothing to happen for up to ~2 minutes** (see [Timing](#timing)). A villager walks over, a trade sound plays, green particles appear, and the item turns into emeralds in the right-hand slots.
5. **Collect the emeralds** from the output slots (or put a hopper underneath).

That's it. Everything below is optional.

---

## 2. The blocks

| Block | What it does | Recipe |
|---|---|---|
| **Selling Shelf** | Villagers **buy** your items and pay emeralds. 18 item slots (left), 9 emerald slots (right). | Planks ×6, Bookshelf ×2, Emerald |
| **Selling Shelf (Accounting)** | Same as the Selling Shelf, but belongs to a **shop group**. Emeralds are sent to the group's Register. Villagers who use it go on a full "shopping trip" (see section 3). | Same as Selling Shelf, with a Gold Ingot in the bottom middle |
| **Purchase Shelf** | You **buy** items from villagers. Put emeralds in, set the item you want, and villagers come and restock it. | Planks ×6, Chest ×2, Emerald |
| **Cash Register** | Collects the emeralds from all Accounting shelves in the same shop group. You can only take items out. | Smooth Stone ×8, Emerald |
| **Shop Setting Stick** | Tool for setting shop group names, chairs and standing spots. | Emerald / Gold Ingot / Stick (vertical) |

### Hoppers

- **Selling Shelves:** insert items from the **top or sides**, extract emeralds from the **bottom**.
- **Purchase Shelf:** insert emeralds from the **top or sides**, extract bought items from the **bottom**.
- **Cash Register:** extract emeralds from the **bottom** only.

### Using the Purchase Shelf

1. Put emeralds in the left slots.
2. **Hold the item you want** and **click the gold-highlighted slot**. The item is only used as a sample and is not consumed.
3. Hover over the sample slot to see the cost per item. A villager will come and fill the output slots, using your emeralds.

Buying is intentionally expensive (by default ×5 the vanilla price), so make sure there are **enough emeralds for at least one item**, or nobody will come.

---

## 3. Building a real shop (shop groups)

A **shop group** links several **Selling Shelves (Accounting)** and one or more **Cash Registers** with a shared name. A villager who visits one shelf in the group will:

1. browse **1–3 more shelves** in the same group,
2. walk to the **Register** and pay (sound + particles),
3. sit on a **chair** or stand on a **standing spot** for a short rest, eating what they bought (if you set any),
4. then leave, and come back after a cooldown.

### Step by step

1. Place **2 or more Selling Shelves (Accounting)** and **1 Cash Register**.
2. Give them the **same group name**. Choose one of these methods:
   - **Easiest:** **sneak + right-click with an empty hand** on each Accounting shelf / Register, type the name, and confirm.
   - **With the Shop Setting Stick** (faster for many blocks):
     1. Hold the stick. The action bar shows the current mode. **Sneak + right-click** to change the mode until it shows **Shop Group Setting**.
     2. **Right-click the air** to open the name screen and type the group name. The stick now remembers it.
     3. **Right-click** each Accounting shelf and Register to apply the name.
3. (Optional) **Chairs and standing spots.** With the stick still holding the group name:
   - Switch the mode to **Chair Setting** and right-click a **stair block** (or any block you want villagers to sit on).
   - Switch to **Standing Position Setting** and right-click a block. Villagers will stand **on top** of it.
   - Switch to **Delete** and right-click a chair/spot to remove it.
   While you hold the stick, the group's blocks are highlighted with particles: chairs are green, standing spots white, shelves sparkle, and registers have green/gold particles.
4. Stock the shelves and wait.

> **Note:** The Shop Setting Stick could not apply group names to shelves in v1.1 (right-clicking just opened the shelf). This is fixed in **v1.2**. On v1.1, use sneak + right-click with an empty hand instead.

---

## 4. Troubleshooting: "Villagers don't trade"

Go through this list. Most of the time it is one of the first three.

| Check | Why |
|---|---|
| **Did you wait 2–3 minutes?** | After a world loads, each villager waits a random time of up to **2 minutes** before shopping. After each shopping trip they wait **2–3 minutes** again. Villagers only look for shelves **every 10 seconds**. |
| **Is it daytime?** | **Sleeping villagers don't shop.** By default their cooldown also pauses while they sleep. Baby villagers never shop. |
| **Does the tooltip show a price?** | Only items with a price can be sold. Hover over the item in the shelf, or press **`?`** to see the list. Unknown modded items usually have no price unless you add one in the config. |
| **Is the shelf close enough?** | Villagers only look for shelves within **32 blocks** of themselves (configurable). |
| **Can villagers actually reach it?** | If a villager can't walk to within ~2 blocks of the shelf within **30 seconds**, it gives up. Avoid fences, slabs around the shelf, 1-block gaps, or placing it on a counter they can't get next to. |
| **Are the emerald slots full?** | A Selling Shelf whose output slots are full is ignored. Empty it or add a hopper. For Accounting shelves the Register also counts as space. |
| **Purchase Shelf: enough emeralds?** | It needs at least the price of **one** item in emeralds, a sample item set, and free output space. |
| **Did a villager visit but buy only part of the stack?** | That's normal. Each villager has a **budget** per visit (by default 10–30 emeralds). Large stacks get sold over several visits. |

Still nothing? Stand near the shelf for a few minutes during the day. A **bell sound with green particles** means a **shopping rush**: all nearby villagers reset their cooldowns and go shopping at once.

---

## 5. Prices & configuration

Open the config screen from **Mod Menu** (Fabric) or the **Mods list** (NeoForge), or edit `config/village_shop_system.json`.

### How prices are decided

1. **Your custom prices** in the config always take priority.
2. **Enchanted books:** based on level and rarity.
3. **Items villagers trade in vanilla:** the vanilla price is used.
4. **Food:** based on how much hunger it restores.
5. **Tools:** based on durability and mining speed.
6. **Potions:** based on effect and duration.

When you **sell**, the price is reduced by the **Selling Penalty** (default −1 emerald). For 1-emerald items, more items are needed instead. When you **buy**, the price is multiplied by the **Purchase Price Multiplier** (default ×5).

### Changing the price of a specific item

You can change prices for any item, including modded ones, in `config/village_shop_system.json`:

```json
"customSellPrices": {
  "minecraft:cake": [1, 1],
  "minecraft:diamond": [8, 1],
  "farmersdelight:tomato": [1, 12]
},
"customBuyPrices": {
  "minecraft:name_tag": 20
},
"enchantedBookBuyPrices": {
  "minecraft:mending": 125
}
```

- `customSellPrices`: `"item id": [emeralds, item count]`. For example, `[1, 12]` means 12 tomatoes sell for 1 emerald.
- `customBuyPrices`: `"item id": emeralds per item` (for the Purchase Shelf).
- Custom prices ignore the selling penalty and the purchase multiplier.

### Using another currency (v1.2+)

Set **`currencyItem`** to any item ID, for example a coin from another mod:

```json
"currencyItem": "examplemod:gold_coin"
```

Shelves and Registers will then pay and accept that item instead of emeralds, 1:1. **Use the same value on the server and on every client.**

### Other useful settings

| Setting | Default | Meaning |
|---|---|---|
| `villagerSearchRange` | 32 | How far villagers look for shelves (blocks) |
| `villagerShoppingCooldownTicks` | 2400 | Base wait between shopping trips (20 ticks = 1 second) |
| `minShelvesToVisit` / `maxShelvesToVisit` | 1 / 3 | Extra shelves visited per trip in a shop group |
| `villagerBudgetPerTrade` | 20 | Average emeralds a villager spends per visit (50%–150%) |
| `shoppingRushChance` | 0.005 | Chance that a trip triggers a shopping rush |
| `pauseCooldownWhileSleeping` | true | Villagers don't count down their cooldown while asleep |

---

## FAQ

**Do I need to build a whole shopping district?**
No. A single Selling Shelf near villagers works. Shop groups, registers and chairs are only for a more lively shop.

**Can I set prices myself?**
Yes. See [Changing the price of a specific item](#changing-the-price-of-a-specific-item).

**Can I use coins from another mod instead of emeralds?**
Yes, from v1.2. See [Using another currency](#using-another-currency-v12).

**Do villagers level up?**
Yes. Each successful trade gives the villager trade XP (configurable), so they can level up their profession.

**Can I use chairs from furniture mods?**
The Chair Setting mode accepts any block, so you can register a furniture-mod chair as a seat. Villagers are seated slightly above the bottom of the block.

**Does it work with MCA Reborn villagers?**
Not tested. The mod works with vanilla `Villager` entities. Villagers from other mods may or may not work, depending on how they are implemented.

**Which versions are supported?**
Minecraft 1.21.1 (Fabric / NeoForge). A 1.20.1 version (Fabric / Forge) is in development.

<a name="timing"></a>
### Timing cheat sheet
- First shopping after world load: **0–2 min** (random per villager)
- Between shopping trips: **2–3 min**
- Shelf search: every **10 s**, within **32 blocks**
- Gives up if the shelf isn't reached in **30 s**
