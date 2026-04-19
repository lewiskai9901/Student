package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.MenuContributionPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@SuppressWarnings("deprecation")
public class HealthcareMenuPlugin implements MenuContributionPlugin {

    @Override public String getDomainCode() { return "healthcare"; }

    @Override
    public List<MenuItemDef> getMenus() {
        return List.of(
            MenuItemDef.of("/patient", "病人管理", "heart-pulse", 5)
                .children(List.of(
                    MenuItemDef.of("/patient/list", "病人列表", "list", 1)
                        .withComponent("@/views/healthcare/PatientList.vue")
                ))
        );
    }
}
