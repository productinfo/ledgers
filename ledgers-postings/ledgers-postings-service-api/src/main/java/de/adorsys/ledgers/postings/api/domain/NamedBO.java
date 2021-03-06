package de.adorsys.ledgers.postings.api.domain;

import java.time.LocalDateTime;

/**
 * The existence or value of a ledger entity is always considered relative to
 * the posting date.
 * <p>
 * When a book is closed, modification on ledger entities must lead to the
 * creation of new entities.
 *
 * @author fpo
 */
public abstract class NamedBO {

    /*Business identifier.  Always unique in a certain scope. Generally in the scope of it's container.*/
    private String name;

    /* Identifier */
    private String id;

    private LocalDateTime created;

    //	todo: seems this property should be moved from base class
    private String userDetails;

    //	todo: seems this property should be moved from base class
    /*The short description of this entity*/
    private String shortDesc;

    //	todo: seems this property should be moved from base class
    /*The long description of this entity*/
    private String longDesc;

    public NamedBO() {
    }

    public NamedBO(String name) {
        this.name = name;
    }

    public NamedBO(String name, String id, LocalDateTime created, String userDetails, String shortDesc, String longDesc) {
        this.name = name;
        this.id = id;
        this.created = created;
        this.userDetails = userDetails;
        this.shortDesc = shortDesc;
        this.longDesc = longDesc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public String getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(String userDetails) {
        this.userDetails = userDetails;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public String getLongDesc() {
        return longDesc;
    }

    public void setLongDesc(String longDesc) {
        this.longDesc = longDesc;
    }

}
