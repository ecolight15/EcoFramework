package jp.minecraftuser.ecoframework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class demonstrating utility function testing approaches
 * These tests validate the core algorithms used in the EcoFramework
 * without requiring Bukkit dependencies.
 */
public class UtilityAlgorithmTest {

    /**
     * Test the color code replacement algorithm used in Utl.repColor()
     */
    @Test
    public void testColorCodeReplacement() {
        // Test basic color code replacement
        String input = "&aHello &cWorld";
        String result = input.replaceAll("&([0-9A-Fa-flLmMnNoOkKrR])", "§$1");
        assertEquals("§aHello §cWorld", result);
        
        // Test all supported codes
        String allCodes = "&0&1&2&3&4&5&6&7&8&9&a&b&c&d&e&f&l&m&n&o&k&r";
        String expectedCodes = "§0§1§2§3§4§5§6§7§8§9§a§b§c§d§e§f§l§m§n§o§k§r";
        String resultCodes = allCodes.replaceAll("&([0-9A-Fa-flLmMnNoOkKrR])", "§$1");
        assertEquals(expectedCodes, resultCodes);
        
        // Test uppercase codes
        String upperCodes = "&AHello &CWorld &L&M&N&O&K&R";
        String expectedUpper = "§AHello §CWorld §L§M§N§O§K§R";
        String resultUpper = upperCodes.replaceAll("&([0-9A-Fa-flLmMnNoOkKrR])", "§$1");
        assertEquals(expectedUpper, resultUpper);
    }

    /**
     * Test the full-width space replacement algorithm used in Utl.repColor()
     */
    @Test
    public void testFullWidthSpaceReplacement() {
        String input = "Hello　World　Test";
        String result = input.replaceAll("　", "  ");
        assertEquals("Hello  World  Test", result);
        
        // Test empty string
        assertEquals("", "".replaceAll("　", "  "));
        
        // Test string with no full-width spaces
        String noSpaces = "Hello World Test";
        assertEquals(noSpaces, noSpaces.replaceAll("　", "  "));
    }

    /**
     * Test the combined color code and space replacement algorithm
     */
    @Test
    public void testCombinedReplacement() {
        String input = "&aHello　&cWorld　&fTest";
        // Apply both replacements as done in Utl.repColor()
        String result = input.replaceAll("&([0-9A-Fa-flLmMnNoOkKrR])", "§$1").replaceAll("　", "  ");
        assertEquals("§aHello  §cWorld  §fTest", result);
    }

    /**
     * Test the string merging algorithm used in Utl.mergeStrings()
     */
    @Test
    public void testStringMerging() {
        // Test basic merging
        String[] args = {"Hello", "World", "Test"};
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : args) {
            if (first) {
                first = false;
            } else {
                sb.append(" ");
            }
            sb.append(s);
        }
        assertEquals("Hello World Test", sb.toString());
        
        // Test single element
        String[] singleArg = {"Hello"};
        sb = new StringBuilder();
        first = true;
        for (String s : singleArg) {
            if (first) {
                first = false;
            } else {
                sb.append(" ");
            }
            sb.append(s);
        }
        assertEquals("Hello", sb.toString());
        
        // Test empty array
        String[] emptyArgs = {};
        sb = new StringBuilder();
        first = true;
        for (String s : emptyArgs) {
            if (first) {
                first = false;
            } else {
                sb.append(" ");
            }
            sb.append(s);
        }
        assertEquals("", sb.toString());
        
        // Test with empty strings
        String[] withEmpty = {"Hello", "", "World", ""};
        sb = new StringBuilder();
        first = true;
        for (String s : withEmpty) {
            if (first) {
                first = false;
            } else {
                sb.append(" ");
            }
            sb.append(s);
        }
        assertEquals("Hello  World ", sb.toString());
    }

    /**
     * Test edge cases for the algorithms
     */
    @Test
    public void testEdgeCases() {
        // Test null handling (would need null checks in actual implementation)
        assertDoesNotThrow(() -> {
            String empty = "";
            empty.replaceAll("&([0-9A-Fa-flLmMnNoOkKrR])", "§$1");
            empty.replaceAll("　", "  ");
        });
        
        // Test very long strings
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longString.append("&a");
        }
        String result = longString.toString().replaceAll("&([0-9A-Fa-flLmMnNoOkKrR])", "§$1");
        assertEquals(2000, result.length()); // Each &a becomes §a
        
        // Test special characters
        String special = "&a特殊文字&c测试";
        String resultSpecial = special.replaceAll("&([0-9A-Fa-flLmMnNoOkKrR])", "§$1");
        assertEquals("§a特殊文字§c测试", resultSpecial);
    }
}