package com.example;

import static org.junit.Assert.*;

import com.example.util.MathEvaluator;
import org.junit.Test;

public class MultiFloatUnitTest {

    @Test
    public void calculator_basicAndScientificOperations_evaluateCorrectly() {
        assertEquals("14", MathEvaluator.evaluateFormatted("2 + 3 × 4"));
        assertEquals("5", MathEvaluator.evaluateFormatted("sqrt(25)"));
        assertEquals("8", MathEvaluator.evaluateFormatted("2^3"));
        assertEquals("1", MathEvaluator.evaluateFormatted("sin(90)"));
        assertEquals("2", MathEvaluator.evaluateFormatted("log(100)"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void calculator_divideByZero_throws() {
        MathEvaluator.evaluateFormatted("10 ÷ 0");
    }

    @Test
    public void drawerTools_filterAndOrderWithFavoritesFirst() {
        java.util.Set<String> favs = new java.util.HashSet<>();
        favs.add("todo");
        favs.add("ai_assistant");

        java.util.List<com.example.model.ToolType> all =
                com.example.model.ToolType.getOrderedDrawerTools(com.example.model.ToolType.DrawerFilter.ALL, favs);
        assertEquals(com.example.model.ToolType.values().length, all.size());
        assertTrue(favs.contains(all.get(0).id));
        assertTrue(favs.contains(all.get(1).id));

        java.util.List<com.example.model.ToolType> prod =
                com.example.model.ToolType.getOrderedDrawerTools(com.example.model.ToolType.DrawerFilter.PRODUCTIVITY, favs);
        assertEquals(4, prod.size());
        assertEquals(com.example.model.ToolType.TODO, prod.get(0));
        for (com.example.model.ToolType t : prod) {
            assertTrue(t.isProductivity());
        }

        java.util.List<com.example.model.ToolType> onlyFavs =
                com.example.model.ToolType.getOrderedDrawerTools(com.example.model.ToolType.DrawerFilter.FAVORITES, favs);
        assertEquals(2, onlyFavs.size());
    }
}
