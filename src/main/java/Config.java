public final class Config {
    public static final boolean printGenerationBrains = false;
    public static String mode = "AI";
    public static final boolean printGenerationScores = false;
    public static final boolean variableSpeeds = false;
    public static final int numTargets = 2;
    public static final boolean printBulletCoords = false;
    public static final boolean printBulletShoot = false;

    //I can do this right? It's not breaking any laws or anything? Do methods in the config file make sense?
    //I looked it up and there's not much info, but sometimes there are methods in config files so I'm going with this.

    //My old buddy randInt

    /**
     * Generates a random number between these two bounds
     * @param min The lower bound (inclusive)
     * @param max The upper bound (inclusive)
     * @return A random number between min and max
     */
    public static int randInt(int min, int max){
        return (int)(Math.random()*(max + 1 - min)) + min;
    }

    //My new buddy trimDouble

    /**
     * Shortens a double to a certain number of digits
     * @param d The double to trim
     * @param numDigits The number of digits the double should have
     * @return The shortened double
     */
    public static double trimDouble(double d, int numDigits){
        int temp = (int)(d * Math.pow(10, numDigits));
        return ((double) temp) / Math.pow(10, numDigits);
    }

    public static void setMode(String mode) {
        Config.mode = mode;
    }
}
