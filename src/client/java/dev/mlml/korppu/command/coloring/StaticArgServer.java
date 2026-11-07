package dev.mlml.korppu.command.coloring;

public class StaticArgServer {
    public static PossibleArg serveFromStatic(int index, PossibleArg... types) {
        if (index >= types.length) {
            return new PossibleArg(null);
        }
        return types[index];
    }
}
