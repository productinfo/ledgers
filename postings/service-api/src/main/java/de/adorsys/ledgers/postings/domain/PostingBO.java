package de.adorsys.ledgers.postings.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The word posting is associated with the moment at which the recorded
 * operation is effective in the ledger. Therefore, we distinguish between
 * following moments:
 * - The operation time: the moment at which this operation took place..
 * - The posting time: the moment at which this operation is
 * effectively posted to the ledger (e.g. having influence of an account balance).
 * - The recording time: the moment at which this operation is recorded in the
 * journal.
 */
public class PostingBO extends HashRecordBO {

    /* The record id */
    private String id;

    /* The user (technically) recording this posting. */
    private String recordUser;

    /* The time of recording of this posting. */
    private LocalDateTime recordTime;

    /*
     * The unique identifier of this business operation. The operation
     * identifier differs from the posting identifier in that it is not unique.
     * The same operation, can be repetitively posted if some conditions change.
     * The operation identifier will always be the same for all the postings of
     * an operation. Only one of them will be effective in the account statement
     * at any given time.
     */
    private String oprId;

    /*
     * The sequence number of the operation processed by this posting.
     *
     * A single operation can be overridden many times as long as the enclosing
     * ledger is not closed. These overriding happens
     * synchronously. Each single one increasing the sequence number of the
     * former posting.
     *
     * This is, the posting id is always a concatenation between the operation
     * id and the sequence number.
     *
     */
    private int oprSeqNbr = 0;

    /* The time of occurrence of this operation. Set by the consuming module. */
    private LocalDateTime oprTime;

    /*
     * The type of operation recorded here. The semantic of this information is
     * determined by the consuming module.
     */
    private String oprType;

    /* Details associated with this operation. */
    private String oprDetails;

    /*
     * This is the time from which the posting is effective in the account
     * statement. This also differs from the recording time in that the posting
     * time can be before or after the recording time.
     *
     * If the posting time if before the recording time, it might have an effect
     * on former postings like past balances. This might lead to the generation
     * of new postings.
     *
     * The posting time of an adjustment operation at day closing is always the
     * last second of that day. So event if that operation is posted while still
     * inside the day, the day closing will be the same. This is, the last
     * second of that day. In the case of an adjustment operation, the posting
     * time and the operation time are identical.
     */
    private LocalDateTime pstTime;

    /*
     * Some posting are mechanical and do not have an influence on the balance
     * of an account. Depending on the business logic of the product module,
     * different types of posting might be defined so that the journal can be
     * used to document all events associated with an account.
     *
     * For a mechanical posting, the same account and amounts must appear in the
     * debit and the credit side of the posting. Some account statement will not
     * display mechanical postings while producing the user statement.
     */
    private PostingTypeBO pstType;

    /*
     * This is the status of the posting. Can be used to book
     */
    private PostingStatusBO pstStatus = PostingStatusBO.POSTED;

    /*
     * The ledger governing this posting.
     */
    private LedgerBO ledger;

    /*
     * The Date use to compute interests. This can be different from the posting
     * date and can lead to the production of other type of balances.
     */
    private LocalDateTime valTime;

//    todo: add description to this field
    private List<PostingLineBO> lines = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecordUser() {
        return recordUser;
    }

    public void setRecordUser(String recordUser) {
        this.recordUser = recordUser;
    }

    public LocalDateTime getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(LocalDateTime recordTime) {
        this.recordTime = recordTime;
    }

    public String getOprId() {
        return oprId;
    }

    public void setOprId(String oprId) {
        this.oprId = oprId;
    }

    public int getOprSeqNbr() {
        return oprSeqNbr;
    }

    public void setOprSeqNbr(int oprSeqNbr) {
        this.oprSeqNbr = oprSeqNbr;
    }

    public LocalDateTime getOprTime() {
        return oprTime;
    }

    public void setOprTime(LocalDateTime oprTime) {
        this.oprTime = oprTime;
    }

    public String getOprType() {
        return oprType;
    }

    public void setOprType(String oprType) {
        this.oprType = oprType;
    }

    public String getOprDetails() {
        return oprDetails;
    }

    public void setOprDetails(String oprDetails) {
        this.oprDetails = oprDetails;
    }

    public LocalDateTime getPstTime() {
        return pstTime;
    }

    public void setPstTime(LocalDateTime pstTime) {
        this.pstTime = pstTime;
    }

    public PostingTypeBO getPstType() {
        return pstType;
    }

    public void setPstType(PostingTypeBO pstType) {
        this.pstType = pstType;
    }

    public PostingStatusBO getPstStatus() {
        return pstStatus;
    }

    public void setPstStatus(PostingStatusBO pstStatus) {
        this.pstStatus = pstStatus;
    }

    public LedgerBO getLedger() {
        return ledger;
    }

    public void setLedger(LedgerBO ledger) {
        this.ledger = ledger;
    }

    public LocalDateTime getValTime() {
        return valTime;
    }

    public void setValTime(LocalDateTime valTime) {
        this.valTime = valTime;
    }

    public List<PostingLineBO> getLines() {
        return lines;
    }

    public void setLines(List<PostingLineBO> lines) {
        this.lines = lines;
    }
}