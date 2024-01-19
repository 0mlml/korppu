package dev.mlml.korppu.config;

import lombok.Getter;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Config
{
    protected final List<GenericSetting<?>> settings = new ArrayList<>();

    public <S extends GenericSetting<?>> S add(S ta)
    {
        settings.add(ta);
        return ta;
    }

    @SuppressWarnings("unchecked")
    public <S extends GenericSetting<?>> S get(String name)
    {
        for (GenericSetting<?> setting : settings)
        {
            if (setting.getName().equals(name))
            {
                return (S) setting;
            }
        }

        return null;
    }

    public List<ClickableWidget> getAsWidgets()
    {
        List<ClickableWidget> widgets = new ArrayList<>();

        for (GenericSetting<?> setting : settings)
        {
            widgets.add(setting.getAsWidget());
        }

        return widgets;
    }
}
