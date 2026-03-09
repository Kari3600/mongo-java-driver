package org.bson.codecs.pojo.entities;

import org.bson.codecs.SimpleEnum;

import java.util.EnumMap;
import java.util.Objects;

public class EnumMapModel {
    private EnumMap<SimpleEnum, Integer> values;

    public EnumMapModel() {
    }

    public EnumMapModel(final EnumMap<SimpleEnum, Integer> values) {
        this.values = values;
    }

    public EnumMap<SimpleEnum, Integer> getValues() {
        return values;
    }

    public void setValues(final EnumMap<SimpleEnum, Integer> values) {
        this.values = values;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnumMapModel that = (EnumMapModel) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        return "EnumMapModel{"
                + "values=" + values
                + '}';
    }
}
