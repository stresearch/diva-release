package com.visym.collector.capturemodule.model;

public class DataModel {

    public static class ConsentSubjectList{
        String subjectEmail;
        public ConsentSubjectList(String email){
            this.subjectEmail = email;
        }
        public String getSubjectEmail() {
            return subjectEmail;
        }

        public void setSubjectEmail(String subjectEmail) {
            this.subjectEmail = subjectEmail;
        }


    }


}
