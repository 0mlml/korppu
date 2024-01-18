package dev.mlml.korppu.config;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

// TODO:  implement later alongside with better config screen generation
public class Config
{
    @Getter
    final List<GenericSetting<?>> settings = new ArrayList<>();

    public <S extends GenericSetting<?>> S add(S ta) {
        settings.add(ta);
        return ta;
    }
}
