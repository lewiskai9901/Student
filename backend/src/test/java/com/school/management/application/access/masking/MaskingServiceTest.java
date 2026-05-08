package com.school.management.application.access.masking;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MaskingServiceTest {
    private final MaskingService s = new MaskingService();

    @Test void phone_normal() { assertThat(s.maskPhone("13800000001")).isEqualTo("138****0001"); }
    @Test void phone_null() { assertThat(s.maskPhone(null)).isNull(); }
    @Test void phone_short() { assertThat(s.maskPhone("123")).isEqualTo("123"); }

    @Test void email_normal() { assertThat(s.maskEmail("alice@example.com")).isEqualTo("a***@example.com"); }
    @Test void email_singleChar() { assertThat(s.maskEmail("a@x.com")).isEqualTo("a***@x.com"); }
    @Test void email_noAt() { assertThat(s.maskEmail("notanemail")).isEqualTo("notanemail"); }
    @Test void email_null() { assertThat(s.maskEmail(null)).isNull(); }

    @Test void idCard_normal() { assertThat(s.maskIdCard("370102199001011234")).isEqualTo("370****1234"); }
    @Test void idCard_short() { assertThat(s.maskIdCard("123")).isEqualTo("123"); }

    @Test void name_oneChar() { assertThat(s.maskName("张")).isEqualTo("张"); }
    @Test void name_twoChar() { assertThat(s.maskName("张三")).isEqualTo("张*"); }
    @Test void name_threeChar() { assertThat(s.maskName("王浩宇")).isEqualTo("王**宇"); }
    @Test void name_null() { assertThat(s.maskName(null)).isNull(); }

    @Test void maskField_dispatch() {
        assertThat(s.maskField("phone", "13800000001")).isEqualTo("138****0001");
        assertThat(s.maskField("unknown", "value")).isEqualTo("value");
        assertThat(s.maskField(null, "v")).isEqualTo("v");
    }
}
