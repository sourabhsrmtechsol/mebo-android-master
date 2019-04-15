package com.example.mebo_ximpatico;

public class TransactionDetails {
    String taskDescription;
    String demonstrationsNo;
    String demonstrationDuration;

    public TransactionDetails()
    {

    }

    public TransactionDetails(String taskDescription, String demonstrationsNo, String demonstrationDuration) {
        this.taskDescription = taskDescription;
        this.demonstrationsNo = demonstrationsNo;
        this.demonstrationDuration = demonstrationDuration;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getDemonstrationsNo() {
        return demonstrationsNo;
    }

    public void setDemonstrationsNo(String demonstrationsNo) {
        this.demonstrationsNo = demonstrationsNo;
    }

    public String getDemonstrationDuration() {
        return demonstrationDuration;
    }

    public void setDemonstrationDuration(String demonstrationDuration) {
        this.demonstrationDuration = demonstrationDuration;
    }


}
