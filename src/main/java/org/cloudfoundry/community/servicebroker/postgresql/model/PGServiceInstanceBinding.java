package org.cloudfoundry.community.servicebroker.postgresql.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cq on 4/7/16.
 */
public class PGServiceInstanceBinding {

    private String bindingid;

    private String serviceInstanceId;

    private Map<String,Object> credentials = new HashMap<>();

    private String syslogDrainUrl;

    private String appGuid;


    public String getBindingid() {
        return bindingid;
    }

    public void setBindingid(String bindingid) {
        this.bindingid = bindingid;
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, Object> credentials) {
        this.credentials = credentials;
    }

    public String getSyslogDrainUrl() {
        return syslogDrainUrl;
    }

    public void setSyslogDrainUrl(String syslogDrainUrl) {
        this.syslogDrainUrl = syslogDrainUrl;
    }

    public String getAppGuid() {
        return appGuid;
    }

    public void setAppGuid(String appGuid) {
        this.appGuid = appGuid;
    }
}
