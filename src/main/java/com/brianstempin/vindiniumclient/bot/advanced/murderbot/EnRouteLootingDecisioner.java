package com.brianstempin.vindiniumclient.bot.advanced.murderbot;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.BotUtils;
import com.brianstempin.vindiniumclient.bot.advanced.AdvancedGameState;
import com.brianstempin.vindiniumclient.bot.advanced.Mine;
import com.brianstempin.vindiniumclient.bot.advanced.Vertex;
import com.brianstempin.vindiniumclient.dto.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.List;

/**
 * Decides if we should take some mines on the way to whatever it was we were doing.
 *
 * This decisioner will decide if its worth going after a near-by mine.
 *
 * Maslov says, "self actualization."
 */
public class EnRouteLootingDecisioner implements Decision<AdvancedMurderBot.GameContext, BotMove> {

    private final static Logger logger = LogManager.getLogger(EnRouteLootingDecisioner.class);

    private final Decision<AdvancedMurderBot.GameContext, BotMove> noGoodMineDecisioner;

    public EnRouteLootingDecisioner(Decision<AdvancedMurderBot.GameContext, BotMove> noGoodMineDecisioner) {
        this.noGoodMineDecisioner = noGoodMineDecisioner;
    }

    @Override
    public BotMove makeDecision(AdvancedMurderBot.GameContext context) {
        GameState.Position myPosition = context.getGameState().getMe().getPos();
        Map<GameState.Position, Vertex> boardGraph = context.getGameState().getBoardGraph();

        // Are we next to a mine that isn't ours?
        for(Vertex currentVertex : boardGraph.get(myPosition).getAdjacentVertices()) {
            Mine mine = context.getGameState().getMines().get(currentVertex.getPosition());
            // XXX added && context.getGameState().getMe().getLife() > 35
            // if mine exists && (it has no owner || it's owner is not us) && myHealth > 35
            if(mine != null && (mine.getOwner() == null
                    || mine.getOwner().getId() != context.getGameState().getMe().getId()) && context.getGameState().getMe().getLife() > 35) {

                // Is it safe to take?
                // XXX added && 
                List<GameState.Hero> enemies = BotUtils.getHeroesAround(context.getGameState(), context.getDijkstraResultMap(), 1);
                
                boolean myHealthIsGreater = false;
                
                // check if my health is greater than both enemies
                for (GameState.Hero enemy : enemies) {
                  if (context.getGameState().getMe().getLife() < enemy.getLife()) {
                    myHealthIsGreater = false;
                    break;
                  } else {
                    myHealthIsGreater = true;
                  }
                }                
                
                if(enemies.size() > 0 && !myHealthIsGreater) {
                    logger.info("Mine found, but another hero is too close.");
                    return noGoodMineDecisioner.makeDecision(context);
                }
                logger.info("Taking a mine that we happen to already be walking by.");
                return BotUtils.directionTowards(myPosition, mine.getPosition());
            }
        }

        // Nope.
        logger.info("No opportunistic mines exist.");
        return noGoodMineDecisioner.makeDecision(context);
    }
}
