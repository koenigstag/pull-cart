# Pull Cart (Forge 1.20.1)

A wooden cart you pull by hand: the cart TFC.SU played with on 1.7.10, which was **MC+ Cart** by andykuo1. This is a
Minecraft 1.20.1 Forge port of [MC+ Cart, MC+ Wheel and MC+ Loom](https://github.com/andykuo1/mcplus-mods) (GPL-2.0),
so it is GPL-2.0 too (see `LICENSE`). The model geometry and textures are MC+ Cart's and MC+ Loom's.

## How it works

| Do | What happens |
|---|---|
| Right-click a block with the **Wooden Cart** | Sets the cart down there, facing the way you look |
| Right-click the cart | You pull it (it follows you, climbing one-block steps); again to let go |
| Pull it while riding a horse | The horse pulls it |
| Right-click it holding a chest, crafting table, ender chest, furnace or jukebox | Loads that block onto it |
| Shift + right-click it | Uses the block: chest (27 slots), crafting, your ender chest, a furnace that smelts while you travel, or a jukebox (puts in the disc you hold, or gives it back) |
| Right-click it while you lead a small animal | Puts the animal in (taller than 1.5 blocks doesn't fit); Shift + right-click lets it out |
| Hit it | Breaks it: it drops itself and its load |
| Shift + hit it | Takes the block off (it drops with its contents) |

- It lets go when it falls more than 6 blocks behind (stuck), and it breaks under water, as MC+ Cart's did.
- While the jukebox plays, the cart stays where it is.
- Any chest or crafting table works, TerraFirmaCraft's wooden ones included.

## Wooden Loom

MC+ Loom's loom: it spins wool into string, like a furnace with two fuel slots.

- Right-click it to open it. Wool goes on top, and a spindle goes in each of the two slots under it.
- A block of wool (any colour) becomes 4 string in 20 seconds.
- Spindles: a stick (10 seconds), a blaze rod (40 seconds) or shears (160 seconds). Both slots need one. The loom
  uses one from each slot at a time and runs for as long as the two last together. Two sticks spin one block of wool.
- While it spins, the wheel turns and puffs of smoke rise, as in MC+ Loom. Taking string out gives a little experience.
- Hoppers put wool in from the top and spindles in from the sides, and take string out from the bottom. A comparator
  reads how full it is. When broken, it drops itself and what it holds.

## Crafting

- **Wooden Wheel**: 8 sticks around a slime ball (or TFC glue).
- **Wooden Cart**: planks `P P` / `PPP`, and two wheels `W W` underneath.
- **Wooden Loom**: planks and a wheel `PW` / `PPP` (or mirrored).

## TerraFirmaCraft

- The cart is a huge, very heavy item, like an anvil: no TFC chest, vessel or barrel takes it, and it can't be put
  on the ground with V (only set down as a cart). Like an anvil, carrying one exhausts you and two overburden you.
  The wheel is large and medium. The loom is large and very heavy, like TFC's own loom.
- The loom also takes TFC's twigs and metal shears as spindles.
- A chest on the cart holding more than 4 huge, very heavy items (anvils, carts and the like) is too heavy to pull
  (`tfc.maxHeavyItems` in `config/pull_cart-common.toml`).

## Config

`config/pull_cart-common.toml`: `pulling.followGap` (blocks between you and the cart, 1.2), `pulling.letGoDistance`
(6), `tfc.maxHeavyItems` (4).

## Build

```
./gradlew build
```

JDK 17. The jar is in `build/libs/`; it is needed on both the server and the client.

## License

GPL-2.0, the license of the original (see `LICENSE`).

- MC+ Cart, MC+ Wheel and MC+ Loom: Copyright (C) 2014-2015 andykuo1, https://github.com/andykuo1/mcplus-mods
- Forge 1.20.1 port: Copyright (C) 2026 koenigstag
