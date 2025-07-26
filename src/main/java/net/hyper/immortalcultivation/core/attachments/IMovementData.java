package net.hyper.immortalcultivation.core.attachments;

public interface IMovementData {

    enum MovementType {
        WALK_SPEED("Walking Speed"),
        SPRINT_SPEED("Sprinting Speed"),
        SWIM_SPEED("Swimming Speed"),
        FLIGHT_SPEED("Flying Speed"),
        JUMP_HEIGHT("Jumping Height"),
        STEP_HEIGHT ("Stepping Height");

        private final String displayName;
        MovementType(String displayName) {this.displayName = displayName;}
        public String getDisplayName() {return displayName;}
    }

    double getMovementType(IMovementData.MovementType type);
    double getBonusMovementType(IMovementData.MovementType type, BonusSource source);
    double getPermanentBonusMovementType(IMovementData.MovementType type, BonusSource source);
}
