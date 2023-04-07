package dungeonmania.entities.buildables;

import dungeonmania.Game;
import dungeonmania.entities.BattleItem;
import dungeonmania.battles.BattleStatistics;

public class Shield extends Buildable implements BattleItem{
    private double defence;

    public Shield(int durability, double defence) {
        super(durability);
        this.defence = defence;
    }

    @Override
    public void use(Game game) {
        reduceDurability();
        if (getDurabilityStat() <= 0) {
            game.getPlayer().remove(this);
        }
    }

    @Override
    public BattleStatistics applyBuff(BattleStatistics origin) {
        return BattleStatistics.applyBuff(origin, new BattleStatistics(0, 0, defence, 1, 1));
    }

    @Override
    public int getDurability() {
        return getDurabilityStat();
    }

}
