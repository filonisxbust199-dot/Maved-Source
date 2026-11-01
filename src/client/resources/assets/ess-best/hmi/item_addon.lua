-- Edit this file in config/ess-best/hmi/item_addon.lua, then save.
-- Changes reload automatically while HMI is active; no resource pack needed.
local spinOnSwitch = true
local swingEnabled = false
local duration = 0.55 -- seconds; keep greater than zero
local turns = 1       -- 1 = 360 degrees, 2 = 720 degrees
local swingAngle = -25
local swingReach = 0.15

duration = math.max(0.05, duration)
local key = context.mainHand and "ess_local_spin_main" or "ess_local_spin_off"
local changed
if context.mainHand then changed = context.mainHandSwitchEvent
else changed = context.offHandSwitchEvent end
local elapsed = registry:getOrDefault(key, duration)
if changed then elapsed = 0 end
elapsed = math.min(duration, elapsed + context.deltaTime)
registry:put(key, elapsed)

if not I:isEmpty(context.item) and not P:isUsingItem(context.player)
    and not I:isOf(context.item, Items:get("minecraft:filled_map")) then
    if spinOnSwitch and elapsed < duration then
        local t = elapsed / duration
        local smooth = t * t * t * (t * (t * 6 - 15) + 10)
        M:rotateY(context.matrices, 360 * turns * smooth)
    end
    if swingEnabled then
        local t = math.max(0, math.min(1, context.swingProgress))
        local pulse = math.sin(math.pi * t) ^ 2
        M:moveZ(context.matrices, -swingReach * pulse)
        M:rotateX(context.matrices, swingAngle * pulse)
    end
end
