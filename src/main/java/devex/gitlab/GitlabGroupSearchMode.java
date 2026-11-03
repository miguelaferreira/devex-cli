package devex.gitlab;

import java.util.function.Predicate;

public enum GitlabGroupSearchMode {
    NAME, FULL_PATH, ID;

    public String textualQualifier() {
        return switch (this) {
            case NAME -> "named";
            case FULL_PATH -> "with full path";
            case ID -> "identified by";
        };
    }

    public Predicate<GitlabGroup> groupPredicate(String search) {
        return switch (this) {
            case NAME -> group -> group.name().equalsIgnoreCase(search);
            case FULL_PATH -> group -> group.fullPath().equalsIgnoreCase(search);
            case ID -> group -> group.id().equalsIgnoreCase(search);
        };
    }
}
