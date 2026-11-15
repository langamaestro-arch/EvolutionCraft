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
