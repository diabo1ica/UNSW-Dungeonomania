package dungeonmania.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import dungeonmania.Game;
import dungeonmania.entities.DestroyedBehaviour;
import dungeonmania.entities.Entity;
import dungeonmania.entities.MovedAwayBehaviour;
import dungeonmania.entities.OverlapBehaviour;
import dungeonmania.entities.Player;
import dungeonmania.entities.Portal;
import dungeonmania.entities.Subscribable;
import dungeonmania.entities.Switch;
import dungeonmania.entities.SwitchDoor;
import dungeonmania.entities.Wire;
import dungeonmania.entities.collectables.Bomb;
import dungeonmania.entities.collectables.LogicalBomb;
import dungeonmania.entities.enemies.Enemy;
import dungeonmania.entities.enemies.ZombieToastSpawner;
import dungeonmania.util.Direction;
import dungeonmania.util.Position;
import dungeonmania.entities.inventory.InventoryItem;
import dungeonmania.entities.ExplosiveItem;
import dungeonmania.entities.LightBulb;


public class GameMap implements Serializable {
    private Game game;
    private Map<Position, GraphNode> nodes = new HashMap<>();
    private Player player;

    public Map<Position, GraphNode> getGraphNode() {
        return this.nodes;
    }

    public void setMapNode(Map<Position, GraphNode> mapNode) {
        this.nodes = mapNode;
    }

    /**
     * Initialise the game map
     * 1. pair up portals
     * 2. register all movables
     * 3. register all spawners
     * 4. register bombs and switches
     * 5. more...
     */
    public void init() {
        initPairPortals();
        initRegisterMovables();
        initRegisterSpawners();
        initAllSubscribables();
    }

    private void initAllSubscribables() {
        // Init regular bombs and Switch
        initSubscribables(Bomb.class, Switch.class);

        // Init Logical Bombs with Switches and Wires
        initSubscribables(LogicalBomb.class, Switch.class);
        initSubscribables(LogicalBomb.class, Wire.class);

        // Init Switch doors with Switches and Wires
        initSubscribables(SwitchDoor.class, Switch.class);
        initSubscribables(SwitchDoor.class, Wire.class);

        // Init Light bulbs with wires and Switches
        initSubscribables(LightBulb.class, Switch.class);
        initSubscribables(LightBulb.class, Wire.class);

        // Init Wires with wires and Switches
        initSubscribables(Wire.class, Switch.class);
        initSubscribables(Wire.class, Wire.class);
    }

    private <S1 extends Entity, S2 extends Entity> void initSubscribables(Class<S1> type1, Class<S2> type2) {
        List<Subscribable> subs1 = getEntities(type1, Subscribable.class);
        List<Subscribable> subs2 = getEntities(type2, Subscribable.class);
        for (Subscribable s1 : subs1) {
            for (Subscribable s2 : subs2) {
                if (Position.isAdjacent(s1.getPosition(), s2.getPosition())
                && !s1.getPosition().equals(s2.getPosition())) {
                    s1.subscribe(s2);
                    s2.subscribe(s1);
                }
            }
        }
    }

    // Pair up portals if there's any
    private void initPairPortals() {
        Map<String, Portal> portalsMap = new HashMap<>();
        nodes.forEach((k, v) -> {
            v.getEntities().stream().filter(Portal.class::isInstance).map(Portal.class::cast).forEach(portal -> {
                String color = portal.getColor();
                if (portalsMap.containsKey(color)) {
                    portal.bind(portalsMap.get(color));
                } else {
                    portalsMap.put(color, portal);
                }
            });
        });
    }

    private void initRegisterMovables() {
        List<Enemy> enemies = getEntities(Enemy.class);
        enemies.forEach(e -> {
            game.register(() -> e.move(game), Game.AI_MOVEMENT, e.getId());
        });
    }

    private void initRegisterSpawners() {
        List<ZombieToastSpawner> zts = getEntities(ZombieToastSpawner.class);
        zts.forEach(e -> {
            game.register(() -> e.spawn(game), Game.AI_MOVEMENT, e.getId());
        });
        game.register(() -> game.getEntityFactory().spawnSpider(game), Game.AI_MOVEMENT, "spawnSpiders");
    }

    public void moveTo(Entity entity, Position position) {
        if (!canMoveTo(entity, position)) {
            return;
        }

        triggerMovingAwayEvent(entity);
        removeNode(entity);
        entity.setPosition(position);
        addEntity(entity);
        triggerOverlapEvent(entity);
    }

    public void moveTo(Entity entity, Direction direction) {
        if (!canMoveTo(entity, Position.translateBy(entity.getPosition(), direction)))
            return;
        triggerMovingAwayEvent(entity);
        removeNode(entity);
        entity.translate(direction);
        addEntity(entity);
        triggerOverlapEvent(entity);
    }

    private void triggerMovingAwayEvent(Entity entity) {
        List<Runnable> callbacks = new ArrayList<>();
        getEntities(entity.getPosition()).forEach(e -> {
            if (e != entity && e instanceof MovedAwayBehaviour) {
                MovedAwayBehaviour ent = (MovedAwayBehaviour) e;
                callbacks.add(() -> ent.onMovedAway(this, entity));
            }
        });
        callbacks.forEach(callback -> {
            callback.run();
        });
    }

    private void triggerOverlapEvent(Entity entity) {
        List<Runnable> overlapCallbacks = new ArrayList<>();
        getEntities(entity.getPosition()).forEach(e -> {
            System.out.println("woi");

            if (e != entity) {
                // only Player can collect collectables
                if (entity instanceof Player) {
                    handleOverlap(overlapCallbacks, entity, e);
                }
                // Player, Zombie, etc can interact with non-collectables
                if (e instanceof OverlapBehaviour) {
                    System.out.println("this of type " + e.getClass());
                    OverlapBehaviour ent = (OverlapBehaviour) e;
                    overlapCallbacks.add(() -> ent.onOverlap(this, entity));
                }
            }
        });
        overlapCallbacks.forEach(callback -> {
            callback.run();
        });
    }

    public void handleOverlap(List<Runnable> overlapCallbacks, Entity mover, Entity item) {
        if (item instanceof ExplosiveItem) {
            handleOverlapExplosive(overlapCallbacks, mover, item);
        } else if (item instanceof InventoryItem) {
            handleOverlapInventory(overlapCallbacks, mover, item);
        }
    }

    public void handleOverlapExplosive(List<Runnable> overlapCallbacks, Entity mover, Entity item) {
        Bomb b = (Bomb) item;
        if (b.getState() != Bomb.State.SPAWNED) {
            return;
        }

        Player p = (Player) mover;
        if (p.pickUp(b)) {
            b.getSubs().stream().forEach(s -> s.unsubscribe(b));
            b.unsubscribeAll();
            overlapCallbacks.add(() -> this.destroyEntity(b));
        }
    }

    public void handleOverlapInventory(List<Runnable> overlapCallbacks, Entity mover, Entity item) {
        Player p = (Player) mover;
        if (p.pickUp(item)) {
            overlapCallbacks.add(() -> this.destroyEntity(item));
        }
    }

    public boolean canMoveTo(Entity entity, Position position) {
        return !nodes.containsKey(position) || nodes.get(position).canMoveOnto(this, entity);
    }

    public Position dijkstraPathFind(Position src, Position dest, Entity entity) {
        // if inputs are invalid, don't move
        if (!nodes.containsKey(src) || !nodes.containsKey(dest))
            return src;

        Map<Position, Integer> dist = new HashMap<>();
        Map<Position, Position> prev = new HashMap<>();
        Map<Position, Boolean> visited = new HashMap<>();

        prev.put(src, null);
        dist.put(src, 0);

        PriorityQueue<Position> q = new PriorityQueue<>((x, y) -> Integer
                .compare(dist.getOrDefault(x, Integer.MAX_VALUE), dist.getOrDefault(y, Integer.MAX_VALUE)));
        q.add(src);

        while (!q.isEmpty()) {
            Position curr = q.poll();
            if (curr.equals(dest) || dist.get(curr) > 200)
                break;
            // check portal
            if (nodes.containsKey(curr) && nodes.get(curr).getEntities().stream().anyMatch(Portal.class::isInstance)) {
                Portal portal = nodes.get(curr).getEntities().stream().filter(Portal.class::isInstance)
                        .map(Portal.class::cast).collect(Collectors.toList()).get(0);
                List<Position> teleportDest = portal.getDestPositions(this, entity);
                teleportDest.stream().filter(p -> !visited.containsKey(p)).forEach(p -> {
                    dist.put(p, dist.get(curr));
                    prev.put(p, prev.get(curr));
                    q.add(p);
                });
                continue;
            }
            visited.put(curr, true);
            List<Position> neighbours = curr.getCardinallyAdjacentPositions().stream()
                    .filter(p -> !visited.containsKey(p))
                    .filter(p -> !nodes.containsKey(p) || nodes.get(p).canMoveOnto(this, entity))
                    .collect(Collectors.toList());

            neighbours.forEach(n -> {
                int newDist = dist.get(curr) + (nodes.containsKey(n) ? nodes.get(n).getWeight() : 1);
                if (newDist < dist.getOrDefault(n, Integer.MAX_VALUE)) {
                    q.remove(n);
                    dist.put(n, newDist);
                    prev.put(n, curr);
                    q.add(n);
                }
            });
        }
        Position ret = dest;
        if (prev.get(ret) == null || ret.equals(src))
            return src;
        while (!prev.get(ret).equals(src)) {
            ret = prev.get(ret);
        }
        return ret;
    }

    public void removeNode(Entity entity) {
        Position p = entity.getPosition();
        if (nodes.containsKey(p)) {
            nodes.get(p).removeEntity(entity);
            if (nodes.get(p).size() == 0) {
                nodes.remove(p);
            }
        }
    }

    public void destroyEntity(Entity entity) {
        removeNode(entity);
        if (entity instanceof DestroyedBehaviour) {
            DestroyedBehaviour ent = (DestroyedBehaviour) entity;
            ent.onDestroy(this);
        }
    }

    public void addEntity(Entity entity) {
        addNode(new GraphNode(entity));
    }

    public void addNode(GraphNode node) {
        Position p = node.getPosition();

        if (!nodes.containsKey(p))
            nodes.put(p, node);
        else {
            GraphNode curr = nodes.get(p);
            curr.mergeNode(node);
            nodes.put(p, curr);
        }
    }

    public Entity getEntity(String id) {
        Entity res = null;
        for (Map.Entry<Position, GraphNode> entry : nodes.entrySet()) {
            List<Entity> es = entry.getValue().getEntities().stream().filter(e -> e.getId().equals(id))
                    .collect(Collectors.toList());
            if (es != null && es.size() > 0) {
                res = es.get(0);
                break;
            }
        }
        return res;
    }

    public List<Entity> getEntities(Position p) {
        GraphNode node = nodes.get(p);
        return (node != null) ? node.getEntities() : new ArrayList<>();
    }

    public List<Entity> getEntities() {
        List<Entity> entities = new ArrayList<>();
        nodes.forEach((k, v) -> entities.addAll(v.getEntities()));
        return entities;
    }

    public <T extends Entity> List<T> getEntities(Class<T> type) {
        return getEntities().stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }

    public <T extends Entity, U> List<U> getEntities(Class<T> type, Class<U> type2) {
        return getEntities().stream().filter(type::isInstance).map(type2::cast).collect(Collectors.toList());
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public int getSpawnerCount() {
        return getEntities(ZombieToastSpawner.class).size();
    }
}
