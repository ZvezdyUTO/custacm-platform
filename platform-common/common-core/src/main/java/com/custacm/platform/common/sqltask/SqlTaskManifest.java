package com.custacm.platform.common.sqltask;

import java.util.List;

record SqlTaskManifest(List<SqlTaskDefinition> tasks) {
    SqlTaskManifest {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }
}
