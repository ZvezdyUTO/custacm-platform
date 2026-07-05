package com.custacm.platform.common.sqltask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SqlTaskGraph {
    private final Map<String, SqlTaskDefinition> definitions;
    private final Map<String, Set<String>> adjacency;
    private final List<SqlTaskDefinition> topologicalOrder;

    private SqlTaskGraph(
            Map<String, SqlTaskDefinition> definitions,
            Map<String, Set<String>> adjacency,
            List<SqlTaskDefinition> topologicalOrder
    ) {
        this.definitions = definitions;
        this.adjacency = adjacency;
        this.topologicalOrder = topologicalOrder;
    }

    static SqlTaskGraph from(Collection<SqlTaskDefinition> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            throw new SqlTaskException(SqlTaskErrorCode.SQL_TASK_CONFIG_INVALID, "sql task manifest must not be empty");
        }
        Map<String, SqlTaskDefinition> definitions = new LinkedHashMap<>();
        for (SqlTaskDefinition task : tasks) {
            if (definitions.putIfAbsent(task.id(), task) != null) {
                throw new SqlTaskException(
                        SqlTaskErrorCode.SQL_TASK_CONFIG_INVALID,
                        "duplicate sql task id: " + task.id()
                );
            }
        }

        Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        Map<String, Integer> indegree = new LinkedHashMap<>();
        for (String taskId : definitions.keySet()) {
            adjacency.put(taskId, new LinkedHashSet<>());
            indegree.put(taskId, 0);
        }
        for (SqlTaskDefinition task : definitions.values()) {
            Set<String> uniqueDependencies = new LinkedHashSet<>();
            for (String dependency : task.dependsOn()) {
                String dependencyId = dependency == null ? "" : dependency.trim();
                if (dependencyId.isBlank()) {
                    throw new SqlTaskException(
                            SqlTaskErrorCode.SQL_TASK_CONFIG_INVALID,
                            "blank dependency declared by sql task: " + task.id()
                    );
                }
                if (dependencyId.equals(task.id())) {
                    throw new SqlTaskException(
                            SqlTaskErrorCode.SQL_TASK_DAG_INVALID,
                            "sql task must not depend on itself: " + task.id()
                    );
                }
                if (!definitions.containsKey(dependencyId)) {
                    throw new SqlTaskException(
                            SqlTaskErrorCode.SQL_TASK_CONFIG_INVALID,
                            "sql task " + task.id() + " depends on missing task: " + dependencyId
                    );
                }
                uniqueDependencies.add(dependencyId);
            }
            for (String dependencyId : uniqueDependencies) {
                if (adjacency.get(dependencyId).add(task.id())) {
                    indegree.compute(task.id(), (ignored, degree) -> degree + 1);
                }
            }
        }
        List<SqlTaskDefinition> topologicalOrder = sortTopologically(definitions, adjacency, indegree);
        return new SqlTaskGraph(definitions, adjacency, topologicalOrder);
    }

    List<SqlTaskDefinition> executionPlan(String startFromTaskId) {
        if (startFromTaskId == null) {
            return topologicalOrder;
        }
        if (!definitions.containsKey(startFromTaskId)) {
            throw new SqlTaskException(
                    SqlTaskErrorCode.SQL_TASK_START_NODE_INVALID,
                    "startFromTaskId does not exist in sql task manifest: " + startFromTaskId
            );
        }
        Set<String> reachable = downstreamClosure(startFromTaskId);
        return topologicalOrder.stream()
                .filter(task -> reachable.contains(task.id()))
                .toList();
    }

    private Set<String> downstreamClosure(String startTaskId) {
        Set<String> visited = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        visited.add(startTaskId);
        queue.add(startTaskId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            for (String next : adjacency.get(current)) {
                if (visited.add(next)) {
                    queue.addLast(next);
                }
            }
        }
        return visited;
    }

    private static List<SqlTaskDefinition> sortTopologically(
            Map<String, SqlTaskDefinition> definitions,
            Map<String, Set<String>> adjacency,
            Map<String, Integer> indegree
    ) {
        ArrayDeque<String> ready = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.addLast(entry.getKey());
            }
        }
        List<SqlTaskDefinition> sorted = new ArrayList<>();
        while (!ready.isEmpty()) {
            String taskId = ready.removeFirst();
            sorted.add(definitions.get(taskId));
            for (String next : adjacency.get(taskId)) {
                int nextDegree = indegree.compute(next, (ignored, degree) -> degree - 1);
                if (nextDegree == 0) {
                    ready.addLast(next);
                }
            }
        }
        if (sorted.size() != definitions.size()) {
            throw new SqlTaskException(
                    SqlTaskErrorCode.SQL_TASK_DAG_INVALID,
                    "sql task graph must be a DAG; cycle detected"
            );
        }
        return List.copyOf(sorted);
    }
}
