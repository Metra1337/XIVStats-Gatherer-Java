package com.ffxivcensus.gatherer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.ffxivcensus.gatherer.player.CharacterStatus;
import com.ffxivcensus.gatherer.player.PlayerBean;
import com.ffxivcensus.gatherer.player.PlayerBeanDAO;
import com.ffxivcensus.gatherer.player.PlayerBuilder;
import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;

/**
 * Gatherer worker class that implements Runnable class.
 * <p>
 * A worker that executes parses and writes the corresponding player record to the DB.
 *
 * @author Peter Reid
 * @since v1.0
 * @see PlayerBuilder
 * @see GathererController
 * @see java.lang.Runnable
 */
public class Gatherer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Gatherer.class);
    private static final Logger RESULT_LOG = LoggerFactory.getLogger(Gatherer.class.getName() + ".result");

    private int playerId;
    private PlayerBeanDAO dao;

    /**
     * Run the Gatherer.
     */
    @Override
    public void run() {
        try {
            LOG.debug("Starting evaluation of player ID: " + getPlayerId());
            // Parse players and write them to DB
            PlayerBean player = PlayerBuilder.getPlayer(getPlayerId(), 1);
            // Currently ignore deleted characters (404)
            if(!CharacterStatus.DELETED.equals(player.getCharacterStatus())) {
                String out = getDao().saveRecord(player);
                LOG.debug(out);
            }
            RESULT_LOG.info(getPlayerId() + " - " + player.getCharacterStatus().name());
        } catch(MySQLNonTransientConnectionException failWriteEx) { // If record fails to write due to too many connections
            LOG.trace("Error: Record write failure, reattempting write");
            // Wait a second
            try {
                Thread.currentThread().wait(1);
            } catch(InterruptedException e) {
            }
            // Then attempt to write again
            try {
                String out = getDao().saveRecord(PlayerBuilder.getPlayer(getPlayerId(), 1));
                LOG.debug(out);
            } catch(Exception e) {
                LOG.error(e.getMessage(), e);
            }
        } catch(Exception e) {
            LOG.error(e.getMessage(), e);
            RESULT_LOG.debug(getPlayerId() + " - FAILED");
        }
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final int playerId) {
        this.playerId = playerId;
    }

    public PlayerBeanDAO getDao() {
        return dao;
    }

    public void setPlayerBeanDAO(@Autowired final PlayerBeanDAO playerBeanDAO) {
        this.dao = playerBeanDAO;
    }

}
