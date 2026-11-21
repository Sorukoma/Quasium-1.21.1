package net.sorukoma.quasium.smokelands.sultan;

import java.util.ArrayList;
import java.util.List;

public final class SultanFight {
    public enum Move { RINGS, INHALE_START, INHALE_PULSE, INHALE_END, CLOUD, SUMMON, BURNOUT, BLAST, STUN_END }

    public record Rules(int phaseTwoPct, int phaseThreePct, int ringTicks, int inhaleEveryTicks, int inhaleTicks, int cloudTicks,
                        int summonTicks, int burnoutTicks, int vulnerableTicks, int stunTicks) {
        public Rules hastened(int inhaleEveryPct, int vulnerablePct) {
            return new Rules(phaseTwoPct, phaseThreePct, ringTicks, Math.max(1, inhaleEveryTicks * inhaleEveryPct / 100), inhaleTicks,
                    cloudTicks, summonTicks, burnoutTicks, Math.max(1, vulnerableTicks * vulnerablePct / 100), stunTicks);
        }
    }

    private static final int FIRST_RINGS = 40;
    private static final int PULSE = 20;

    private final Rules rules;
    private int phase = 1;
    private int toRings = FIRST_RINGS;
    private int toInhale;
    private int inhaleLeft;
    private int toCloud;
    private int toSummon;
    private int toBurnout;
    private int vulnerableLeft;
    private int stunLeft;

    public SultanFight(Rules rules) {
        this.rules = rules;
        toInhale = rules.inhaleEveryTicks();
    }

    public static int phaseFor(float healthPct, Rules rules) {
        if (healthPct > rules.phaseTwoPct()) return 1;
        return healthPct > rules.phaseThreePct() ? 2 : 3;
    }

    public List<Move> tick(float healthPct) {
        List<Move> moves = new ArrayList<>();
        enter(phaseFor(healthPct, rules));
        if (stunLeft > 0) {
            if (--stunLeft == 0) moves.add(Move.STUN_END);
            return moves;
        }
        if (--toRings <= 0) {
            toRings = rules.ringTicks();
            moves.add(Move.RINGS);
        }
        breathe(moves);
        if (phase >= 2) {
            if (--toCloud <= 0) {
                toCloud = rules.cloudTicks();
                moves.add(Move.CLOUD);
            }
            if (--toSummon <= 0) {
                toSummon = rules.summonTicks();
                moves.add(Move.SUMMON);
            }
        }
        if (phase == 3) burn(moves);
        return moves;
    }

    private void enter(int next) {
        if (next == phase) return;
        if (next >= 2 && phase < 2) {
            toCloud = rules.cloudTicks();
            toSummon = rules.summonTicks() / 2;
        }
        if (next == 3) toBurnout = rules.burnoutTicks() / 3;
        if (next < 3) vulnerableLeft = 0;
        phase = next;
    }

    private void breathe(List<Move> moves) {
        if (inhaleLeft > 0) {
            inhaleLeft--;
            if ((rules.inhaleTicks() - inhaleLeft) % PULSE == 0) moves.add(Move.INHALE_PULSE);
            if (inhaleLeft == 0) moves.add(Move.INHALE_END);
        }
        if (--toInhale <= 0) {
            toInhale = rules.inhaleEveryTicks();
            inhaleLeft = rules.inhaleTicks();
            moves.add(Move.INHALE_START);
        }
    }

    private void burn(List<Move> moves) {
        if (vulnerableLeft > 0) {
            if (--vulnerableLeft == 0) {
                toBurnout = rules.burnoutTicks();
                moves.add(Move.BLAST);
            }
            return;
        }
        if (--toBurnout <= 0) {
            vulnerableLeft = rules.vulnerableTicks();
            moves.add(Move.BURNOUT);
        }
    }

    public boolean douse() {
        if (vulnerableLeft <= 0) return false;
        vulnerableLeft = 0;
        toBurnout = rules.burnoutTicks();
        inhaleLeft = 0;
        toInhale = rules.inhaleEveryTicks();
        stunLeft = rules.stunTicks();
        return true;
    }

    public Rules rules() {
        return rules;
    }

    public int phase() {
        return phase;
    }

    public boolean inhaling() {
        return inhaleLeft > 0 && stunLeft == 0;
    }

    public boolean vulnerable() {
        return vulnerableLeft > 0;
    }

    public boolean stunned() {
        return stunLeft > 0;
    }
}
