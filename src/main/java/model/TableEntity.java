package model;

import java.io.Serializable;
import java.util.Objects;

public abstract class TableEntity implements Serializable {

    private String id = "";
    public static String createTableStatement;
    private static final long serialVersionUID = 1L;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        TableEntity other = (TableEntity)obj;

        if ((this.getId().length() > 0) && (other != null)) {
            return getId().equals(other.getId());
        }
        return super.equals(obj);
    }
}
