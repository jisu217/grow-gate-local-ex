package com.growgate.publicdata.entity;

import com.growgate.publicdata.enums.PublicDataProvider;
import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDataRecordSchemaContractTest {

    @Test
    void providerColumnLengthCoversAllProviderEnumNames() throws Exception {
        Field providerField = PublicDataRecord.class.getDeclaredField("provider");
        int providerColumnLength = providerField.getAnnotation(Column.class).length();

        int maxProviderNameLength = Arrays.stream(PublicDataProvider.values())
                .map(Enum::name)
                .mapToInt(String::length)
                .max()
                .orElse(0);

        assertThat(providerColumnLength).isGreaterThanOrEqualTo(maxProviderNameLength);
    }
}
