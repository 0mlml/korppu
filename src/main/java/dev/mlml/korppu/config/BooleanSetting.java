package dev.mlml.korppu.config;

import java.util.List;
import java.util.function.Consumer;

public class BooleanSetting extends GenericSetting<Boolean>
{
    public BooleanSetting(String name, String tooltip, Boolean defaultValue, List<Consumer<Boolean>> callbacks)
    {
        super(name, tooltip, defaultValue, callbacks);
    }
}
