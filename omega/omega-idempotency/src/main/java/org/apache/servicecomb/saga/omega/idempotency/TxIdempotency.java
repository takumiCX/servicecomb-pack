package org.apache.servicecomb.saga.omega.idempotency;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Date;

/**
 * @author: takumiCX
 * @create: 2019-03-23
 * @email: takumicx@zju.edu.cn
 **/

@Entity
@Table(name = "TxIdempotency")
public class TxIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surrogateId;
    private String serviceName;
    private String instanceId;
    private Date createdAt;
    private Date updatedAt;
    private String globalTxId;
    private String parentTxId;
    private String state;
    //结果
    private byte[] result;

    public TxIdempotency() {
    }

    public Long getSurrogateId() {
        return surrogateId;
    }

    public void setSurrogateId(Long surrogateId) {
        this.surrogateId = surrogateId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getGlobalTxId() {
        return globalTxId;
    }

    public void setGlobalTxId(String globalTxId) {
        this.globalTxId = globalTxId;
    }

    public String getParentTxId() {
        return parentTxId;
    }

    public void setParentTxId(String parentTxId) {
        this.parentTxId = parentTxId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public byte[] getResult() {
        return result;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "TxIdempotency{" +
                "surrogateId=" + surrogateId +
                ", serviceName='" + serviceName + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", globalTxId='" + globalTxId + '\'' +
                ", parentTxId='" + parentTxId + '\'' +
                ", state='" + state + '\'' +
                ", result=" + Arrays.toString(result) +
                '}';
    }
}
