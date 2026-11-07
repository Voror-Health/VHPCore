package org.endeavourhealth.core.database.dal.datagenerator.models;

import java.util.Date;
import java.util.UUID;

public class RemoteFilingRecord {

    private int subscriberId;
    private UUID serviceId;
    private UUID systemId;
    private UUID exchangeId;
    private UUID batchId;
    private UUID queuedMessageId;
    //private String queuedMessageBody; //don't include this as large and not needed
    private long filingOrder;
    private Date dtFileSent;
    private Date dtFileFilingAttempted;
    private Boolean fileFilingSuccess;
    private String filingFailureMessage;
    private Date dtInserted;

    public int getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(int subscriberId) {
        this.subscriberId = subscriberId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getSystemId() {
        return systemId;
    }

    public void setSystemId(UUID systemId) {
        this.systemId = systemId;
    }

    public UUID getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(UUID exchangeId) {
        this.exchangeId = exchangeId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public UUID getQueuedMessageId() {
        return queuedMessageId;
    }

    public void setQueuedMessageId(UUID queuedMessageId) {
        this.queuedMessageId = queuedMessageId;
    }

    /*public String getQueuedMessageBody() {
        return queuedMessageBody;
    }

    public void setQueuedMessageBody(String queuedMessageBody) {
        this.queuedMessageBody = queuedMessageBody;
    }*/

    public long getFilingOrder() {
        return filingOrder;
    }

    public void setFilingOrder(long filingOrder) {
        this.filingOrder = filingOrder;
    }

    public Date getDtFileSent() {
        return dtFileSent;
    }

    public void setDtFileSent(Date dtFileSent) {
        this.dtFileSent = dtFileSent;
    }

    public Date getDtFileFilingAttempted() {
        return dtFileFilingAttempted;
    }

    public void setDtFileFilingAttempted(Date dtFileFilingAttempted) {
        this.dtFileFilingAttempted = dtFileFilingAttempted;
    }

    public Boolean getFileFilingSuccess() {
        return fileFilingSuccess;
    }

    public void setFileFilingSuccess(Boolean fileFilingSuccess) {
        this.fileFilingSuccess = fileFilingSuccess;
    }

    public String getFilingFailureMessage() {
        return filingFailureMessage;
    }

    public void setFilingFailureMessage(String filingFailureMessage) {
        this.filingFailureMessage = filingFailureMessage;
    }

    public Date getDtInserted() {
        return dtInserted;
    }

    public void setDtInserted(Date dtInserted) {
        this.dtInserted = dtInserted;
    }
}
