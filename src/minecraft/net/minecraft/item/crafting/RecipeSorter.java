package net.minecraft.item.crafting;

import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.Comparator;

class RecipeSorter implements Comparator
{
    final CraftingManager craftingManager;

    RecipeSorter(CraftingManager par1CraftingManager)
    {
        this.craftingManager = par1CraftingManager;
    }

    public int compareRecipes(IRecipe par1IRecipe, IRecipe par2IRecipe)
    {
        // MCPC+ start - add mod recipe support
        //return par1IRecipe instanceof ShapelessRecipes && par2IRecipe instanceof ShapedRecipes ? 1 : (par2IRecipe instanceof ShapelessRecipes && par1IRecipe instanceof ShapedRecipes ? -1 : (par2IRecipe.getRecipeSize() < par1IRecipe.getRecipeSize() ? -1 : (par2IRecipe.getRecipeSize() > par1IRecipe.getRecipeSize() ? 1 : 0)));
        if ((par1IRecipe instanceof ShapelessRecipes || par1IRecipe instanceof ShapelessOreRecipe) && (par2IRecipe instanceof ShapedRecipes || par2IRecipe instanceof ShapedOreRecipe)) {
            return 1;
        } else if ((par2IRecipe instanceof ShapelessRecipes || par2IRecipe instanceof ShapelessOreRecipe) && (par1IRecipe instanceof ShapedRecipes || par1IRecipe instanceof ShapedOreRecipe)) {
            return -1;
        } else if (par2IRecipe.getRecipeSize() < par1IRecipe.getRecipeSize()) {
            return -1;
        } else if (par2IRecipe.getRecipeSize() > par1IRecipe.getRecipeSize()) {
            return 1;
        } else {
            // MCPC+ - arbitrary comparison key to ensure transitivity requirement satisfied
            return par2IRecipe.toString().compareTo(par1IRecipe.toString());
        }
        // MCPC+ end
    }

    public int compare(Object par1Obj, Object par2Obj)
    {
        return this.compareRecipes((IRecipe)par1Obj, (IRecipe)par2Obj);
    }
}
