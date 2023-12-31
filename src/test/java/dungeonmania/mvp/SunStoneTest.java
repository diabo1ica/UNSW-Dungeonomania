package dungeonmania.mvp;

import dungeonmania.DungeonManiaController;
import dungeonmania.response.models.DungeonResponse;
// import dungeonmania.response.models.BattleResponse;
// import dungeonmania.response.models.RoundResponse;
import dungeonmania.util.Direction;
import dungeonmania.util.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SunStoneTest {
    @Test
    @DisplayName("Test picking up a SunStone removes the bomb from the map and adds the bomb to the inventory")
    public void pickUp() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_sunStoneTest_pickUp", "c_sunStoneTest_pickUp");
        assertEquals(1, TestUtils.getEntities(res, "sun_stone").size());
        assertEquals(0, TestUtils.getInventory(res, "sun_stone").size());

        // Pick up SunStone
        res = dmc.tick(Direction.RIGHT);
        assertEquals(0, TestUtils.getEntities(res, "sun_stone").size());
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());
    }

    @Test
    @DisplayName("Test player can use a SunStone to open and walk through a door, SunStone remains in inventory")
    public void openDoor() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_sunStoneTest_pickUp", "c_sunStoneTest_pickUp");

        // Pick up SunStone
        res = dmc.tick(Direction.RIGHT);
        assertEquals(0, TestUtils.getEntities(res, "sun_stone").size());
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());

        // Try to open a door and walk through
        Position pos = TestUtils.getEntities(res, "player").get(0).getPosition();
        res = dmc.tick(Direction.RIGHT);
        res = dmc.tick(Direction.RIGHT);
        res = dmc.tick(Direction.RIGHT);
        assertNotEquals(pos, TestUtils.getEntities(res, "player").get(0).getPosition());

        // Sun stone is still inside inventory even though has been used to open door
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());
    }

    @Test
    @DisplayName("Test player can use a SunStone to build shield, SunStone remains in inventory")
    public void buildShieldUsingSunStone() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_sunStoneTest_buildShield", "c_sunStoneTest_buildShield");

        assertEquals(0, TestUtils.getInventory(res, "wood").size());
        assertEquals(0, TestUtils.getInventory(res, "sun_stone").size());

        // Pick up Wood x2
        res = dmc.tick(Direction.RIGHT);
        res = dmc.tick(Direction.RIGHT);
        assertEquals(2, TestUtils.getInventory(res, "wood").size());

        // Pick up SunStone
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());

        // Build Shield
        assertEquals(0, TestUtils.getInventory(res, "shield").size());
        res = assertDoesNotThrow(() -> dmc.build("shield"));
        assertEquals(1, TestUtils.getInventory(res, "shield").size());

        // Woods are used up, but Sun Stone remains
        assertEquals(0, TestUtils.getInventory(res, "wood").size());
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());
    }

    @Test
    @DisplayName("Test Sun Stone contributes to Treasure goal")
    public void sunStoneTreasureGoal() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_sunStoneTest_treasureGoal", "c_sunStoneTest_treasureGoal");

        // move player to right
        res = dmc.tick(Direction.RIGHT);

        // assert goal not met
        assertTrue(TestUtils.getGoals(res).contains(":treasure"));

        // collect treasure 1
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "treasure").size());

        // assert goal not met
        assertTrue(TestUtils.getGoals(res).contains(":treasure"));

        // collect treasure 2
        res = dmc.tick(Direction.RIGHT);
        assertEquals(2, TestUtils.getInventory(res, "treasure").size());

        // assert goal not met
        assertTrue(TestUtils.getGoals(res).contains(":treasure"));

        // collect sun stone
        res = dmc.tick(Direction.RIGHT);
        assertEquals(2, TestUtils.getInventory(res, "treasure").size());
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());

        // assert goal met
        assertEquals("", TestUtils.getGoals(res));
    }

    @Test
    @DisplayName("Test building Sceptre using 1 sun stone, 1 wood, and 1 treasure")
    public void buildSpectre1() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_sunStoneTest_buildSceptre", "c_sunStoneTest_buildSceptre");

        // move player to right (collects sun stone)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());

        // move player to right (collects wood)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "wood").size());

        // move player to right (collects treasure)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "treasure").size());

        // build sceptre
        assertEquals(0, TestUtils.getInventory(res, "sceptre").size());
        res = assertDoesNotThrow(() -> dmc.build("sceptre"));
        assertEquals(1, TestUtils.getInventory(res, "sceptre").size());
    }

    @Test
    @DisplayName("Test building Sceptre using 1 sun stone, 2 arrows, and 1 treasure")
    public void buildSpectre2() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_sunStoneTest_buildSceptre2", "c_sunStoneTest_buildSceptre");

        // move player to right (collects sun stone)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());

        // move player to right (collects arrow 1)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "arrow").size());

        // move player to right (collects arrow 2)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(2, TestUtils.getInventory(res, "arrow").size());

        // move player to right (collects treasure)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "treasure").size());

        // build sceptre
        assertEquals(0, TestUtils.getInventory(res, "sceptre").size());
        res = assertDoesNotThrow(() -> dmc.build("sceptre"));
        assertEquals(1, TestUtils.getInventory(res, "sceptre").size());
    }

    @Test
    @DisplayName("Test building Sceptre using 2 sun stones and 1 wood")
    public void buildSpectre3() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_sunStoneTest_buildSceptre3", "c_sunStoneTest_buildSceptre");

        // move player to right (collects sun stone 1)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());

        // move player to right (collects sun stone 2)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(2, TestUtils.getInventory(res, "sun_stone").size());

        // move player to right (collects wood)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "wood").size());

        // build sceptre
        assertEquals(0, TestUtils.getInventory(res, "sceptre").size());
        res = assertDoesNotThrow(() -> dmc.build("sceptre"));
        assertEquals(1, TestUtils.getInventory(res, "sceptre").size());

        // one sun stone is used up as per requirement to build sceptre
        // the other sun stone still remains inside inventory (replacing other ingredient)
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());
    }

    @Test
    @DisplayName("Test building Sceptre using 3 sun stone")
    public void buildSpectre4() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_sunStoneTest_buildSceptre4", "c_sunStoneTest_buildSceptre");

        // move player to right (collects sun stone 1)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());

        // move player to right (collects sun stone 2)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(2, TestUtils.getInventory(res, "sun_stone").size());

        // move player to right (collects wood)
        res = dmc.tick(Direction.RIGHT);
        assertEquals(3, TestUtils.getInventory(res, "sun_stone").size());

        // build sceptre
        assertEquals(0, TestUtils.getInventory(res, "sceptre").size());
        res = assertDoesNotThrow(() -> dmc.build("sceptre"));
        assertEquals(1, TestUtils.getInventory(res, "sceptre").size());

        // one sun stone is used up as per requirement to build sceptre
        // the other sun stone still remains inside inventory (replacing other ingredient)
        assertEquals(2, TestUtils.getInventory(res, "sun_stone").size());
    }

    @Test
    @DisplayName("Test Mind control duration")
    public void mindControl() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_sunStoneTest_use_scepter",
        "c_sunStoneTest_use_scepter");

        res = dmc.tick(Direction.RIGHT);
        res = dmc.tick(Direction.RIGHT);
        res = dmc.tick(Direction.RIGHT);
        res = assertDoesNotThrow(() -> dmc.build("sceptre"));

        // Mind control merc
        String mercId = TestUtils.getEntities(res, "mercenary").get(0).getId();
        res = assertDoesNotThrow(() -> dmc.interact(mercId));

        // Player move to merc assert not battle
        Position playerPos = TestUtils.getEntities(res, "player").get(0).getPosition();
        Position mercPos = TestUtils.getEntities(res, "mercenary").get(0).getPosition();
        res = dmc.tick(Direction.RIGHT);
        // Player and merc switch position
        assertEquals(mercPos, TestUtils.getEntities(res, "player").get(0).getPosition());
        assertEquals(playerPos, TestUtils.getEntities(res, "mercenary").get(0).getPosition());

        // Mind control duration up, player and merc battle on next tick
        res = dmc.tick(Direction.LEFT);
        assertEquals(0, TestUtils.getEntities(res, "mercenary").size());
    }

    @Test
    @DisplayName("Test Midnight armor buff")
    public void midnightBuff() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        String config = "c_sunStoneTest_midnight_buff";
        DungeonResponse res = dmc.newGame("d_sunStoneTest_midnight_no_buff", config);
        res = dmc.tick(Direction.RIGHT);
        res = dmc.tick(Direction.RIGHT);
        res = dmc.tick(Direction.RIGHT);
        res = dmc.tick(Direction.RIGHT);
        assertEquals(0, TestUtils.getEntities(res, "player").size());

        // Create new game this time battle with armor assert player win
        res = dmc.newGame("d_sunStoneTest_midnight_buff", config);
        res = dmc.tick(Direction.RIGHT);
        res = dmc.tick(Direction.RIGHT);
        res = assertDoesNotThrow(() -> dmc.build("midnight_armour"));
        assertEquals(1, TestUtils.getInventory(res, "midnight_armour").size());

        res = dmc.tick(Direction.RIGHT);
        assertEquals(0, TestUtils.getEntities(res, "mercenary").size());
    }

    @Test
    @DisplayName("Test cannot build Midnight armor when there is a zombie toast")
    public void midnightZombie() {
        DungeonManiaController dmc;
        dmc = new DungeonManiaController();
        String config = "c_sunStoneTest_midnight_buff";
        DungeonResponse res = dmc.newGame("d_sunStoneTest_no_zombie", config);
        // picks up sword
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "sword").size());

        // picks up sun_stone
        res = dmc.tick(Direction.RIGHT);
        assertEquals(1, TestUtils.getInventory(res, "sun_stone").size());

        // cannot build midnight_armour when there is a zombie
        assertEquals(0, TestUtils.getEntities(res, "midnight_armour").size());
        res = assertDoesNotThrow(() -> dmc.build("midnight_armour"));
        assertEquals(1, TestUtils.getInventory(res, "midnight_armour").size());

    }

}
