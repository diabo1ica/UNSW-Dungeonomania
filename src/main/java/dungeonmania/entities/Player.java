package dungeonmania.entities;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import dungeonmania.battles.BattleStatistics;
import dungeonmania.battles.Battleable;
import dungeonmania.entities.buildables.MidnightArmour;
import dungeonmania.entities.collectables.Bomb;
import dungeonmania.entities.collectables.Treasure;
import dungeonmania.entities.collectables.SunStone;
import dungeonmania.entities.collectables.potions.Potion;
import dungeonmania.entities.enemies.Enemy;
import dungeonmania.entities.enemies.Mercenary;
import dungeonmania.entities.inventory.Inventory;
import dungeonmania.entities.inventory.InventoryItem;
import dungeonmania.entities.playerState.BaseState;
import dungeonmania.entities.playerState.PotionState;
import dungeonmania.entities.playerState.PotionStateChanger;
import dungeonmania.map.GameMap;
import dungeonmania.util.Direction;
import dungeonmania.util.Position;

public class Player extends Entity implements Battleable, OverlapBehaviour {
    public static final double DEFAULT_ATTACK = 5.0;
    public static final double DEFAULT_HEALTH = 5.0;
    private BattleStatistics battleStatistics;
    private Inventory inventory;
    private Queue<Potion> queue = new LinkedList<>();
    private Potion inEffective = null;
    private int nextTrigger = 0;

    private int collectedTreasureCount = 0;
    private int collectedSunStoneCount = 0;

    private PotionState state;
    private int killCount = 0;

    public Player(Position position, double health, double attack) {
        super(position);
        battleStatistics = new BattleStatistics(
                health,
                attack,
                0,
                BattleStatistics.DEFAULT_DAMAGE_MAGNIFIER,
                BattleStatistics.DEFAULT_PLAYER_DAMAGE_REDUCER);
        inventory = new Inventory();
        state = new BaseState();
    }

    public int getCollectedTreasureCount() {
        return collectedTreasureCount;
    }

    public int getCollectedSunStoneCount() {
        return collectedSunStoneCount;
    }

    public boolean hasWeapon() {
        return inventory.hasWeapon();
    }

    public BattleItem getWeapon() {
        return inventory.getWeapon();
    }

    public List<BattleItem> getBattleItems() {
        return inventory.getEntities(BattleItem.class);
    }

    public List<String> getBuildables(GameMap map) {
        return inventory.getBuildables(map);
    }

    public boolean build(String entity, EntityFactory factory) {
        InventoryItem item = inventory.checkBuildCriteria(this, entity, factory);
        if (item == null) return false;

        // if inventory contains midnight_armour then
        if (item instanceof MidnightArmour) {
            MidnightArmour itemMidnightArmour = (MidnightArmour) item;
            this.battleStatistics = BattleStatistics.applyBuff(getBattleStatistics(), new BattleStatistics(0,
                    itemMidnightArmour.getAttackBuff(), itemMidnightArmour.getDefenceBuff(), 1, 1));
        }
        return inventory.add(item);
    }

    public void move(GameMap map, Direction direction) {
        this.setFacing(direction);
        map.moveTo(this, Position.translateBy(this.getPosition(), direction));
    }

    @Override
    public void onOverlap(GameMap map, Entity entity) {
        if (entity instanceof Enemy) {
            if (entity instanceof Mercenary) {
                if (((Mercenary) entity).isAllied()) return;
            }
            map.getGame().battle(this, (Enemy) entity);
        }
    }

    @Override
    public boolean canMoveOnto(GameMap map, Entity entity) {
        return true;
    }

    public Entity getEntity(String itemUsedId) {
        return inventory.getEntity(itemUsedId);
    }

    public boolean pickUp(Entity item) {
        if (item instanceof Treasure) {
            collectedTreasureCount++;
        }
        if (item instanceof SunStone) {
            collectedSunStoneCount++;
        }
        return inventory.add((InventoryItem) item);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Potion getEffectivePotion() {
        return inEffective;
    }

    public <T extends InventoryItem> void use(Class<T> itemType) {
        T item = inventory.getFirst(itemType);
        if (item != null) inventory.remove(item);
    }

    public void use(Bomb bomb, GameMap map) {
        inventory.remove(bomb);
        bomb.onPutDown(map, getPosition());
    }

    public void triggerNext(int currentTick) {
        if (queue.isEmpty()) {
            inEffective = null;
            state = PotionStateChanger.transition(inEffective);
            return;
        } else {
            inEffective = queue.remove();
            state = PotionStateChanger.transition(inEffective);
        }
        nextTrigger = currentTick + inEffective.getDuration();
    }

    public void use(Potion potion, int tick) {
        inventory.remove(potion);
        queue.add(potion);
        if (inEffective == null) {
            triggerNext(tick);
        }
    }

    public void onTick(int tick) {
        if (inEffective == null || tick == nextTrigger) {
            triggerNext(tick);
        }
    }

    public void remove(InventoryItem item) {
        inventory.remove(item);
    }

    @Override
    public BattleStatistics getBattleStatistics() {
        return battleStatistics;
    }

    public double getHealth() {
        return battleStatistics.getHealth();
    }

    public void setHealth(double health) {
        battleStatistics.setHealth(health);
    }

    public <T extends InventoryItem> int countEntityOfType(Class<T> itemType) {
        return inventory.count(itemType);
    }

    // Apply potion state as buff
    public BattleStatistics applyBuff(BattleStatistics origin) {
        return state.applyState(origin);
    }

    public void killCountIncrease() {
        killCount++;
    }

    public int getKillCount() {
        return killCount;
    }
}
