package devex.gitlab;

import java.util.function.Predicate;

public enum GitlabGroupSearchMode {
    NAME, FULL_PATH, ID;

    public String textualQualifier() {
        switch (this) {
            case NAME:
                return "named";
            case FULL_PATH:
                return "with full path";
            case ID:
                return "identified by";
        }
        throw new IllegalArgumentException("There is no qualifier defined for " + this);
    }

    public Predicate<GitlabGroup> groupPredicate(String search) {
        switch (this) {
            case NAME:
                return group -> group.getName().equalsIgnoreCase(search);
            case FULL_PATH:
                return group -> group.getFullPath().equalsIgnoreCase(search);
            case ID:
                return group -> group.getId().equalsIgnoreCase(search);
        }
        throw new IllegalArgumentException("There is no predicate defined for " + this);
    }
}
