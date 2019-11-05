package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

public enum EnumColor {
    RED(0,0.1d),
    YELLOw(0.1d, 0.5d),
    GREEN(0.5d, 1.0d);

    public final double MIN;
    public final double MAX;
    EnumColor(double min, double max) {
        this.MIN = min;
        this.MAX = max;
    }
}
