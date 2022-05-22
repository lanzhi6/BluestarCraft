package me.lanzhi.bluestarcraft;

import de.tr7zw.nbtapi.NBTItem;
import me.lanzhi.bluestarapi.Api.config.AutoSerialize;
import me.lanzhi.bluestarapi.Api.config.YamlFile;
import me.lanzhi.bluestarcraft.api.BluestarCraft;
import me.lanzhi.bluestarcraft.api.recipe.Recipe;
import me.lanzhi.bluestarcraft.api.recipe.ShapelessRecipe;
import me.lanzhi.bluestarcraft.api.recipe.matcher.ExactMatcher;
import me.lanzhi.bluestarcraft.api.recipe.matcher.MaterialMatcher;
import me.lanzhi.bluestarcraft.commands.craftCommand;
import me.lanzhi.bluestarcraft.listeners.CraftGuiListener;
import me.lanzhi.bluestarcraft.listeners.CraftTableListener;
import me.lanzhi.bluestarcraft.listeners.RegisterListener;
import me.lanzhi.bluestarcraft.managers.BluestarCraftManager;
import me.lanzhi.bluestarcraft.managers.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class BluestarCraftPlugin extends JavaPlugin
{
    private BluestarCraftManager bluestarCraftManager;
    private YamlFile recipes;
    private YamlFile data;

    @Override
    public void onEnable()
    {
        this.bluestarCraftManager=new BluestarCraftManager(this);
        getCommand("bluestarcraft").setExecutor(new craftCommand(this));
        Bukkit.getPluginManager().registerEvents(new CraftGuiListener(this),this);
        Bukkit.getPluginManager().registerEvents(new CraftTableListener(this),this);
        Bukkit.getPluginManager().registerEvents(new RegisterListener(this),this);
        BluestarCraft.setPlugin(this);
        NBTItem nbtItem=new NBTItem(new ItemStack(Material.CRAFTING_TABLE));
        nbtItem.setBoolean("BluestarCraft.Table",true);
        ShapedRecipe recipe=new ShapedRecipe(new NamespacedKey(this,"crafttable"),nbtItem.getItem());
        recipe.shape(" a ","aba"," a ");
        recipe.setIngredient('a',Material.DIAMOND);
        recipe.setIngredient('b',Material.CRAFTING_TABLE);
        Bukkit.addRecipe(recipe);
        AutoSerialize.registerClass(ExactMatcher.class);
        AutoSerialize.registerClass(MaterialMatcher.class);
        AutoSerialize.registerClass(ShapelessRecipe.class);
        AutoSerialize.registerClass(me.lanzhi.bluestarcraft.api.recipe.ShapedRecipe.class);
        this.saveResource("recipe.yml",false);
        data=YamlFile.loadYamlFile(new File(getDataFolder(),"data.yml"));
        List<?>list=data.getList("recipe");
        if (list==null)
        {
            list=new ArrayList<>();
        }
        for (Object o:list)
        {
            if (!(o instanceof Recipe))
            {
                continue;
            }
            bluestarCraftManager.addRecipe((Recipe) o);
        }
        recipes=YamlFile.loadYamlFile(new File(getDataFolder(),"recipe.yml"));
        loadRecipes();
        new Metrics(this);
        System.out.println("BluestarCraft已加载");
    }

    @Override
    public void onDisable()
    {
        for (Inventory inventory:bluestarCraftManager.getInventories())
        {
            for (HumanEntity entity:inventory.getViewers())
            {
                entity.closeInventory();
                bluestarCraftManager.closeCraft(entity,inventory);
            }
        }
        for (Inventory inventory:bluestarCraftManager.getRegisters())
        {
            for (HumanEntity entity:inventory.getViewers())
            {
                entity.closeInventory();
            }
        }
        Bukkit.removeRecipe(new NamespacedKey(this,"crafttable"));
        List<Recipe>recipes=new ArrayList<>();
        for (Recipe recipe:bluestarCraftManager.getRecipes())
        {
            if (recipe.needSave())
            {
                recipes.add(recipe);
            }
        }
        data.set("recipe",recipes);
        data.save();
        System.out.println("BluestarCraft已卸载");
    }

    public BluestarCraftManager getBluestarCraftManager()
    {
        return bluestarCraftManager;
    }

    private void loadRecipes()
    {
        ConfigurationSection shaped=recipes.getConfigurationSection("shaped");
        ConfigurationSection shapeless=recipes.getConfigurationSection("shapeless");
        if (shaped!=null)
        {
            for (String key: shaped.getKeys(false))
            {
                me.lanzhi.bluestarcraft.api.recipe.ShapedRecipe recipe=new me.lanzhi.bluestarcraft.api.recipe.ShapedRecipe(
                        key,
                        new ItemStack(Material.matchMaterial(shaped.getString(key+".result")),shaped.getInt(key+".amount")),
                        shaped.getStringList(key+".shape").toArray(new String[0])
                );
                for (String s: shaped.getConfigurationSection(key+".ingredient").getKeys(false))
                {
                    recipe.setIngredient(s.charAt(0),Material.matchMaterial(shaped.getString(key+".ingredient."+s)));
                }
                bluestarCraftManager.addRecipe(recipe);
            }
        }
        if (shapeless!=null)
        {
            for (String key: shapeless.getKeys(false))
            {
                ShapelessRecipe recipe=new ShapelessRecipe(key,
                                                           new ItemStack(Material.matchMaterial(shapeless.getString(key+".result")),
                                                                         shapeless.getInt(key+".amount")
                                                           )
                );
                for (String s: shapeless.getConfigurationSection(key+".ingredient").getKeys(false))
                {
                    recipe.addMaterial(Material.matchMaterial(s),shapeless.getInt(key+".ingredient."+s));
                }
                bluestarCraftManager.addRecipe(recipe);
            }
        }
    }
}