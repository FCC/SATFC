package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

/**
 * Type of possible constraints.
 *
 * @author afrechet
 */
public enum ConstraintKey {
    CO,
    ADJp1,
    ADJm1,
    ADJp2,
    ADJm2;

    public static ConstraintKey fromString(String string) {
        switch (string) {
            case "CO":
                return CO;
            case "ADJ+1":
                return ADJp1;
            case "ADJ-1":
                return ADJm1;
            case "ADJ+2":
                return ADJp2;
            case "ADJ-2":
                return ADJm2;
            default:
                throw new IllegalArgumentException("Unrecognized constraint key " + string);
        }
    }
}