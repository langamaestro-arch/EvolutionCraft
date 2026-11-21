package es.evolutioncraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Restricciones por resultado de receta, derivadas de los JSON de:
 * sophisticatedbackpacks 3.26.18.2236 y networkstorage 1.0.2.
 *
 * Un resultado conserva la era mayor de los materiales usados para obtenerlo,
 * incluyendo mejoras encadenadas. Se aplica exclusivamente a las recetas
 * de estos dos mods y no modifica sus archivos ni sus mecánicas internas.
 */
public final class RecipeEraRules {
    private RecipeEraRules(){}

    public static int requiredEra(ItemStack stack){
        if(stack.isEmpty())return 0;
        return requiredEra(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
    public static int requiredEra(String id){
        // Throwing Discs 1.0.0: materials verified against each bundled recipe.
        if(id.startsWith("throwingdiscs:")){
            return switch(id.substring("throwingdiscs:".length())){
                case "copper_throwing_disc" -> 2;
                case "iron_throwing_disc","golden_throwing_disc" -> 3;
                case "diamond_throwing_disc" -> 4;
                case "netherite_throwing_disc" -> 5;
                default -> 0;
            };
        }
        // Nicktale Gear 1.1.0 contains no bundled crafting recipe JSON.
        // Use explicit item-family tiers rather than pretending an unverified
        // ingredient cost. All special equipment is at least Iron tier.
        // Non-equipment (ammo, consumables, cosmetics) is unaffected.
        if(id.startsWith("nicktalegear:")){
            String name=id.substring("nicktalegear:".length());
            if(name.startsWith("midnight_leather_")||
               name.startsWith("ranger_leather_"))return 1;
            if(name.startsWith("royal_gold_")||
               name.startsWith("steel_guard_"))return 3;
            if(name.startsWith("arcane_royal_")||
               name.startsWith("arcane_tide_"))return 5;
            if(name.startsWith("war_"))return 4;
            return switch(name){
                case "agent_007_pistol","amos_bow","buster_sword",
                    "energy_sword","martillo_chillon","nichirin_blade",
                    "master_sword","keyblade","kingdom_key" -> 3;
                case "dark_repulser","dragon_slayer","elucidator",
                    "leviathan_axe","oathkeeper","oblivion",
                    "redhorn_stonethresher","skyward_harp",
                    "spear_of_longinus","way_to_dawn","zangetsu" -> 4;
                case "excalibur_fate","mistsplitter_reforged",
                    "primordial_jade_cutter","primordial_jade_winged_spear",
                    "staff_of_homa","ultima_weapon","wolfs_gravestone",
                    "yoru" -> 5;
                default -> 0;
            };
        }
        if(id.startsWith("networkstorage:")){
            return switch(id.substring("networkstorage:".length())){
                case "chest_connector","guide_book","network_autocrafter",
                    "network_cable","network_controller","network_memory",
                    "network_terminal","recipe_terminal" -> 3;
                default -> 0;
            };
        }
        if(!id.startsWith("sophisticatedbackpacks:"))return 0;
        String name=id.substring("sophisticatedbackpacks:".length());
        if(name.equals("backpack")||name.equals("backpack_dye"))return 0;
        if(name.equals("copper_backpack"))return 2;
        if(name.equals("netherite_backpack") ||
           name.equals("stack_upgrade_omega_tier") ||
           name.equals("stack_upgrade_tier_4") ||
           name.matches("stack_upgrade_.*_to_tier_4_conversion"))return 5;
        if(name.equals("diamond_backpack") ||
           name.startsWith("advanced_") && !name.equals("advanced_filter_upgrade") ||
           name.equals("anvil_upgrade") ||
           name.equals("inception_upgrade") ||
           name.equals("xp_pump_upgrade") ||
           name.equals("auto_blasting_upgrade") ||
           name.equals("auto_smelting_upgrade") ||
           name.equals("auto_smoking_upgrade") ||
           name.equals("stack_upgrade_tier_3") ||
           name.matches("stack_upgrade_.*_to_tier_3_conversion"))return 4;
        // Las conversiones sin nuevos diamantes heredan como mínimo el hierro
        // de sus mejoras base. Las recetas de estos upgrades contienen hierro,
        // oro o un upgrade base previamente fabricado con hierro.
        return 3;
    }
}
