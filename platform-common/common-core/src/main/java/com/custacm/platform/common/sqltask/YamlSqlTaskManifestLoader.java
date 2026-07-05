package com.custacm.platform.common.sqltask;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class YamlSqlTaskManifestLoader {
    private final ResourceLoader resourceLoader;

    YamlSqlTaskManifestLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    SqlTaskManifest load(String manifestLocation) {
        Resource resource = resourceLoader.getResource(manifestLocation);
        if (!resource.exists()) {
            throw new SqlTaskException(
                    SqlTaskErrorCode.SQL_TASK_RESOURCE_UNREADABLE,
                    "sql task manifest does not exist: " + manifestLocation
            );
        }
        try (InputStream inputStream = resource.getInputStream()) {
            Object loaded = new Yaml().load(inputStream);
            return parseManifest(loaded, manifestLocation);
        } catch (SqlTaskException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new SqlTaskException(
                    SqlTaskErrorCode.SQL_TASK_RESOURCE_UNREADABLE,
                    "failed to read sql task manifest: " + manifestLocation,
                    ex
            );
        } catch (RuntimeException ex) {
            throw new SqlTaskException(
                    SqlTaskErrorCode.SQL_TASK_CONFIG_INVALID,
                    "failed to parse sql task manifest: " + manifestLocation,
                    ex
            );
        }
    }

    private SqlTaskManifest parseManifest(Object loaded, String manifestLocation) {
        if (!(loaded instanceof Map<?, ?> root)) {
            throw invalid("sql task manifest root must be an object: " + manifestLocation);
        }
        Object tasksValue = root.get("tasks");
        if (!(tasksValue instanceof List<?> taskItems)) {
            throw invalid("sql task manifest must contain a tasks list: " + manifestLocation);
        }
        List<SqlTaskDefinition> tasks = new ArrayList<>();
        for (int index = 0; index < taskItems.size(); index++) {
            Object taskValue = taskItems.get(index);
            if (!(taskValue instanceof Map<?, ?> taskMap)) {
                throw invalid("sql task entry must be an object at index " + index);
            }
            tasks.add(parseTask(taskMap, index));
        }
        return new SqlTaskManifest(tasks);
    }

    private SqlTaskDefinition parseTask(Map<?, ?> taskMap, int index) {
        String id = requiredText(taskMap, "id", index);
        String sqlLocation = requiredText(taskMap, "sql", index);
        String description = optionalText(taskMap.get("description"));
        List<String> dependsOn = parseDependsOn(taskMap.get("dependsOn"), index);
        Duration timeout = parseTimeout(taskMap.get("timeoutSeconds"), index);
        return new SqlTaskDefinition(id, description, sqlLocation, dependsOn, timeout);
    }

    private List<String> parseDependsOn(Object value, int index) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawItems)) {
            throw invalid("dependsOn must be a list at task index " + index);
        }
        List<String> result = new ArrayList<>();
        for (Object rawItem : rawItems) {
            String dependency = optionalText(rawItem);
            if (dependency.isBlank()) {
                throw invalid("dependsOn item must not be blank at task index " + index);
            }
            result.add(dependency);
        }
        return result;
    }

    private Duration parseTimeout(Object value, int index) {
        if (value == null) {
            return Duration.ZERO;
        }
        if (value instanceof Number number) {
            long seconds = number.longValue();
            if (seconds < 0L || seconds > Integer.MAX_VALUE) {
                throw invalid("timeoutSeconds is out of range at task index " + index);
            }
            return Duration.ofSeconds(seconds);
        }
        throw invalid("timeoutSeconds must be a number at task index " + index);
    }

    private String requiredText(Map<?, ?> map, String key, int index) {
        String value = optionalText(map.get(key));
        if (value.isBlank()) {
            throw invalid(key + " must not be blank at task index " + index);
        }
        return value;
    }

    private String optionalText(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private SqlTaskException invalid(String message) {
        return new SqlTaskException(SqlTaskErrorCode.SQL_TASK_CONFIG_INVALID, message);
    }
}
